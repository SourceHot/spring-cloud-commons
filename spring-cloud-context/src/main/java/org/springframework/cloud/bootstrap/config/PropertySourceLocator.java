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

package org.springframework.cloud.bootstrap.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

/**
 * Strategy for locating (possibly remote) property sources for the Environment.
 * Implementations should not fail unless they intend to prevent the application from
 * starting.
 *
 * @author Dave Syer
 *
 */
public interface PropertySourceLocator {

	/**
	 * @param environment The current Environment.
	 * @return A PropertySource, or null if there is none.
	 * @throws IllegalStateException if there is a fail-fast condition.
	 */
	PropertySource<?> locate(Environment environment);

	default Collection<PropertySource<?>> locateCollection(Environment environment) {
		return locateCollection(this, environment);
	}

	static Collection<PropertySource<?>> locateCollection(PropertySourceLocator locator, Environment environment) {
		// 通过PropertySourceLocator对象的locate方法加载属性源
		PropertySource<?> propertySource = locator.locate(environment);
		// 属性源为空返回空集合
		if (propertySource == null) {
			return Collections.emptyList();
		}
		// 确认属性源是否是CompositePropertySource的实例,如果不是则直接将其转换为Collection类型后返回
		if (CompositePropertySource.class.isInstance(propertySource)) {
			// 获取属性源集合
			Collection<PropertySource<?>> sources = ((CompositePropertySource) propertySource).getPropertySources();
			// 创建返回对象
			List<PropertySource<?>> filteredSources = new ArrayList<>();
			// 从属性源集合中循环，过滤空数据将其放入返回对象
			for (PropertySource<?> p : sources) {
				if (p != null) {
					filteredSources.add(p);
				}
			}
			// 返回
			return filteredSources;
		}
		else {
			return Arrays.asList(propertySource);
		}
	}

}
