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

package org.springframework.boot;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.support.SpringFactoriesLoader;

/**
 * SpringApplicationRunListener 接口
 *
 * Listener for the {@link SpringApplication} {@code run} method.
 * {@link SpringApplicationRunListener}s are loaded via the {@link SpringFactoriesLoader}
 * and should declare a public constructor that accepts a {@link SpringApplication}
 * instance and a {@code String[]} of arguments. A new
 * {@link SpringApplicationRunListener} instance will be created for each run.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * SpringApplication 运行的监听器接口
 * 】】】目前，SpringApplicationRunListener 的实现类，只有 EventPublishingRunListener 类。
 */
public interface SpringApplicationRunListener {

	// 在run()方法开始执行时被调用，表示应用刚刚启动，对应的 Spring Boot 事件为 ApplicationStartingEvent
	void starting();

	// ConfigurableEnvironment 构建完成时调用，对应的 Spring Boot 事件为 ApplicationEnvironmentPreparedEvent
	void environmentPrepared(ConfigurableEnvironment environment);

	// ApplicationContext 构建完成时调用，对应的 Spring Boot 事件为 ApplicationContextInitializedEvent
	void contextPrepared(ConfigurableApplicationContext context);

	// ApplicationContext 完成加载但还未启动时调用，对应的 Spring Boot 事件为 ApplicationPreparedEvent
	void contextLoaded(ConfigurableApplicationContext context);

	// ApplicationContext 已启动，但 callRunners 还未执行时调用，对应的 Spring Boot 事件为 ApplicationStartedEvent
	void started(ConfigurableApplicationContext context);

	// ApplicationContext 启动完毕被调用，对应的 Spring Boot 事件为 ApplicationReadyEvent
	void running(ConfigurableApplicationContext context);

	// 应用出错时被调用，对应的 Spring Boot 事件为 ApplicationFailedEvent
	void failed(ConfigurableApplicationContext context, Throwable exception);

}
