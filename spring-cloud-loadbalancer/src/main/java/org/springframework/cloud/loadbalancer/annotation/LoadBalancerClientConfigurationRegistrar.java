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

package org.springframework.cloud.loadbalancer.annotation;

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 */
public class LoadBalancerClientConfigurationRegistrar implements ImportBeanDefinitionRegistrar {

	private static String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String value = (String) client.get("value");
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("name");
		}
		if (StringUtils.hasText(value)) {
			return value;
		}
		throw new IllegalStateException("Either 'name' or 'value' must be provided in @LoadBalancerClient");
	}

	private static void registerClientConfiguration(BeanDefinitionRegistry registry, Object name,
			Object configuration) {
		// 构造bean定义，构造标准是LoadBalancerClientSpecification类
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(LoadBalancerClientSpecification.class);
		builder.addConstructorArgValue(name);
		builder.addConstructorArgValue(configuration);
		registry.registerBeanDefinition(name + ".LoadBalancerClientSpecification", builder.getBeanDefinition());
	}

	@Override
	public void registerBeanDefinitions(AnnotationMetadata metadata,
										BeanDefinitionRegistry registry) {
		// 读取LoadBalancerClients注解信息
		Map<String, Object> attrs =
				metadata.getAnnotationAttributes(LoadBalancerClients.class.getName(), true);
		// 如果注解不为空并且value属性存在
		if (attrs != null && attrs.containsKey("value")) {
			// 提取value属性
			AnnotationAttributes[] clients = (AnnotationAttributes[]) attrs.get("value");
			// 循环value属性进行注册
			for (AnnotationAttributes client : clients) {
				// 注册客户端配置
				registerClientConfiguration(registry, getClientName(client),
						client.get("configuration"));
			}
		}
		if (attrs != null && attrs.containsKey("defaultConfiguration")) {
			String name;
			if (metadata.hasEnclosingClass()) {
				name = "default." + metadata.getEnclosingClassName();
			} else {
				name = "default." + metadata.getClassName();
			}
			registerClientConfiguration(registry, name, attrs.get("defaultConfiguration"));
		}
		Map<String, Object> client =
				metadata.getAnnotationAttributes(LoadBalancerClient.class.getName(), true);
		String name = getClientName(client);
		if (name != null) {
			registerClientConfiguration(registry, name, client.get("configuration"));
		}
	}

}
