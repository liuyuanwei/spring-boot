/*
 * Copyright 2012-2019 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.groovy.GroovyBeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.web.reactive.context.StandardReactiveWebEnvironment;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.env.*;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.*;
import org.springframework.web.context.support.StandardServletEnvironment;

import java.lang.reflect.Constructor;
import java.security.AccessControlException;
import java.util.*;

/**
 * Class that can be used to bootstrap and launch a Spring application from a Java main
 * method. By default class will perform the following steps to bootstrap your
 * application:
 *
 * <ul>
 * <li>Create an appropriate {@link ApplicationContext} instance (depending on your
 * classpath)</li>
 * <li>Register a {@link CommandLinePropertySource} to expose command line arguments as
 * Spring properties</li>
 * <li>Refresh the application context, loading all singleton beans</li>
 * <li>Trigger any {@link CommandLineRunner} beans</li>
 * </ul>
 *
 * In most circumstances the static {@link #run(Class, String[])} method can be called
 * directly from your {@literal main} method to bootstrap your application:
 *
 * <pre class="code">
 * &#064;Configuration
 * &#064;EnableAutoConfiguration
 * public class MyApplication  {
 *
 *   // ... Bean definitions
 *
 *   public static void main(String[] args) {
 *     SpringApplication.run(MyApplication.class, args);
 *   }
 * }
 * </pre>
 *
 * <p>
 * For more advanced configuration a {@link SpringApplication} instance can be created and
 * customized before being run:
 *
 * <pre class="code">
 * public static void main(String[] args) {
 *   SpringApplication application = new SpringApplication(MyApplication.class);
 *   // ... customize application settings here
 *   application.run(args)
 * }
 * </pre>
 *
 * {@link SpringApplication}s can read beans from a variety of different sources. It is
 * generally recommended that a single {@code @Configuration} class is used to bootstrap
 * your application, however, you may also set {@link #getSources() sources} from:
 * <ul>
 * <li>The fully qualified class name to be loaded by
 * {@link AnnotatedBeanDefinitionReader}</li>
 * <li>The location of an XML resource to be loaded by {@link XmlBeanDefinitionReader}, or
 * a groovy script to be loaded by {@link GroovyBeanDefinitionReader}</li>
 * <li>The name of a package to be scanned by {@link ClassPathBeanDefinitionScanner}</li>
 * </ul>
 *
 * Configuration properties are also bound to the {@link SpringApplication}. This makes it
 * possible to set {@link SpringApplication} properties dynamically, like additional
 * sources ("spring.main.sources" - a CSV list) the flag to indicate a web environment
 * ("spring.main.web-application-type=none") or the flag to switch off the banner
 * ("spring.main.banner-mode=off").
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Jeremy Rickard
 * @author Craig Burke
 * @author Michael Simons
 * @author Madhura Bhave
 * @author Brian Clozel
 * @author Ethan Rubinson
 * @see #run(Class, String[])
 * @see #run(Class[], String[])
 * @see #SpringApplication(Class...)
 * Spring 应用启动器。正如其代码上所添加的注释，
 * 它来提供启动 Spring 应用的功能。
 *
 * SpringApplication 在运行前做了一系列的准备工作，
 * 如：推断 Web 应用类型、加载 Spring 的上下文初始化器和事件监听器以及配置默认属性等。
 */
public class SpringApplication {

	/**
	 * The class name of application context that will be used by default for non-web
	 * environments.
	 */
	public static final String DEFAULT_CONTEXT_CLASS = "org.springframework.context."
			+ "annotation.AnnotationConfigApplicationContext";

	/**
	 * The class name of application context that will be used by default for web
	 * environments.
	 */
	public static final String DEFAULT_SERVLET_WEB_CONTEXT_CLASS = "org.springframework.boot."
			+ "web.servlet.context.AnnotationConfigServletWebServerApplicationContext";

	/**
	 * The class name of application context that will be used by default for reactive web
	 * environments.
	 */
	public static final String DEFAULT_REACTIVE_WEB_CONTEXT_CLASS = "org.springframework."
			+ "boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext";

	/**
	 * Default banner location.
	 */
	public static final String BANNER_LOCATION_PROPERTY_VALUE = SpringApplicationBannerPrinter.DEFAULT_BANNER_LOCATION;

	/**
	 * Banner location property key.
	 */
	public static final String BANNER_LOCATION_PROPERTY = SpringApplicationBannerPrinter.BANNER_LOCATION_PROPERTY;

	private static final String SYSTEM_PROPERTY_JAVA_AWT_HEADLESS = "java.awt.headless";

	private static final Log logger = LogFactory.getLog(SpringApplication.class);

    /**
     * 主要的 Java Config 类的数组
     */
	private Set<Class<?>> primarySources;

	private Set<String> sources = new LinkedHashSet<>();

    /**
     * 启动 Application 的类名
     */
	private Class<?> mainApplicationClass;

    /**
     * Banner 模式
     */
	private Banner.Mode bannerMode = Banner.Mode.CONSOLE;

	private boolean logStartupInfo = true;

    /**
     * 是否添加 JVM 启动参数
     */
	private boolean addCommandLineProperties = true;

    /**
     * 是否添加共享的 ConversionService
     */
	private boolean addConversionService = true;

    /**
     * Banner 对象
     */
	private Banner banner;

    /**
     * 资源加载器
	 * resourceLoader 主要用来获取 Resource 及 ClassLoader。这里值为 null
     */
	private ResourceLoader resourceLoader;

	private BeanNameGenerator beanNameGenerator;

	private ConfigurableEnvironment environment;

	private Class<? extends ConfigurableApplicationContext> applicationContextClass;

    /**
     * Web 应用类型
     */
	private WebApplicationType webApplicationType;

    /**
     * 是否 AWT headless
     */
	private boolean headless = true;

    /**
     * 是否注册 ShutdownHook 钩子
     */
	private boolean registerShutdownHook = true;

    /**
     * ApplicationContextInitializer 数组
     */
	private List<ApplicationContextInitializer<?>> initializers;

    /**
     * ApplicationListener 数组
     */
	private List<ApplicationListener<?>> listeners;

    /**
     * 默认的属性集合
     */
	private Map<String, Object> defaultProperties;

    /**
     * 附加的 profiles 的数组
     */
	private Set<String> additionalProfiles = new HashSet<>();

	private boolean allowBeanDefinitionOverriding;

    /**
     * 是否自定义 Environment
     */
	private boolean isCustomEnvironment = false;

	/**
	 * Create a new {@link SpringApplication} instance. The application context will load
	 * beans from the specified primary sources (see {@link SpringApplication class-level}
	 * documentation for details. The instance can be customized before calling
	 * {@link #run(String...)}.
	 * @param primarySources the primary bean sources
	 * @see #run(Class, String[])
	 * @see #SpringApplication(ResourceLoader, Class...)
	 * @see #setSources(Set)
	 */
	public SpringApplication(Class<?>... primarySources) {
		this(null, primarySources);
	}

	/**
	 * Create a new {@link SpringApplication} instance. The application context will load
	 * beans from the specified primary sources (see {@link SpringApplication class-level}
	 * documentation for details. The instance can be customized before calling
	 * {@link #run(String...)}.
	 * @param resourceLoader the resource loader to use
	 * @param primarySources the primary bean sources
	 * @see #run(Class, String[])
	 * @see #setSources(Set)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		// resourceLoader 主要用来获取 Resource 及 ClassLoader。这里值为 null
		this.resourceLoader = resourceLoader; // resourceLoader一般是null

		// 断言主要加载资源类不能为 null，否则报错
		Assert.notNull(primarySources, "PrimarySources must not be null");

		// 主要的 Java Config 类的数组，一般是项目的启动类
		// primarySources是SpringApplication.run的参数，存放的是主配置类
		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));


		/*
			SpringApplication允许指定应用的类型，大体上包括Web应用和非Web应用。
			从 Spring Boot 2.0开始，Web应用又可分为Servlet Web和Reactive Web。
			】】】而在准备阶段，是通过检查当前ClassPath下某些Class是否存在，从而推导应用的类型
		 */
		// 通过 classpath ，判断 Web 应用类型。
		this.webApplicationType = WebApplicationType.deduceFromClasspath();

		/*
			ApplicationContextInitializer 接口的主要作用是在 ConfigurableApplicationContext#refresh() 方法调用之前做一些初始化工作。
		 */
		// 加载应用上下文初始化器 initializer
		// 初始化 initializers 属性
		setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));

		// 加载应用事件监听器 listener
		// 初始化 listeners 属性
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));


		/*
			调用 #deduceMainApplicationClass() 方法，获得是调用了哪个 #main(String[] args) 方法
			在文初的例子中，就是 MVCApplication 类。
			这个 mainApplicationClass 属性，没有什么逻辑上的用途，【主要就是用来打印下日志】，
			说明是通过这个类启动 Spring 应用的。
		 */
		// 推断引导类，也就是找到入口类
		// 初始化 mainApplicationClass 属性
		this.mainApplicationClass = deduceMainApplicationClass();
	}

	private Class<?> deduceMainApplicationClass() {
		try {
		    // 获得当前 StackTraceElement 数组
			StackTraceElement[] stackTrace = new RuntimeException().getStackTrace();
			// 判断哪个执行了 main 方法
			for (StackTraceElement stackTraceElement : stackTrace) {
				if ("main".equals(stackTraceElement.getMethodName())) {
					return Class.forName(stackTraceElement.getClassName());
				}
			}
		} catch (ClassNotFoundException ex) {
			// Swallow and continue
		}
		return null;
	}

	/**
	 * 可以看到，在运行阶段执行的操作比较多，虽然看起来杂乱无章，但其实还是有规律可循的。
	 * 比如，执行的 SpringApplicationRunListeners 中的阶段方法，刚启动阶段的 starting 、已启动阶段的 started 、启动完成阶段的 running 等。
	 * 还有对应的 Spring 应用上下文的创建、准备、启动操作等。
	 */
	public ConfigurableApplicationContext run(String... args) {
	    // <1> 创建 StopWatch 对象，并启动。
		// 【StopWatch 主要用于简单统计 run 启动过程的时长】。
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		//
		ConfigurableApplicationContext context = null;
		// <7> 获得异常报告器 SpringBootExceptionReporter 数组
		Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();

		// <2> 配置 headless 属性
		// 这个逻辑，可以无视，和 AWT 相关。
		configureHeadlessProperty();

		/*
			该对象属于组合模式的实现，核心是内部关联的 SpringApplicationRunListener 集合
			【SpringApplicationRunListener 是 Spring Boot 的运行时监听器】
			目前，SpringApplicationRunListener 的实现类，只有 EventPublishingRunListener 类。
		 */
		// <3> 获得 SpringApplicationRunListener 的数组，并启动监听
		// 从getSpringFactoriesInstances中获取
		SpringApplicationRunListeners listeners = getRunListeners(args);
		listeners.starting();
		try {

			/*
				该类是用于简化 Spring Boot 应用启动参数的封装接口，
				】】】】我们启动项目时输入的命令参数会封装在该类中。
					一种是通过 IDEA 输入的参数
					另一种是 springboot jar包运行时传递的参数：cmd中运行java -jar xxx.jar name=张三 pwa=123
			 */
		    // 创建  ApplicationArguments 对象
			// 】】】用来获取 SpringApplication.run(args)传入的参数。【注意仅仅是参数】
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);


			/*
				接着进入构造 ConfigurableEnvironment 的阶段，
				该类是用来处理我们外部化配置的，如 properties、YAML 等，提供对配置文件的基础操作。
				当然，它能处理的外部配置可不仅仅如此
			 */
            // <4> 加载属性配置。
			// 获取 properties 配置文件
			// 】】】执行完成后，所有的 environment 的属性都会加载进来，包括 application.properties 和外部的属性配置。
			ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
			configureIgnoreBeanInfo(environment);

			// 5> 打印 Spring Banner
			Banner printedBanner = printBanner(environment);

            // <6> 】】】创建 Spring 容器。
			// 根据 webApplicationType 类型，获得 ApplicationContext 类型
			// web类型是 AnnotationConfigServletWebServerApplicationContext
			context = createApplicationContext();

			/*
				从 getSpringFactoriesInstances获取
				进行获得 SpringBootExceptionReporter 类型的对象数组。
				SpringBootExceptionReporter ，记录启动过程中的异常信息。
				在下面异常捕获catch中有用到
			 */
			// 】】】创建应用上下文
			// <7> 获得异常报告器 SpringBootExceptionReporter 数组
			exceptionReporters = getSpringFactoriesInstances(
					SpringBootExceptionReporter.class,
					new Class[] { ConfigurableApplicationContext.class }, context);


			/*
				主要是给 context 的一些属性属性做赋值（初始化它的一些属性），比如：environment、resouceLoader
				遍历 ApplicationContextInitializer 数组，逐个调用 ApplicationContextInitializer#initialize(context) 方法，进行初始化。
				设置 beanFactory 的属性
				加载 BeanDefinition 们
			 */
			// 】】】准备应用上下文
			// 创建完 Spring 应用上下文之后，执行 prepareContext 方法进入准备上下文阶段
			// <8> 主要是调用所有初始化类的 initialize 方法
			// 准备 ApplicationContext 对象，主要是初始化它的一些属性
			prepareContext(context, environment, listeners, applicationArguments,
					printedBanner);

			// 】】】初始化应用上下文
			// 】】】接下来就是真正启动阶段，执行的是 refreshContext 方法：
			// <9> 初始化 Spring 容器。
			// 启动（刷新） Spring 容器
			refreshContext(context);

			// <10> 执行 Spring 容器的初始化的后置逻辑。
			//  【默认实现为空】。
			afterRefresh(context, applicationArguments);

			// <11> 停止 StopWatch 统计时长
			stopWatch.stop();
			// <12> 打印 Spring Boot 启动的时长日志。
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
			}

			// <13> 通知 SpringApplicationRunListener 的数组，【Spring 容器启动完成。】
			listeners.started(context);

			/*
				<1> 处，获得所有 Runner 们，并进行排序。
				<2> 处，遍历 Runner 数组，执行逻辑。
			 */
			// <14> 调用 ApplicationRunner 或者 CommandLineRunner 的运行方法。
			// 】】】项目启动后，做的一些操作，开发人员可自行扩展
			callRunners(context, applicationArguments);
		} catch (Throwable ex) {
			/*
				如果发生异常，则调用 #handleRunFailure(...) 方法，交给 SpringBootExceptionReporter 进行处理，
				并抛出 IllegalStateException 异常。
			 */
		    // <14.1> 如果发生异常，则进行处理，并抛出 IllegalStateException 异常
			handleRunFailure(context, ex, exceptionReporters, listeners);
			throw new IllegalStateException(ex);
		}

        // <15> 通知 SpringApplicationRunListener 的数组，Spring 容器运行中。
		try {
			listeners.running(context);
		} catch (Throwable ex) {
			/*
				如果发生异常，则调用 #handleRunFailure(...) 方法，交给 SpringBootExceptionReporter 进行处理，
				并抛出 IllegalStateException 异常。
			 */
            // <15.1> 如果发生异常，则进行处理，并抛出 IllegalStateException 异常
            handleRunFailure(context, ex, exceptionReporters, null);
			throw new IllegalStateException(ex);
		}
		return context;
	}

	private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments) {

		/*
			根据 webApplicationType 类型，会创建不同类型的 ConfigurableEnvironment 对象。
			web创建 StandardServletEnvironment
		 */
        // <1> 创建 ConfigurableEnvironment 对象，并进行配置
		// 有获取，没有创建
		ConfigurableEnvironment environment = getOrCreateEnvironment();
		// 】】】配置 environment 变量
		configureEnvironment(environment, applicationArguments.getSourceArgs());


        // <2> 通知 SpringApplicationRunListener 的数组，【环境变量已经准备完成。】
		listeners.environmentPrepared(environment);
		// <3> 绑定 environment 到 SpringApplication 上
		// 暂时不太知道用途。
		bindToSpringApplication(environment);

		/*
			默认情况下，isCustomEnvironment 为 false ，所以会执行这块逻辑。
			但是，一般情况下，返回的还是 environment 自身，
			【所以可以无视这块逻辑先。】
		 */
		// <4> 如果非自定义 environment ，则根据条件转换
		if (!this.isCustomEnvironment) {
			environment = new EnvironmentConverter(getClassLoader()).convertEnvironmentIfNecessary(environment, deduceEnvironmentClass());
		}

		/*
			调用 ConfigurationPropertySources#attach(Environment environment) 静态方法，
			如果有 attach 到 environment上的 MutablePropertySources ，则添加到 environment 的 PropertySource 中。
			【这块逻辑，也可以先无视】。
		 */
		// <5> 如果有 attach 到 environment 上的 MutablePropertySources ，则添加到 environment 的 PropertySource 中。
		ConfigurationPropertySources.attach(environment);
		return environment;
	}

	private Class<? extends StandardEnvironment> deduceEnvironmentClass() {
		switch (this.webApplicationType) {
		case SERVLET:
			return StandardServletEnvironment.class;
		case REACTIVE:
			return StandardReactiveWebEnvironment.class;
		default:
			return StandardEnvironment.class;
		}
	}

	/*
		我们来看看主要做了哪些操作：

			1、设置了 Spring 应用上下文的 ApplicationArguments，上面说过是处理外部化配置的，
			具体类型为 StandardServletEnvironment 。

			2、Spring 应用上下文后置处理，主要是覆盖当前 Spring 应用上下文默认所关联的 ResourceLoader 和 ClassLoader 。

			3、执行 Spring 的初始化器，上篇文章说过在 Spring Boot 准备阶段初始化了一
			批在 spring.factories 文件中定义好的 ApplicationContextInitializer ，这里就是执行它们的 initialize 方法，
			同时这里也是一个扩展点，后面详细讨论。

			4、执行 SpringApplicationRunListeners 的 contextPrepared 阶段方法，表示 ApplicationContext 准备完成，
			同时向 Spring Boot 监听器发布 ApplicationContextInitializedEvent 事件 。

			5、将 springApplicationArguments 和 springBootBanner 注册为 Bean。

			6、加载 Spring 应用上下文的配置源，也是在上篇文章 Spring Boot 准备阶段获取的
			 primarySources 和 sources ，primarySources 来源于 SpringApplication 构造器参数，
			 sources 则来源于自定义配置的 setSources 方法。

			7、最后执行 SpringApplicationRunListeners 的 contextLoaded 阶段方法，
			表示 ApplicationContext 完成加载但还未启动，同时向 Spring Boot 监听器发布 ApplicationPreparedEvent 事件 。
	 */
	/*
		主要是给 context 的属性做赋值，以及 ApplicationContextInitializer 的初始化。
	 */
	private void prepareContext(ConfigurableApplicationContext context,
			ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments, Banner printedBanner) {
		// <1> 】】】设置 context 的 environment 属性
	    context.setEnvironment(environment);
	    // <2> 设置 context 的一些属性。比如设置ResourceLoader
		postProcessApplicationContext(context);

		/*
			遍历 ApplicationContextInitializer 数组，逐个调用 ApplicationContextInitializer#initialize(context) 方法，
			进行初始化。
		 */
		// 】】】<3> 初始化 ApplicationContextInitializer
		applyInitializers(context);
        // <4> 通知 SpringApplicationRunListener 的数组，【Spring 容器准备完成】。
		listeners.contextPrepared(context);
		// <5> 打印日志
		if (this.logStartupInfo) {
			logStartupInfo(context.getParent() == null);
			logStartupProfileInfo(context);
		}

		// Add boot specific singleton beans
        // <6> 设置 beanFactory 的属性
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
		if (printedBanner != null) {
			beanFactory.registerSingleton("springBootBanner", printedBanner);
		}
		if (beanFactory instanceof DefaultListableBeanFactory) {
			((DefaultListableBeanFactory) beanFactory).setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}

        // 】】】<7> 加载 BeanDefinition 们
		Set<Object> sources = getAllSources(); // 比如包括MVCApplication类
		Assert.notEmpty(sources, "Sources must not be empty");
		load(context, sources.toArray(new Object[0]));

        // <8> 通知 SpringApplicationRunListener 的数组，【Spring 容器加载完成】。
        listeners.contextLoaded(context);
	}

	/*
		// 是否注册 ShutdownHook 钩子
		private boolean registerShutdownHook = true;
	 */
	private void refreshContext(ConfigurableApplicationContext context) {

		/*
			底层调用的是 AbstractApplicationContext 的 refresh 方法，到这里 Spring 应用正式启动，
			】】】Spring Boot 核心特性也随之启动，【如自动装配】。
		 */
		/*
			】】】这里，可以触发 Spring Boot 的自动配置的功能
		 */
	    // 】】】<1> 开启（刷新）Spring 容器
		refresh(context);



		// <2> 【注册 ShutdownHook 钩子】
		// 这个钩子，主要用于 Spring 应用的关闭时，销毁相应的 Bean 们。
		if (this.registerShutdownHook) {
			try {
				context.registerShutdownHook();
			} catch (AccessControlException ex) {
				// Not allowed in some environments.
			}
		}
	}

	private void configureHeadlessProperty() {
		System.setProperty(SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, System.getProperty(
				SYSTEM_PROPERTY_JAVA_AWT_HEADLESS, Boolean.toString(this.headless)));
	}

	private SpringApplicationRunListeners getRunListeners(String[] args) {
		Class<?>[] types = new Class<?>[] { SpringApplication.class, String[].class };
		return new SpringApplicationRunListeners(logger, getSpringFactoriesInstances(
				SpringApplicationRunListener.class, types, this, args));
	}

    /**
     * 获得指定类类对应的对象们。
     *
     * @param type 指定类
     * @param <T> 泛型
     * @return 对象们
     */

	private <T> Collection<T> getSpringFactoriesInstances(Class<T> type) {
		return getSpringFactoriesInstances(type, new Class<?>[] {});
	}

	/*
		获得指定类类对应的对象们
	 */
	private <T> Collection<T> getSpringFactoriesInstances(Class<T> type,
			Class<?>[] parameterTypes, Object... args) {
		ClassLoader classLoader = getClassLoader();
		// Use names and ensure unique to protect against duplicates
        // 加载指定类型对应的，在 `META-INF/spring.factories` 里的类名的数组
		// 】】】在 META-INF/spring.factories 文件中，会以 KEY-VALUE 的格式，配置每个类对应的实现类们。
		Set<String> names = new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(type, classLoader));
		// 创建对象们
		List<T> instances = createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names);
		// 排序对象们
		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}

    /**
     * 创建对象的数组
     *
     * @param type 父类
     * @param parameterTypes 构造方法的参数类型
     * @param classLoader 类加载器
     * @param args 参数
     * @param names 类名的数组
     * @param <T> 泛型
     * @return 对象的数组
     */
	@SuppressWarnings("unchecked")
	private <T> List<T> createSpringFactoriesInstances(Class<T> type,
			Class<?>[] parameterTypes, ClassLoader classLoader, Object[] args,
			Set<String> names) {
		List<T> instances = new ArrayList<>(names.size()); // 数组大小，细节~
		// 遍历 names 数组
		for (String name : names) {
			try {
			    // 获得 name 对应的类
				Class<?> instanceClass = ClassUtils.forName(name, classLoader);
				// 判断类是否实现自 type 类
				Assert.isAssignable(type, instanceClass);
				// 获得构造方法
				Constructor<?> constructor = instanceClass.getDeclaredConstructor(parameterTypes);
				// 创建对象
				T instance = (T) BeanUtils.instantiateClass(constructor, args);
				instances.add(instance);
			} catch (Throwable ex) {
				throw new IllegalArgumentException("Cannot instantiate " + type + " : " + name, ex);
			}
		}
		return instances;
	}

    /**
     * @return 获得或创建 ConfigurableEnvironment 对象
     */
	private ConfigurableEnvironment getOrCreateEnvironment() {
	    // 已经存在，则进行返回
		if (this.environment != null) {
			return this.environment;
		}
		// 不存在，则根据 webApplicationType 类型，进行创建。
		switch (this.webApplicationType) {
            case SERVLET:
                return new StandardServletEnvironment();
            case REACTIVE:
                return new StandardReactiveWebEnvironment();
            default:
                return new StandardEnvironment();
		}
	}

	/**
	 * Template method delegating to
	 * {@link #configurePropertySources(ConfigurableEnvironment, String[])} and
	 * {@link #configureProfiles(ConfigurableEnvironment, String[])} in that order.
	 * Override this method for complete control over Environment customization, or one of
	 * the above for fine-grained control over property sources or profiles, respectively.
	 * @param environment this application's environment
	 * @param args arguments passed to the {@code run} method
	 * @see #configureProfiles(ConfigurableEnvironment, String[])
	 * @see #configurePropertySources(ConfigurableEnvironment, String[])
	 */
	protected void configureEnvironment(ConfigurableEnvironment environment, String[] args) {

		/*
			// 是否添加共享的 ConversionService
			private boolean addConversionService = true;
		 */
	    // <1.1> 设置 environment 的 conversionService 属性
		// 【可以暂时无视】。
		if (this.addConversionService) {
			ConversionService conversionService = ApplicationConversionService.getSharedInstance();
			environment.setConversionService((ConfigurableConversionService) conversionService);
		}

		// <1.2> 增加 environment 的 PropertySource 属性源
		// 代码上可以看出，可以根据配置的 defaultProperties、或者 JVM 启动参数，作为附加的 PropertySource 属性源。
		configurePropertySources(environment, args);
		// <1.3> 配置 environment 的 activeProfiles 属性
		configureProfiles(environment, args);
	}

	/**
	 * Add, remove or re-order any {@link PropertySource}s in this application's
	 * environment.
	 * @param environment this application's environment
	 * @param args arguments passed to the {@code run} method
	 * @see #configureEnvironment(ConfigurableEnvironment, String[])
	 */
	/*
		// 是否添加 JVM 启动参数
		private boolean addCommandLineProperties = true;
		// 默认的属性集合
		private Map<String, Object> defaultProperties;
	 */
	/*
		代码上可以看出，可以根据配置的 defaultProperties、或者 JVM 启动参数，作为附加的 PropertySource 属性源。
	 */
	protected void configurePropertySources(ConfigurableEnvironment environment, String[] args) {
		MutablePropertySources sources = environment.getPropertySources();
		// 配置的 defaultProperties
		if (this.defaultProperties != null && !this.defaultProperties.isEmpty()) {
			sources.addLast(new MapPropertySource("defaultProperties", this.defaultProperties));
		}
		// 来自启动参数的
		if (this.addCommandLineProperties && args.length > 0) {
			String name = CommandLinePropertySource.COMMAND_LINE_PROPERTY_SOURCE_NAME;
			if (sources.contains(name)) { // 已存在，就进行替换
				PropertySource<?> source = sources.get(name);
				CompositePropertySource composite = new CompositePropertySource(name);
				composite.addPropertySource(new SimpleCommandLinePropertySource(
						"springApplicationCommandLineArgs", args));
				composite.addPropertySource(source);
				sources.replace(name, composite);
			} else { // 不存在，就进行添加
				sources.addFirst(new SimpleCommandLinePropertySource(args));
			}
		}
	}

	/**
	 * Configure which profiles are active (or active by default) for this application
	 * environment. Additional profiles may be activated during configuration file
	 * processing via the {@code spring.profiles.active} property.
	 * @param environment this application's environment
	 * @param args arguments passed to the {@code run} method
	 * @see #configureEnvironment(ConfigurableEnvironment, String[])
	 * @see org.springframework.boot.context.config.ConfigFileApplicationListener
	 */
	/*
		// 附加的 profiles 的数组
		private Set<String> additionalProfiles = new HashSet<>();
	 */
	protected void configureProfiles(ConfigurableEnvironment environment, String[] args) {
		environment.getActiveProfiles(); // ensure they are initialized 保证已经被初始化
		// But these ones should go first (last wins in a property key clash)
		// 附加的 profiles 的数组
		Set<String> profiles = new LinkedHashSet<>(this.additionalProfiles);
		profiles.addAll(Arrays.asList(environment.getActiveProfiles()));
		// 设置 activeProfiles
		environment.setActiveProfiles(StringUtils.toStringArray(profiles));
	}

	private void configureIgnoreBeanInfo(ConfigurableEnvironment environment) {
		if (System.getProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME) == null) {
			Boolean ignore = environment.getProperty("spring.beaninfo.ignore", Boolean.class, Boolean.TRUE);
			System.setProperty(CachedIntrospectionResults.IGNORE_BEANINFO_PROPERTY_NAME, ignore.toString());
		}
	}

	/**
	 * Bind the environment to the {@link SpringApplication}.
	 * @param environment the environment to bind
	 */
	protected void bindToSpringApplication(ConfigurableEnvironment environment) {
		try {
			Binder.get(environment).bind("spring.main", Bindable.ofInstance(this));
		} catch (Exception ex) {
			throw new IllegalStateException("Cannot bind to SpringApplication", ex);
		}
	}

	private Banner printBanner(ConfigurableEnvironment environment) {
		if (this.bannerMode == Banner.Mode.OFF) {
			return null;
		}
		ResourceLoader resourceLoader = (this.resourceLoader != null)
				? this.resourceLoader : new DefaultResourceLoader(getClassLoader());
		SpringApplicationBannerPrinter bannerPrinter = new SpringApplicationBannerPrinter(
				resourceLoader, this.banner);
		if (this.bannerMode == Mode.LOG) {
			return bannerPrinter.print(environment, this.mainApplicationClass, logger);
		}
		return bannerPrinter.print(environment, this.mainApplicationClass, System.out);
	}

	/**
	 * Strategy method used to create the {@link ApplicationContext}. By default this
	 * method will respect any explicitly set application context or application context
	 * class before falling back to a suitable default.
	 * @return the application context (not yet refreshed)
	 * @see #setApplicationContextClass(Class)
	 */
	/*
		// The class name of application context that will be used by default for non-web environments.
		public static final String DEFAULT_CONTEXT_CLASS = "org.springframework.context."
				+ "annotation.AnnotationConfigApplicationContext";

		// The class name of application context that will be used by default for web environments.
		public static final String DEFAULT_SERVLET_WEB_CONTEXT_CLASS = "org.springframework.boot."
				+ "web.servlet.context.AnnotationConfigServletWebServerApplicationContext";

		// The class name of application context that will be used by default for reactive web environments.
		public static final String DEFAULT_REACTIVE_WEB_CONTEXT_CLASS = "org.springframework."
				+ "boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext";
	 */
	protected ConfigurableApplicationContext createApplicationContext() {
	    // 根据 webApplicationType 类型，获得 ApplicationContext 类型
		Class<?> contextClass = this.applicationContextClass;
		if (contextClass == null) {
			try {
				switch (this.webApplicationType) {
				case SERVLET:
					// AnnotationConfigServletWebServerApplicationContext
					contextClass = Class.forName(DEFAULT_SERVLET_WEB_CONTEXT_CLASS);
					break;
				case REACTIVE:
					contextClass = Class.forName(DEFAULT_REACTIVE_WEB_CONTEXT_CLASS);
					break;
				default:
					contextClass = Class.forName(DEFAULT_CONTEXT_CLASS);
				}
			} catch (ClassNotFoundException ex) {
				throw new IllegalStateException("Unable create a default ApplicationContext, " + "please specify an ApplicationContextClass", ex);
			}
		}
		// 创建 ApplicationContext 对象
		return (ConfigurableApplicationContext) BeanUtils.instantiateClass(contextClass);
	}

	/**
	 * Apply any relevant post processing the {@link ApplicationContext}. Subclasses can
	 * apply additional processing as required.
	 * @param context the application context
	 */
	protected void postProcessApplicationContext(ConfigurableApplicationContext context) {
		if (this.beanNameGenerator != null) {
			context.getBeanFactory().registerSingleton(AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR, this.beanNameGenerator);
		}
		// 设置ResourceLoader
		if (this.resourceLoader != null) {
			if (context instanceof GenericApplicationContext) {
				((GenericApplicationContext) context).setResourceLoader(this.resourceLoader);
			}
			if (context instanceof DefaultResourceLoader) {
				((DefaultResourceLoader) context).setClassLoader(this.resourceLoader.getClassLoader());
			}
		}
		if (this.addConversionService) {
			context.getBeanFactory().setConversionService(ApplicationConversionService.getSharedInstance());
		}
	}

	/**
	 * Apply any {@link ApplicationContextInitializer}s to the context before it is
	 * refreshed.
	 * @param context the configured ApplicationContext (not refreshed yet)
	 * @see ConfigurableApplicationContext#refresh()
	 */
	/*
		遍历 ApplicationContextInitializer 数组，逐个调用 ApplicationContextInitializer#initialize(context) 方法，进行初始化。
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void applyInitializers(ConfigurableApplicationContext context) {
	    // 遍历 ApplicationContextInitializer 数组
		for (ApplicationContextInitializer initializer : getInitializers()) {
		    // 校验 ApplicationContextInitializer 的泛型非空
			Class<?> requiredType = GenericTypeResolver.resolveTypeArgument(initializer.getClass(), ApplicationContextInitializer.class);
			Assert.isInstanceOf(requiredType, context, "Unable to call initializer.");
			// 初始化 ApplicationContextInitializer
			initializer.initialize(context);
		}
	}

	/**
	 * Called to log startup information, subclasses may override to add additional
	 * logging.
	 * @param isRoot true if this application is the root of a context hierarchy
	 */
	protected void logStartupInfo(boolean isRoot) {
		if (isRoot) {
			new StartupInfoLogger(this.mainApplicationClass).logStarting(getApplicationLog());
		}
	}

	/**
	 * Called to log active profile information.
	 * @param context the application context
	 */
	protected void logStartupProfileInfo(ConfigurableApplicationContext context) {
		Log log = getApplicationLog();
		if (log.isInfoEnabled()) {
			String[] activeProfiles = context.getEnvironment().getActiveProfiles();
			if (ObjectUtils.isEmpty(activeProfiles)) {
				String[] defaultProfiles = context.getEnvironment().getDefaultProfiles();
				log.info("No active profile set, falling back to default profiles: "
						+ StringUtils.arrayToCommaDelimitedString(defaultProfiles));
			} else {
				log.info("The following profiles are active: "
						+ StringUtils.arrayToCommaDelimitedString(activeProfiles));
			}
		}
	}

	/**
	 * Returns the {@link Log} for the application. By default will be deduced.
	 * @return the application log
	 */
	protected Log getApplicationLog() {
		if (this.mainApplicationClass == null) {
			return logger;
		}
		return LogFactory.getLog(this.mainApplicationClass);
	}

	/**
	 * Load beans into the application context.
	 * @param context the context to load beans into
	 * @param sources the sources to load
	 */
	protected void load(ApplicationContext context, Object[] sources) {
		if (logger.isDebugEnabled()) {
			logger.debug("Loading source " + StringUtils.arrayToCommaDelimitedString(sources));
		}

		/*
			调用 #getBeanDefinitionRegistry(ApplicationContext context) 方法，创获取 BeanDefinitionRegistry 对象。
		 */
		// <1>创建 BeanDefinitionLoader 对象
		BeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);

		// 设置 loader 的属性
		if (this.beanNameGenerator != null) {
			loader.setBeanNameGenerator(this.beanNameGenerator);
		}
		if (this.resourceLoader != null) {
			loader.setResourceLoader(this.resourceLoader);
		}
		if (this.environment != null) {
			loader.setEnvironment(this.environment);
		}

		// 】】】执行 BeanDefinition 加载
		loader.load();
	}

	/**
	 * The ResourceLoader that will be used in the ApplicationContext.
	 * @return the resourceLoader the resource loader that will be used in the
	 * ApplicationContext (or null if the default)
	 */
	public ResourceLoader getResourceLoader() {
		return this.resourceLoader;
	}

	/**
	 * Either the ClassLoader that will be used in the ApplicationContext (if
	 * {@link #setResourceLoader(ResourceLoader) resourceLoader} is set, or the context
	 * class loader (if not null), or the loader of the Spring {@link ClassUtils} class.
	 * @return a ClassLoader (never null)
	 */
	public ClassLoader getClassLoader() {
		if (this.resourceLoader != null) {
			return this.resourceLoader.getClassLoader();
		}
		return ClassUtils.getDefaultClassLoader();
	}

	/**
	 * Get the bean definition registry.
	 * @param context the application context
	 * @return the BeanDefinitionRegistry if it can be determined
	 */
	private BeanDefinitionRegistry getBeanDefinitionRegistry(ApplicationContext context) {
		if (context instanceof BeanDefinitionRegistry) {
			return (BeanDefinitionRegistry) context;
		}
		if (context instanceof AbstractApplicationContext) {
			return (BeanDefinitionRegistry) ((AbstractApplicationContext) context)
					.getBeanFactory();
		}
		throw new IllegalStateException("Could not locate BeanDefinitionRegistry");
	}

	/**
	 * Factory method used to create the {@link BeanDefinitionLoader}.
	 * @param registry the bean definition registry
	 * @param sources the sources to load
	 * @return the {@link BeanDefinitionLoader} that will be used to load beans
	 */
	protected BeanDefinitionLoader createBeanDefinitionLoader(BeanDefinitionRegistry registry, Object[] sources) {
		return new BeanDefinitionLoader(registry, sources);
	}

	/**
	 * Refresh the underlying {@link ApplicationContext}.
	 * @param applicationContext the application context to refresh
	 */
	protected void refresh(ApplicationContext applicationContext) {
	    // 断言，判断 applicationContext 是 AbstractApplicationContext 的子类
		Assert.isInstanceOf(AbstractApplicationContext.class, applicationContext);
		// 启动（刷新） AbstractApplicationContext
		((AbstractApplicationContext) applicationContext).refresh();
	}

	/**
	 * Called after the context has been refreshed.
	 * @param context the application context
	 * @param args the application arguments
	 */
	protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {
	}

	private void callRunners(ApplicationContext context, ApplicationArguments args) {

		/*
			<1> 处，获得所有 Runner 们，并进行排序。
		 */
	    // <1> 获得所有 Runner 们。从ioc中获取
		List<Object> runners = new ArrayList<>();
		// <1.1> 获得所有 ApplicationRunner Bean 们
		runners.addAll(context.getBeansOfType(ApplicationRunner.class).values());
		// <1.2> 获得所有 CommandLineRunner Bean 们
		runners.addAll(context.getBeansOfType(CommandLineRunner.class).values());
		// <1.3> 排序 runners
		AnnotationAwareOrderComparator.sort(runners);

		// <2> 遍历 Runner 数组，执行逻辑
		for (Object runner : new LinkedHashSet<>(runners)) {
			if (runner instanceof ApplicationRunner) {
				callRunner((ApplicationRunner) runner, args);
			}
			if (runner instanceof CommandLineRunner) {
				callRunner((CommandLineRunner) runner, args);
			}
		}
	}

	private void callRunner(ApplicationRunner runner, ApplicationArguments args) {
		try {
			(runner).run(args);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to execute ApplicationRunner", ex);
		}
	}

	private void callRunner(CommandLineRunner runner, ApplicationArguments args) {
		try {
			(runner).run(args.getSourceArgs());
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to execute CommandLineRunner", ex);
		}
	}

	// TODO
	private void handleRunFailure(ConfigurableApplicationContext context,
			Throwable exception,
			Collection<SpringBootExceptionReporter> exceptionReporters,
			SpringApplicationRunListeners listeners) {
		try {
			try {
				handleExitCode(context, exception);
				if (listeners != null) {
					listeners.failed(context, exception);
				}
			}
			finally {
				reportFailure(exceptionReporters, exception);
				if (context != null) {
					context.close();
				}
			}
		}
		catch (Exception ex) {
			logger.warn("Unable to close ApplicationContext", ex);
		}
		ReflectionUtils.rethrowRuntimeException(exception);
	}

	private void reportFailure(Collection<SpringBootExceptionReporter> exceptionReporters,
			Throwable failure) {
		try {
			for (SpringBootExceptionReporter reporter : exceptionReporters) {
				if (reporter.reportException(failure)) {
					registerLoggedException(failure);
					return;
				}
			}
		}
		catch (Throwable ex) {
			// Continue with normal handling of the original failure
		}
		if (logger.isErrorEnabled()) {
			logger.error("Application run failed", failure);
			registerLoggedException(failure);
		}
	}

	/**
	 * Register that the given exception has been logged. By default, if the running in
	 * the main thread, this method will suppress additional printing of the stacktrace.
	 * @param exception the exception that was logged
	 */
	protected void registerLoggedException(Throwable exception) {
		SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
		if (handler != null) {
			handler.registerLoggedException(exception);
		}
	}

	private void handleExitCode(ConfigurableApplicationContext context,
			Throwable exception) {
		int exitCode = getExitCodeFromException(context, exception);
		if (exitCode != 0) {
			if (context != null) {
				context.publishEvent(new ExitCodeEvent(context, exitCode));
			}
			SpringBootExceptionHandler handler = getSpringBootExceptionHandler();
			if (handler != null) {
				handler.registerExitCode(exitCode);
			}
		}
	}

	private int getExitCodeFromException(ConfigurableApplicationContext context,
			Throwable exception) {
		int exitCode = getExitCodeFromMappedException(context, exception);
		if (exitCode == 0) {
			exitCode = getExitCodeFromExitCodeGeneratorException(exception);
		}
		return exitCode;
	}

	private int getExitCodeFromMappedException(ConfigurableApplicationContext context,
			Throwable exception) {
		if (context == null || !context.isActive()) {
			return 0;
		}
		ExitCodeGenerators generators = new ExitCodeGenerators();
		Collection<ExitCodeExceptionMapper> beans = context
				.getBeansOfType(ExitCodeExceptionMapper.class).values();
		generators.addAll(exception, beans);
		return generators.getExitCode();
	}

	private int getExitCodeFromExitCodeGeneratorException(Throwable exception) {
		if (exception == null) {
			return 0;
		}
		if (exception instanceof ExitCodeGenerator) {
			return ((ExitCodeGenerator) exception).getExitCode();
		}
		return getExitCodeFromExitCodeGeneratorException(exception.getCause());
	}

	SpringBootExceptionHandler getSpringBootExceptionHandler() {
		if (isMainThread(Thread.currentThread())) {
			return SpringBootExceptionHandler.forCurrentThread();
		}
		return null;
	}

	private boolean isMainThread(Thread currentThread) {
		return ("main".equals(currentThread.getName())
				|| "restartedMain".equals(currentThread.getName()))
				&& "main".equals(currentThread.getThreadGroup().getName());
	}

	/**
	 * Returns the main application class that has been deduced or explicitly configured.
	 * @return the main application class or {@code null}
	 */
	public Class<?> getMainApplicationClass() {
		return this.mainApplicationClass;
	}

	/**
	 * Set a specific main application class that will be used as a log source and to
	 * obtain version information. By default the main application class will be deduced.
	 * Can be set to {@code null} if there is no explicit application class.
	 * @param mainApplicationClass the mainApplicationClass to set or {@code null}
	 */
	public void setMainApplicationClass(Class<?> mainApplicationClass) {
		this.mainApplicationClass = mainApplicationClass;
	}

	/**
	 * Returns the type of web application that is being run.
	 * @return the type of web application
	 * @since 2.0.0
	 */
	public WebApplicationType getWebApplicationType() {
		return this.webApplicationType;
	}

	/**
	 * Sets the type of web application to be run. If not explicitly set the type of web
	 * application will be deduced based on the classpath.
	 * @param webApplicationType the web application type
	 * @since 2.0.0
	 */
	public void setWebApplicationType(WebApplicationType webApplicationType) {
		Assert.notNull(webApplicationType, "WebApplicationType must not be null");
		this.webApplicationType = webApplicationType;
	}

	/**
	 * Sets if bean definition overriding, by registering a definition with the same name
	 * as an existing definition, should be allowed. Defaults to {@code false}.
	 * @param allowBeanDefinitionOverriding if overriding is allowed
	 * @since 2.1
	 * @see DefaultListableBeanFactory#setAllowBeanDefinitionOverriding(boolean)
	 */
	public void setAllowBeanDefinitionOverriding(boolean allowBeanDefinitionOverriding) {
		this.allowBeanDefinitionOverriding = allowBeanDefinitionOverriding;
	}

	/**
	 * Sets if the application is headless and should not instantiate AWT. Defaults to
	 * {@code true} to prevent java icons appearing.
	 * @param headless if the application is headless
	 */
	public void setHeadless(boolean headless) {
		this.headless = headless;
	}

	/**
	 * Sets if the created {@link ApplicationContext} should have a shutdown hook
	 * registered. Defaults to {@code true} to ensure that JVM shutdowns are handled
	 * gracefully.
	 * @param registerShutdownHook if the shutdown hook should be registered
	 */
	public void setRegisterShutdownHook(boolean registerShutdownHook) {
		this.registerShutdownHook = registerShutdownHook;
	}

	/**
	 * Sets the {@link Banner} instance which will be used to print the banner when no
	 * static banner file is provided.
	 * @param banner the Banner instance to use
	 */
	public void setBanner(Banner banner) {
		this.banner = banner;
	}

	/**
	 * Sets the mode used to display the banner when the application runs. Defaults to
	 * {@code Banner.Mode.CONSOLE}.
	 * @param bannerMode the mode used to display the banner
	 */
	public void setBannerMode(Banner.Mode bannerMode) {
		this.bannerMode = bannerMode;
	}

	/**
	 * Sets if the application information should be logged when the application starts.
	 * Defaults to {@code true}.
	 * @param logStartupInfo if startup info should be logged.
	 */
	public void setLogStartupInfo(boolean logStartupInfo) {
		this.logStartupInfo = logStartupInfo;
	}

	/**
	 * Sets if a {@link CommandLinePropertySource} should be added to the application
	 * context in order to expose arguments. Defaults to {@code true}.
	 * @param addCommandLineProperties if command line arguments should be exposed
	 */
	public void setAddCommandLineProperties(boolean addCommandLineProperties) {
		this.addCommandLineProperties = addCommandLineProperties;
	}

	/**
	 * Sets if the {@link ApplicationConversionService} should be added to the application
	 * context's {@link Environment}.
	 * @param addConversionService if the application conversion service should be added
	 * @since 2.1.0
	 */
	public void setAddConversionService(boolean addConversionService) {
		this.addConversionService = addConversionService;
	}

	/**
	 * Set default environment properties which will be used in addition to those in the
	 * existing {@link Environment}.
	 * @param defaultProperties the additional properties to set
	 */
	public void setDefaultProperties(Map<String, Object> defaultProperties) {
		this.defaultProperties = defaultProperties;
	}

	/**
	 * Convenient alternative to {@link #setDefaultProperties(Map)}.
	 * @param defaultProperties some {@link Properties}
	 */
	public void setDefaultProperties(Properties defaultProperties) {
		this.defaultProperties = new HashMap<>();
		for (Object key : Collections.list(defaultProperties.propertyNames())) {
			this.defaultProperties.put((String) key, defaultProperties.get(key));
		}
	}

	/**
	 * Set additional profile values to use (on top of those set in system or command line
	 * properties).
	 * @param profiles the additional profiles to set
	 */
	public void setAdditionalProfiles(String... profiles) {
		this.additionalProfiles = new LinkedHashSet<>(Arrays.asList(profiles));
	}

	/**
	 * Sets the bean name generator that should be used when generating bean names.
	 * @param beanNameGenerator the bean name generator
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = beanNameGenerator;
	}

	/**
	 * Sets the underlying environment that should be used with the created application
	 * context.
	 * @param environment the environment
	 */
	public void setEnvironment(ConfigurableEnvironment environment) {
		this.isCustomEnvironment = true;
		this.environment = environment;
	}

	/**
	 * Add additional items to the primary sources that will be added to an
	 * ApplicationContext when {@link #run(String...)} is called.
	 * <p>
	 * The sources here are added to those that were set in the constructor. Most users
	 * should consider using {@link #getSources()}/{@link #setSources(Set)} rather than
	 * calling this method.
	 * @param additionalPrimarySources the additional primary sources to add
	 * @see #SpringApplication(Class...)
	 * @see #getSources()
	 * @see #setSources(Set)
	 * @see #getAllSources()
	 */
	public void addPrimarySources(Collection<Class<?>> additionalPrimarySources) {
		this.primarySources.addAll(additionalPrimarySources);
	}

	/**
	 * Returns a mutable set of the sources that will be added to an ApplicationContext
	 * when {@link #run(String...)} is called.
	 * <p>
	 * Sources set here will be used in addition to any primary sources set in the
	 * constructor.
	 * @return the application sources.
	 * @see #SpringApplication(Class...)
	 * @see #getAllSources()
	 */
	public Set<String> getSources() {
		return this.sources;
	}

	/**
	 * Set additional sources that will be used to create an ApplicationContext. A source
	 * can be: a class name, package name, or an XML resource location.
	 * <p>
	 * Sources set here will be used in addition to any primary sources set in the
	 * constructor.
	 * @param sources the application sources to set
	 * @see #SpringApplication(Class...)
	 * @see #getAllSources()
	 */
	public void setSources(Set<String> sources) {
		Assert.notNull(sources, "Sources must not be null");
		this.sources = new LinkedHashSet<>(sources);
	}

	/**
	 * Return an immutable set of all the sources that will be added to an
	 * ApplicationContext when {@link #run(String...)} is called. This method combines any
	 * primary sources specified in the constructor with any additional ones that have
	 * been {@link #setSources(Set) explicitly set}.
	 * @return an immutable set of all sources
	 */
	public Set<Object> getAllSources() {
		Set<Object> allSources = new LinkedHashSet<>();
		if (!CollectionUtils.isEmpty(this.primarySources)) {
			allSources.addAll(this.primarySources);
		}
		if (!CollectionUtils.isEmpty(this.sources)) {
			allSources.addAll(this.sources);
		}
		return Collections.unmodifiableSet(allSources);
	}

	/**
	 * Sets the {@link ResourceLoader} that should be used when loading resources.
	 * @param resourceLoader the resource loader
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		this.resourceLoader = resourceLoader;
	}

	/**
	 * Sets the type of Spring {@link ApplicationContext} that will be created. If not
	 * specified defaults to {@link #DEFAULT_SERVLET_WEB_CONTEXT_CLASS} for web based
	 * applications or {@link AnnotationConfigApplicationContext} for non web based
	 * applications.
	 * @param applicationContextClass the context class to set
	 */
	public void setApplicationContextClass(
			Class<? extends ConfigurableApplicationContext> applicationContextClass) {
		this.applicationContextClass = applicationContextClass;
		this.webApplicationType = WebApplicationType
				.deduceFromApplicationContext(applicationContextClass);
	}

	/**
	 * Sets the {@link ApplicationContextInitializer} that will be applied to the Spring
	 * {@link ApplicationContext}.
	 * @param initializers the initializers to set
	 */
	public void setInitializers(Collection<? extends ApplicationContextInitializer<?>> initializers) {
		this.initializers = new ArrayList<>();
		this.initializers.addAll(initializers);
	}

	/**
	 * Add {@link ApplicationContextInitializer}s to be applied to the Spring
	 * {@link ApplicationContext}.
	 * @param initializers the initializers to add
	 */
	public void addInitializers(ApplicationContextInitializer<?>... initializers) {
		this.initializers.addAll(Arrays.asList(initializers));
	}

	/**
	 * Returns read-only ordered Set of the {@link ApplicationContextInitializer}s that
	 * will be applied to the Spring {@link ApplicationContext}.
	 * @return the initializers
	 */
	public Set<ApplicationContextInitializer<?>> getInitializers() {
		return asUnmodifiableOrderedSet(this.initializers);
	}

	/**
	 * Sets the {@link ApplicationListener}s that will be applied to the SpringApplication
	 * and registered with the {@link ApplicationContext}.
	 * @param listeners the listeners to set
	 */
	public void setListeners(Collection<? extends ApplicationListener<?>> listeners) {
		this.listeners = new ArrayList<>();
		this.listeners.addAll(listeners);
	}

	/**
	 * Add {@link ApplicationListener}s to be applied to the SpringApplication and
	 * registered with the {@link ApplicationContext}.
	 * @param listeners the listeners to add
	 */
	public void addListeners(ApplicationListener<?>... listeners) {
		this.listeners.addAll(Arrays.asList(listeners));
	}

	/**
	 * Returns read-only ordered Set of the {@link ApplicationListener}s that will be
	 * applied to the SpringApplication and registered with the {@link ApplicationContext}
	 * .
	 * @return the listeners
	 */
	public Set<ApplicationListener<?>> getListeners() {
		return asUnmodifiableOrderedSet(this.listeners);
	}

	/**
     * 运行 Spring 应用
     *
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified source using default settings.
	 * @param primarySource the primary source to load 加载的主类
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ConfigurableApplicationContext run(Class<?> primarySource, String... args) {
		return run(new Class<?>[] { primarySource }, args);
	}

	/**
     * 运行 Spring 应用
     *
	 * Static helper that can be used to run a {@link SpringApplication} from the
	 * specified sources using default settings and user supplied arguments.
	 * @param primarySources the primary sources to load 加载的主类的数组
	 * @param args the application arguments (usually passed from a Java main method)
	 * @return the running {@link ApplicationContext}
	 */
	public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
	    // 创建 SpringApplication 对象，并执行运行。
		return new SpringApplication(primarySources).run(args);
	}

	/**
	 * A basic main that can be used to launch an application. This method is useful when
	 * application sources are defined via a {@literal --spring.main.sources} command line
	 * argument.
	 * <p>
	 * Most developers will want to define their own main method and call the
	 * {@link #run(Class, String...) run} method instead.
	 * @param args command line arguments
	 * @throws Exception if the application cannot be started
	 * @see SpringApplication#run(Class[], String[])
	 * @see SpringApplication#run(Class, String...)
	 */
	public static void main(String[] args) throws Exception {
		SpringApplication.run(new Class<?>[0], args);
	}

	/**
	 * Static helper that can be used to exit a {@link SpringApplication} and obtain a
	 * code indicating success (0) or otherwise. Does not throw exceptions but should
	 * print stack traces of any encountered. Applies the specified
	 * {@link ExitCodeGenerator} in addition to any Spring beans that implement
	 * {@link ExitCodeGenerator}. In the case of multiple exit codes the highest value
	 * will be used (or if all values are negative, the lowest value will be used)
	 * @param context the context to close if possible
	 * @param exitCodeGenerators exist code generators
	 * @return the outcome (0 if successful)
	 */
	public static int exit(ApplicationContext context,
			ExitCodeGenerator... exitCodeGenerators) {
		Assert.notNull(context, "Context must not be null");
		int exitCode = 0;
		try {
			try {
				ExitCodeGenerators generators = new ExitCodeGenerators();
				Collection<ExitCodeGenerator> beans = context
						.getBeansOfType(ExitCodeGenerator.class).values();
				generators.addAll(exitCodeGenerators);
				generators.addAll(beans);
				exitCode = generators.getExitCode();
				if (exitCode != 0) {
					context.publishEvent(new ExitCodeEvent(context, exitCode));
				}
			}
			finally {
				close(context);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			exitCode = (exitCode != 0) ? exitCode : 1;
		}
		return exitCode;
	}

	private static void close(ApplicationContext context) {
		if (context instanceof ConfigurableApplicationContext) {
			ConfigurableApplicationContext closable = (ConfigurableApplicationContext) context;
			closable.close();
		}
	}

	private static <E> Set<E> asUnmodifiableOrderedSet(Collection<E> elements) {
		List<E> list = new ArrayList<>(elements);
		list.sort(AnnotationAwareOrderComparator.INSTANCE);
		return new LinkedHashSet<>(list);
	}

}
