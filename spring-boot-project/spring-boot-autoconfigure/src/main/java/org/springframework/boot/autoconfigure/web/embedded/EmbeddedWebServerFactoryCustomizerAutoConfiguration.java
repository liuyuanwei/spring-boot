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

package org.springframework.boot.autoconfigure.web.embedded;

import io.undertow.Undertow;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.UpgradeProtocol;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Loader;
import org.eclipse.jetty.webapp.WebAppContext;
import org.xnio.SslClientAuthMode;
import reactor.netty.http.server.HttpServer;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for embedded servlet and reactive
 * web servers customizations.
 *
 * @author Phillip Webb
 * @since 2.0.0
 *
 * 负责创建内嵌的 Tomcat、Jetty 等等 Web 服务器的配置类。
 * 因为它的目的是自动配置，所以类名以 AutoConfiguration 作为后缀。
 *
 * <1.1>在类上添加了 @Configuration 注解，声明这是一个配置类。
 *
 *
 * <2> 处，在类上添加了 @ConditionalOnWebApplication 条件注解，
 * 表示当前配置类需要在当前项目是 Web 项目的条件下，才能生效。
 * 在 Spring Boot 项目中，会将项目类型分成 Web 项目（使用 SpringMVC 或者 WebFlux）和非 Web 项目。
 *
 * <3.1> 处，使用 @EnableConfigurationProperties 注解，让 ServerProperties 配置属性类生效。
 * 在 Spring Boot 定义了 @ConfigurationProperties 注解，用于声明配置属性类，将指定前缀的配置项批量注入到该类中。
 */
@Configuration // <1.1>
@ConditionalOnWebApplication // <2.1>
@EnableConfigurationProperties(ServerProperties.class) // <3.1>
public class EmbeddedWebServerFactoryCustomizerAutoConfiguration {

	/*
		<1.2>、<1.3>  处，分别是用于初始化 Tomcat、Jetty 相关 Bean 的配置类。

		<2.1>、<2.2> 处，在类上添加了 @ConditionalOnClass 条件注解，表示当前配置类需要在当前项目有指定类的条件下，才能生效。
			TomcatWebServerFactoryCustomizerConfiguration 配置类，需要有 tomcat-embed-core 依赖提供的 Tomcat、UpgradeProtocol 依赖类，才能创建内嵌的 Tomcat 服务器。
			JettyWebServerFactoryCustomizer 配置类，需要有 jetty-server 依赖提供的 Server、Loader、WebAppContext 类，才能创建内嵌的 Jetty 服务器。

	 	<3.2>、<3.3> 处，在创建 TomcatWebServerFactoryCustomizer 和 JettyWebServerFactoryCustomizer 对象时，
	 	都会将 ServerProperties 传入其中，作为后续创建的 Web 服务器的配置。也就是说，我们通过修改在配置文件的配置项，就可以自定义 Web 服务器的配置。
	 */

	/**
	 * TomcatWebServerFactoryCustomizerConfiguration 配置类，
	 * 负责创建 TomcatWebServerFactoryCustomizer Bean，从而初始化内嵌的 Tomcat 并进行启动。
	 */
	@Configuration // <1.2>
	@ConditionalOnClass({ Tomcat.class, UpgradeProtocol.class })
	public static class TomcatWebServerFactoryCustomizerConfiguration {

		@Bean
		public TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer(
				Environment environment, ServerProperties serverProperties) {
			// <3.2>
			return new TomcatWebServerFactoryCustomizer(environment, serverProperties);
		}

	}

	/**
	 * JettyWebServerFactoryCustomizer 配置类，
	 * 负责创建 JettyWebServerFactoryCustomizer Bean，从而初始化内嵌的 Jetty 并进行启动。
	 */
	@Configuration // <1.3>
	@ConditionalOnClass({ Server.class, Loader.class, WebAppContext.class })
	public static class JettyWebServerFactoryCustomizerConfiguration {

		@Bean
		public JettyWebServerFactoryCustomizer jettyWebServerFactoryCustomizer(
				Environment environment, ServerProperties serverProperties) {
			// <3.3>
			return new JettyWebServerFactoryCustomizer(environment, serverProperties);
		}

	}

	/**
	 * Nested configuration if Undertow is being used.
	 */
	@Configuration
	@ConditionalOnClass({ Undertow.class, SslClientAuthMode.class })
	public static class UndertowWebServerFactoryCustomizerConfiguration {

		@Bean
		public UndertowWebServerFactoryCustomizer undertowWebServerFactoryCustomizer(
				Environment environment, ServerProperties serverProperties) {
			return new UndertowWebServerFactoryCustomizer(environment, serverProperties);
		}

	}

	/**
	 * Nested configuration if Netty is being used.
	 */
	@Configuration
	@ConditionalOnClass(HttpServer.class)
	public static class NettyWebServerFactoryCustomizerConfiguration {

		@Bean
		public NettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(
				Environment environment, ServerProperties serverProperties) {
			return new NettyWebServerFactoryCustomizer(environment, serverProperties);
		}

	}

}
