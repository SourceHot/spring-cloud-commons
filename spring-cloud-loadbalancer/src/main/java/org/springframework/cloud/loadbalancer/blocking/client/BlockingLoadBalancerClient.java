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

package org.springframework.cloud.loadbalancer.blocking.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import static org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer.REQUEST;

/**
 * The default {@link LoadBalancerClient} implementation.
 *
 * @author Olga Maciaszek-Sharma
 * @since 2.2.0
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class BlockingLoadBalancerClient implements LoadBalancerClient {

	private final LoadBalancerClientFactory loadBalancerClientFactory;

	private final LoadBalancerProperties properties;

	public BlockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory,
									  LoadBalancerProperties properties) {
		this.loadBalancerClientFactory = loadBalancerClientFactory;
		this.properties = properties;

	}

	@Override
	public <T> T execute(String serviceId, LoadBalancerRequest<T> request)
			throws IOException {
		// 获取hint数据
		String hint = getHint(serviceId);
		// 创建负载均衡下的请求适配器对象
		LoadBalancerRequestAdapter<T, DefaultRequestContext> lbRequest =
				new LoadBalancerRequestAdapter<>(
						request, new DefaultRequestContext(request, hint));
		// 获取负载均衡生命周期集合
		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = getSupportedLifecycleProcessors(
				serviceId);
		// 执行负载均衡生命周期中的onStart方法
		supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
		// 进行服务实例选择
		ServiceInstance serviceInstance = choose(serviceId, lbRequest);
		// 如果服务实例为空
		if (serviceInstance == null) {
			// 执行负载均衡生命周期中的onComplete方法
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
					new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest,
							new EmptyResponse())));
			// 抛出异常
			throw new IllegalStateException("No instances available for " + serviceId);
		}
		// 进行最终的处理
		return execute(serviceId, serviceInstance, lbRequest);
	}

	@Override
	public <T> T execute(String serviceId, ServiceInstance serviceInstance,
						 LoadBalancerRequest<T> request) throws IOException {
		// 创建响应对象
		DefaultResponse defaultResponse = new DefaultResponse(serviceInstance);
		// 获取负载均衡生命周期集合
		Set<LoadBalancerLifecycle> supportedLifecycleProcessors = getSupportedLifecycleProcessors(
				serviceId);
		// 创建请求对象
		Request lbRequest =
				request instanceof Request ? (Request) request : new DefaultRequest<>();
		// 执行负载均衡生命周期中的onStartRequest方法
		supportedLifecycleProcessors.forEach(
				lifecycle -> lifecycle.onStartRequest(lbRequest,
						new DefaultResponse(serviceInstance)));
		try {
			// 发出请求接收响应
			T response = request.apply(serviceInstance);
			// 从响应中获取客户端响应内容
			Object clientResponse = getClientResponse(response);
			// 执行负载均衡生命周期中的onComplete方法
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
					new CompletionContext<>(CompletionContext.Status.SUCCESS, lbRequest,
							defaultResponse, clientResponse)));

			// 返回响应
			return response;
		} catch (IOException iOException) {
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
					new CompletionContext<>(CompletionContext.Status.FAILED, iOException,
							lbRequest, defaultResponse)));
			throw iOException;
		} catch (Exception exception) {
			supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onComplete(
					new CompletionContext<>(CompletionContext.Status.FAILED, exception,
							lbRequest, defaultResponse)));
			ReflectionUtils.rethrowRuntimeException(exception);
		}
		return null;
	}

	private <T> Object getClientResponse(T response) {
		ClientHttpResponse clientHttpResponse = null;
		if (response instanceof ClientHttpResponse) {
			clientHttpResponse = (ClientHttpResponse) response;
		}
		if (clientHttpResponse != null) {
			try {
				return new ResponseData(clientHttpResponse, null);
			} catch (IOException ignored) {
			}
		}
		return response;
	}

	private Set<LoadBalancerLifecycle> getSupportedLifecycleProcessors(String serviceId) {
		return LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(
				loadBalancerClientFactory.getInstances(serviceId,
						LoadBalancerLifecycle.class), DefaultRequestContext.class,
				Object.class, ServiceInstance.class);
	}

	@Override
	public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
		return LoadBalancerUriTools.reconstructURI(serviceInstance, original);
	}

	@Override
	public ServiceInstance choose(String serviceId) {
		return choose(serviceId, REQUEST);
	}

	@Override
	public <T> ServiceInstance choose(String serviceId, Request<T> request) {
		// 通过负载均衡客户端工厂创建响应式的负载均衡接口
		ReactiveLoadBalancer<ServiceInstance> loadBalancer = loadBalancerClientFactory.getInstance(
				serviceId);
		// 如果响应式的负载均衡接口为空
		if (loadBalancer == null) {
			return null;
		}
		// 从响应式的负载均衡接口中获取服务实例
		Response<ServiceInstance> loadBalancerResponse = Mono.from(
				loadBalancer.choose(request)).block();
		if (loadBalancerResponse == null) {
			return null;
		}
		// 返回服务实例
		return loadBalancerResponse.getServer();
	}

	private String getHint(String serviceId) {
		String defaultHint = properties.getHint().getOrDefault("default", "default");
		String hintPropertyValue = properties.getHint().get(serviceId);
		return hintPropertyValue != null ? hintPropertyValue : defaultHint;
	}

}
