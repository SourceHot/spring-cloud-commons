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

package org.springframework.cloud.client;

import java.net.URI;
import java.util.Map;

/**
 * Represents an instance of a service in a discovery system.
 *
 * 服务实例集合
 * @author Spencer Gibb
 * @author Tim Ysewyn
 */
public interface ServiceInstance {

	/**
	 * 获取实例id
	 * @return The unique instance ID as registered.
	 */
	default String getInstanceId() {
		return null;
	}

	/**
	 * 获取实例id
	 * @return The service ID as registered.
	 */
	String getServiceId();

	/**
	 * 获取host
	 * @return The hostname of the registered service instance.
	 */
	String getHost();

	/**
	 * 获取port
	 * @return The port of the registered service instance.
	 */
	int getPort();

	/**
	 * 是否安全，即是否使用https
	 * @return Whether the port of the registered service instance uses HTTPS.
	 */
	boolean isSecure();

	/**
	 * 获取uri对象
	 * @return The service URI address.
	 */
	URI getUri();

	/**
	 * 获取元数据
	 * @return The key / value pair metadata associated with the service instance.
	 */
	Map<String, String> getMetadata();

	/**
	 * 获取服务实例的方案
	 * @return The scheme of the service instance.
	 */
	default String getScheme() {
		return null;
	}

}
