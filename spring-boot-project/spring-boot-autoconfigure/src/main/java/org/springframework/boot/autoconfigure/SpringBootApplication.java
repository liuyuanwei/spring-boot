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

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Indicates a {@link Configuration configuration} class that declares one or more
 * {@link Bean @Bean} methods and also triggers {@link EnableAutoConfiguration
 * auto-configuration} and {@link ComponentScan component scanning}. This is a convenience
 * annotation that is equivalent to declaring {@code @Configuration},
 * {@code @EnableAutoConfiguration} and {@code @ComponentScan}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented

/*
	Java 自带的注解。
	使用此注解声明出来的自定义注解，在使用此自定义注解时，如果注解在类上面时，子类会自动继承此注解，否则的话，子类不会继承此注解。
	使用 @Inherited 声明出来的注解，只有在类上使用时才会有效，对方法，属性等其他无效。
 */
@Inherited
/*
	Spring Boot 自定义的注解
	标记这是一个 Spring Boot 配置类。
	它上面继承自 @Configuration 注解，【所以两者功能也一致】，
	可以将当前类内声明的一个或多个以 @Bean 注解标记的方法的实例纳入到 Srping 容器中，并且实例名就是方法名。
 */
@SpringBootConfiguration
/*
	】】】激活自动配置功能，
	】】】是 spring-boot-autoconfigure 项目最核心的注解。
	其中默认路径扫描以及组件装配、排除等都通过它来实现。
 */
@EnableAutoConfiguration
/*
	扫描指定路径下的 Component（@Componment、@Configuration、@Service 等等）。
	】】】用来扫描被 @Component标注的类 ，【只不过这里是用来过滤 Bean 的】，指定哪些类不进行扫描，而且用的是自定义规则。
 */
@ComponentScan(excludeFilters = {
		@Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
		@Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })
public @interface SpringBootApplication {
	/*
		通过使用它，不仅仅能标记这是一个 Spring Boot 应用，而且能够开启自动配置的功能。
	 */


	/**
	 * 根据class来排除，排除指定的类加入spring容器，传入的类型是class类型。
	 * 且继承自 @EnableAutoConfiguration 中的属性。
	 */
	@AliasFor(annotation = EnableAutoConfiguration.class)
	Class<?>[] exclude() default {};

	/**
	 * 根据class name来排除，排除特定的类加入spring容器，参数类型是class的全类名字符串数组。
	 * 同样继承自 @EnableAutoConfiguration。
	 */
	@AliasFor(annotation = EnableAutoConfiguration.class)
	String[] excludeName() default {};

	/**
	 * 可以指定多个包名进行扫描。继承自 @ComponentScan
	 * @since 1.3.0
	 */
	@AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
	String[] scanBasePackages() default {};

	/**
	 * 可以指定多个类或接口的class，然后扫描 class 所在包下的所有组件。同样继承自 @ComponentScan 。
	 * @since 1.3.0
	 */
	@AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
	Class<?>[] scanBasePackageClasses() default {};

}