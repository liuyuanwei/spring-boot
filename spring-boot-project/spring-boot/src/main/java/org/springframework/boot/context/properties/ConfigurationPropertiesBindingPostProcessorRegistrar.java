/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.properties;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

/**
 * {@link ImportBeanDefinitionRegistrar} for binding externalized application properties
 * to {@link ConfigurationProperties} beans.
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
public class ConfigurationPropertiesBindingPostProcessorRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (!registry.containsBeanDefinition(ConfigurationPropertiesBindingPostProcessor.BEAN_NAME)) {
		    // <1> 注册 ConfigurationPropertiesBindingPostProcessor BeanDefinition
			registerConfigurationPropertiesBindingPostProcessor(registry);
			// <2> 注册 ConfigurationBeanFactoryMetadata BeanDefinition
			registerConfigurationBeanFactoryMetadata(registry);
		}
	}

	private void registerConfigurationPropertiesBindingPostProcessor(BeanDefinitionRegistry registry) {
		// 创建 GenericBeanDefinition 对象
	    GenericBeanDefinition definition = new GenericBeanDefinition();
		definition.setBeanClass(ConfigurationPropertiesBindingPostProcessor.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		// 注册
		registry.registerBeanDefinition(ConfigurationPropertiesBindingPostProcessor.BEAN_NAME, definition);
	}

	private void registerConfigurationBeanFactoryMetadata(BeanDefinitionRegistry registry) {
        // 创建 GenericBeanDefinition 对象
        GenericBeanDefinition definition = new GenericBeanDefinition();
		definition.setBeanClass(ConfigurationBeanFactoryMetadata.class);
		definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        // 注册
        registry.registerBeanDefinition(ConfigurationBeanFactoryMetadata.BEAN_NAME, definition);
	}

}
