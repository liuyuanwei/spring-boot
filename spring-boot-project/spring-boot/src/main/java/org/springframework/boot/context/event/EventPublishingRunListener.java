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

package org.springframework.boot.context.event;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ErrorHandler;

import java.util.List;

/**
 * {@link SpringApplicationRunListener} to publish {@link SpringApplicationEvent}s.
 * <p>
 * Uses an internal {@link ApplicationEventMulticaster} for the events that are fired
 * before the context is actually refreshed.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Artsiom Yudovin
 * 实现 SpringApplicationRunListener、Ordered 接口，
 * 将 SpringApplicationRunListener 监听到的事件，】】】【转换成对应的 SpringApplicationEvent 事件，发布到监听器们】。
 */
/*
	通过这样的方式，可以很方便的将 SpringApplication 启动的各种事件，方便的修改成对应的 SpringApplicationEvent 事件。
	这样，我们就可以不需要修改 SpringApplication 的代码。
	】】】或者说，我们认为 EventPublishingRunListener 是一个“转换器”。
 */
public class EventPublishingRunListener implements SpringApplicationRunListener, Ordered {

    /**
     * Spring 应用
     */
	private final SpringApplication application;
    /**
     * 参数集合
     */
	private final String[] args;
    /**
     * 事件广播器
	 * 该类是 Spring 的事件广播器，也就是通过它来广播各种事件。
     */
	private final SimpleApplicationEventMulticaster initialMulticaster;


	/*
		可以看到，通过构造方法创建 EventPublishingRunListener 实例的过程中，调用了 getListeners 方法
		将 SpringApplication 中所有 ApplicationListener 监听器关联到了 initialMulticaster 属性中。
		没错，这里的 ApplicationListener 监听器就是上篇文章中在 SpringApplication 准备阶段从 spring.factories 文件加载的 key 为 ApplicationListener 的实现类集合，
		该实现类集合全部重写了 onApplicationEvent 方法。
	 */
	public EventPublishingRunListener(SpringApplication application, String[] args) {
		this.application = application;
		this.args = args;
		// 创建 SimpleApplicationEventMulticaster 对象
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
		// 添加应用的监听器们，到 initialMulticaster 中
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}

		/**
		 * application.getListeners()返回的是这个
		 * ApplicationListener 数组
		 * private List<ApplicationListener<?>> listeners;
		 *
		 */
	}


	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void starting() {
		this.initialMulticaster.multicastEvent(new ApplicationStartingEvent(this.application, this.args));
	}

	@Override // ApplicationEnvironmentPreparedEvent
	public void environmentPrepared(ConfigurableEnvironment environment) {
		this.initialMulticaster.multicastEvent(new ApplicationEnvironmentPreparedEvent(this.application, this.args, environment));
	}

	@Override // ApplicationContextInitializedEvent
	public void contextPrepared(ConfigurableApplicationContext context) {
		this.initialMulticaster.multicastEvent(new ApplicationContextInitializedEvent(this.application, this.args, context));
	}

	@Override // ApplicationPreparedEvent
	public void contextLoaded(ConfigurableApplicationContext context) {
		for (ApplicationListener<?> listener : this.application.getListeners()) {
			if (listener instanceof ApplicationContextAware) {
				((ApplicationContextAware) listener).setApplicationContext(context);
			}
			context.addApplicationListener(listener);
		}
		this.initialMulticaster.multicastEvent(new ApplicationPreparedEvent(this.application, this.args, context));
	}

	@Override // ApplicationStartedEvent
	public void started(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationStartedEvent(this.application, this.args, context));
	}

	@Override
	public void running(ConfigurableApplicationContext context) {
		context.publishEvent(new ApplicationReadyEvent(this.application, this.args, context));
	}

	@Override // ApplicationFailedEvent
	public void failed(ConfigurableApplicationContext context, Throwable exception) {
		ApplicationFailedEvent event = new ApplicationFailedEvent(this.application, this.args, context, exception);
		if (context != null && context.isActive()) {
			// Listeners have been registered to the application context so we should
			// use it at this point if we can
			context.publishEvent(event);
		} else {
			// An inactive context may not have a multicaster so we use our multicaster to
			// call all of the context's listeners instead
			if (context instanceof AbstractApplicationContext) {
				for (ApplicationListener<?> listener : ((AbstractApplicationContext) context)
						.getApplicationListeners()) {
					this.initialMulticaster.addApplicationListener(listener);
				}
			}
			this.initialMulticaster.setErrorHandler(new LoggingErrorHandler());
			this.initialMulticaster.multicastEvent(event);
		}
	}

	private static class LoggingErrorHandler implements ErrorHandler {

		private static Log logger = LogFactory.getLog(EventPublishingRunListener.class);

		@Override
		public void handleError(Throwable throwable) {
			logger.warn("Error calling ApplicationEventListener", throwable);
		}

	}

}
