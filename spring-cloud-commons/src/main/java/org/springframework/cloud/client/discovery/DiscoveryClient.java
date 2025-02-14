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

package org.springframework.cloud.client.discovery;

import java.util.List;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.core.Ordered;

/**
 * Represents read operations commonly available to discovery services such as Netflix
 * Eureka or consul.io.
 *
 * 服务发现器
 *
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Chris Bono
 */
public interface DiscoveryClient extends Ordered {

	/**
	 * Default order of the discovery client. 默认序号
	 */
	int DEFAULT_ORDER = 0;

	/**
	 * A human-readable description of the implementation, used in HealthIndicator. 获取描述
	 * @return The description.
	 */
	String description();

	/**
	 * Gets all ServiceInstances associated with a particular serviceId. 根据服务id获取服务实例集合
	 * @param serviceId The serviceId to query.
	 * @return A List of ServiceInstance.
	 */
	List<ServiceInstance> getInstances(String serviceId);

	/**
	 * 获取所有服务实例id
	 * @return All known service IDs.
	 */
	List<String> getServices();

	/**
	 * Can be used to verify the client is valid and able to make calls.
	 * <p>
	 * A successful invocation with no exception thrown implies the client is able to make
	 * calls.
	 * <p>
	 * The default implementation simply calls {@link #getServices()} - client
	 * implementations can override with a lighter weight operation if they choose to.
	 * 探测接口
	 */
	default void probe() {
		getServices();
	}

	/**
	 * Default implementation for getting order of discovery clients. 获取序号
	 * @return order
	 */
	@Override
	default int getOrder() {
		return DEFAULT_ORDER;
	}

}
