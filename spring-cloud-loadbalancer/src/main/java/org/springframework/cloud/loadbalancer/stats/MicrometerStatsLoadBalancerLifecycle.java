/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.loadbalancer.stats;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.CompletionContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancerLifecycle;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.client.loadbalancer.TimedRequestContext;

import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildDiscardedRequestTags;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildFailedRequestTags;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildServiceInstanceTags;
import static org.springframework.cloud.loadbalancer.stats.LoadBalancerTags.buildSuccessRequestTags;

/**
 * An implementation of {@link LoadBalancerLifecycle} that records metrics for
 * load-balanced calls.
 *
 * @author Olga Maciaszek-Sharma
 * @since 3.0.0
 */
public class MicrometerStatsLoadBalancerLifecycle implements LoadBalancerLifecycle<Object, Object, ServiceInstance> {

	private final MeterRegistry meterRegistry;

	private final ConcurrentHashMap<ServiceInstance, AtomicLong> activeRequestsPerInstance = new ConcurrentHashMap<>();

	public MicrometerStatsLoadBalancerLifecycle(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public boolean supports(Class requestContextClass, Class responseClass, Class serverTypeClass) {
		return ServiceInstance.class.isAssignableFrom(serverTypeClass);
	}

	@Override
	public void onStart(Request<Object> request) {
		// do nothing
	}

	@Override
	public void onStartRequest(Request<Object> request, Response<ServiceInstance> lbResponse) {
		// 判断上下文类型是否是TimedRequestContext
		if (request.getContext() instanceof TimedRequestContext) {
			((TimedRequestContext) request.getContext()).setRequestStartTime(System.nanoTime());
		}
		// 如果请求中没有服务对象将直接结束处理
		if (!lbResponse.hasServer()) {
			return;
		}
		// 从请求中获取服务实例
		ServiceInstance serviceInstance = lbResponse.getServer();
		// 置入数据缓存
		AtomicLong activeRequestsCounter =
				activeRequestsPerInstance.computeIfAbsent(serviceInstance, instance -> {
					AtomicLong createdCounter = new AtomicLong();
					Gauge.builder("loadbalancer.requests.active", () -> createdCounter)
							.tags(buildServiceInstanceTags(serviceInstance))
							.register(meterRegistry);
					return createdCounter;
				});
		// 累加1
		activeRequestsCounter.incrementAndGet();
	}

	@Override
	public void onComplete(CompletionContext<Object, ServiceInstance, Object> completionContext) {
		// 获取当前时间
		long requestFinishedTimestamp = System.nanoTime();
		// 上下文状态是否是DISCARD
		if (CompletionContext.Status.DISCARD.equals(completionContext.status())) {
			// micrometer相关操作
			Counter.builder("loadbalancer.requests.discard")
					.tags(buildDiscardedRequestTags(completionContext))
					.register(meterRegistry).increment();
			return;
		}
		// 获取服务实例
		ServiceInstance serviceInstance = completionContext.getLoadBalancerResponse().getServer();
		// 获取服务实例的使用次数
		AtomicLong activeRequestsCounter = activeRequestsPerInstance.get(serviceInstance);
		// 使用次数不为空则减一
		if (activeRequestsCounter != null) {
			activeRequestsCounter.decrementAndGet();
		}
		// 获取上下文
		Object loadBalancerRequestContext = completionContext.getLoadBalancerRequest().getContext();
		// 对上下文中的时间进行检查
		if (requestHasBeenTimed(loadBalancerRequestContext)) {
			// 如果处理状态是失败
			if (CompletionContext.Status.FAILED.equals(completionContext.status())) {
				Timer.builder("loadbalancer.requests.failed")
						.tags(buildFailedRequestTags(completionContext))
						.register(meterRegistry)
						.record(requestFinishedTimestamp
										- ((TimedRequestContext) loadBalancerRequestContext).getRequestStartTime(),
								TimeUnit.NANOSECONDS);
				return;
			}
			// 非失败状态
			Timer.builder("loadbalancer.requests.success")
					.tags(buildSuccessRequestTags(completionContext))
					.register(meterRegistry)
					.record(requestFinishedTimestamp
									- ((TimedRequestContext) loadBalancerRequestContext).getRequestStartTime(),
							TimeUnit.NANOSECONDS);
		}
	}

	private boolean requestHasBeenTimed(Object loadBalancerRequestContext) {
		return loadBalancerRequestContext instanceof TimedRequestContext
				&& (((TimedRequestContext) loadBalancerRequestContext).getRequestStartTime() != 0L);
	}

}
