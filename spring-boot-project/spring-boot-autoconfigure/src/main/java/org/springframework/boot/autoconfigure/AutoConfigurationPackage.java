/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Indicates that the package containing the annotated class should be registered with
 * {@link AutoConfigurationPackages}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see AutoConfigurationPackages
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
/*
	@Import 导入了 AutoConfigurationPackages.Registrar 类，该类实现了 ImportBeanDefinitionRegistrar 接口
 */
/*
	主要功能自动配置包，它会获取主程序类所在的包路径，
	】】】并将包路径（包括子包）下的所有组件注册到 Spring IOC 容器中。

	这是用来将启动类所在包，以及下面所有子包里面的所有组件扫描到Spring容器中，
	】】】这里的组件是指被 @Component或其派生注解标注的类。【这也就是为什么不用标注@ComponentScan的原因。】
 */
/*
	AutoConfigurationPackages

	简单来说，就是将使用 @AutoConfigurationPackage 注解的类所在的包（package），注册成一个 Spring IoC 容器中的 Bean 。
	酱紫，后续有其它模块需要使用，就可以通过获得该 Bean ，从而获得所在的包。例如说，JPA 模块，需要使用到。
 */
@Import(AutoConfigurationPackages.Registrar.class)
public @interface AutoConfigurationPackage {

}