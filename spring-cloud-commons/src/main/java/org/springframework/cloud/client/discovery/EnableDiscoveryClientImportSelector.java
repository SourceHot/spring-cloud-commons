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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.cloud.commons.util.SpringFactoryImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.type.AnnotationMetadata;

/**
 * @author Spencer Gibb
 */
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class EnableDiscoveryClientImportSelector extends SpringFactoryImportSelector<EnableDiscoveryClient> {

	@Override
	public String[] selectImports(AnnotationMetadata metadata) {
		// 获取需要导入的类名集合
		String[] imports = super.selectImports(metadata);

		// 获取EnableDiscoveryClient注解的属性表
		AnnotationAttributes attributes = AnnotationAttributes
				.fromMap(metadata.getAnnotationAttributes(getAnnotationClass().getName(), true));

		// 获取是否需要自动注入属性
		boolean autoRegister = attributes.getBoolean("autoRegister");

		// 如果需要自动注入则会添加AutoServiceRegistrationConfiguration类
		if (autoRegister) {
			List<String> importsList = new ArrayList<>(Arrays.asList(imports));
			importsList.add("org.springframework.cloud.client.serviceregistry.AutoServiceRegistrationConfiguration");
			imports = importsList.toArray(new String[0]);
		}
		// 如果不需要自动注入
		else {
			// 获取环境对象
			Environment env = getEnvironment();
			// 确认环境对象是ConfigurableEnvironment类型
			if (ConfigurableEnvironment.class.isInstance(env)) {
				// 类型转换
				ConfigurableEnvironment configEnv = (ConfigurableEnvironment) env;
				// 创建属性容器
				LinkedHashMap<String, Object> map = new LinkedHashMap<>();
				// 向属性容器中放入spring.cloud.service-registry.auto-registration.enabled属性，属性值为false
				map.put("spring.cloud.service-registry.auto-registration.enabled", false);
				// 创建springCloudDiscoveryClient属性名称的属性源，数据值是属性容器
				MapPropertySource propertySource = new MapPropertySource("springCloudDiscoveryClient", map);
				// 向环境对象最后加入属性源
				configEnv.getPropertySources().addLast(propertySource);
			}

		}

		return imports;
	}

	/**
	 * 确认是否已启用
	 */
	@Override
	protected boolean isEnabled() {
		// 获取环境对象中的spring.cloud.discovery.enabled数据
		return getEnvironment().getProperty("spring.cloud.discovery.enabled", Boolean.class, Boolean.TRUE);
	}

	@Override
	protected boolean hasDefaultFactory() {
		return true;
	}

}
