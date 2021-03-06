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

package org.springframework.boot.autoconfigure;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.annotation.DeterminableImports;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * Class for storing auto-configuration packages for reference later (e.g. by JPA entity
 * scanner).
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Oliver Gierke
 * 自动配置所在的包名
 */
/*
	简单来说，就是将使用 @AutoConfigurationPackage 注解的类所在的包（package），注册成一个 Spring IoC 容器中的 Bean 。
	酱紫，后续有其它模块需要使用，就可以通过获得该 Bean ，从而获得所在的包。例如说，JPA 模块，需要使用到。
 */
public abstract class AutoConfigurationPackages {

	private static final Log logger = LogFactory.getLog(AutoConfigurationPackages.class);

	private static final String BEAN = AutoConfigurationPackages.class.getName();

	/**
	 * Determine if the auto-configuration base packages for the given bean factory are
	 * available.
	 * @param beanFactory the source bean factory
	 * @return true if there are auto-config packages available
	 * 判断是否存在该 BEAN 在传入的容器中
	 */
	public static boolean has(BeanFactory beanFactory) {
		return beanFactory.containsBean(BEAN) && !get(beanFactory).isEmpty();
	}

	/**
	 * Return the auto-configuration base packages for the given bean factory.
	 * @param beanFactory the source bean factory
	 * @return a list of auto-configuration packages
	 * @throws IllegalStateException if auto-configuration is not enabled
	 * 获得 BEAN 。
	 */
	public static List<String> get(BeanFactory beanFactory) {
		try {
			return beanFactory.getBean(BEAN, BasePackages.class).get();
		} catch (NoSuchBeanDefinitionException ex) {
			throw new IllegalStateException("Unable to retrieve @EnableAutoConfiguration base packages");
		}
	}

	/**
	 * Programmatically registers the auto-configuration package names. Subsequent
	 * invocations will add the given package names to those that have already been
	 * registered. You can use this method to manually define the base packages that will
	 * be used for a given {@link BeanDefinitionRegistry}. Generally it's recommended that
	 * you don't call this method directly, but instead rely on the default convention
	 * where the package name is set from your {@code @EnableAutoConfiguration}
	 * configuration class or classes.
	 * @param registry the bean definition registry
	 * @param packageNames the package names to set
	 *                     注册一个用于存储报名（package）的 Bean 到 Spring IoC 容器中。
	 *  将这个包名packageNames保存至 BasePackages 类中，然后通过 BeanDefinitionRegistry 将其注册
	 */
	public static void register(BeanDefinitionRegistry registry, String... packageNames) {
	    // <1> 如果已经存在该 BEAN ，则修改其包（package）属性
		// 而合并 package 的逻辑，通过 #addBasePackages(ConstructorArgumentValues constructorArguments, String[] packageNames) 方法，进行实现。
		if (registry.containsBeanDefinition(BEAN)) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(BEAN);
			ConstructorArgumentValues constructorArguments = beanDefinition.getConstructorArgumentValues();
			constructorArguments.addIndexedArgumentValue(0, addBasePackages(constructorArguments, packageNames));
        // <2> 如果不存在该 BEAN ，则创建一个 Bean ，并进行注册
        } else { GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
			beanDefinition.setBeanClass(BasePackages.class);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, packageNames);
			beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
			registry.registerBeanDefinition(BEAN, beanDefinition);
		}
	}

	private static String[] addBasePackages(ConstructorArgumentValues constructorArguments, String[] packageNames) {
		// 获得已存在的
	    String[] existing = (String[]) constructorArguments.getIndexedArgumentValue(0, String[].class).getValue();
		// 进行合并
	    Set<String> merged = new LinkedHashSet<>();
		merged.addAll(Arrays.asList(existing));
		merged.addAll(Arrays.asList(packageNames));
		return StringUtils.toStringArray(merged);
	}

	/**
	 * {@link ImportBeanDefinitionRegistrar} to store the base package from the importing
	 * configuration.
	 * 是 AutoConfigurationPackages 的内部类，实现 ImportBeanDefinitionRegistrar、DeterminableImports 接口，
	 * 注册器，用于处理 @AutoConfigurationPackage 注解。
	 */
	static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {

		@Override
		public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {

			/*
				调用 #register(BeanDefinitionRegistry registry, String... packageNames) 方法，
				】】】注册一个用于存储报名（package）的 Bean 到 Spring IoC 容器中
			 */
			/*
				 】】】new PackageImport(metadata).getPackageName()	获取启动类所在的包名。
				 这里主要是通过 metadata 元数据信息构造 PackageImport 类。
				  先获取启动类的类名，再通过 ClassUtils.getPackageName 获取启动类所在的包名。
			 */
			register(registry, new PackageImport(metadata).getPackageName());
			/*
				最后就是将这个包名保存至 BasePackages 类中，然后通过 BeanDefinitionRegistry 将其注册，进行后续处理，至此该流程结束。
			 */
		}

		@Override
		public Set<Object> determineImports(AnnotationMetadata metadata) {
			return Collections.singleton(new PackageImport(metadata));
		}

	}

	/**
	 * Wrapper for a package import.
	 * PackageImport 是 AutoConfigurationPackages 的内部类，用于获得包名
	 */
	private static final class PackageImport {

        /**
         * 包名
         */
		private final String packageName;

		/*
			这里主要是通过 metadata 元数据信息构造 PackageImport 类。先获取启动类的类名，
			再通过 ClassUtils.getPackageName 获取启动类所在的包名。
		 */
		PackageImport(AnnotationMetadata metadata) {
			this.packageName = ClassUtils.getPackageName(metadata.getClassName());
		}

		public String getPackageName() {
			return this.packageName;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return this.packageName.equals(((PackageImport) obj).packageName);
		}

		@Override
		public int hashCode() {
			return this.packageName.hashCode();
		}

		@Override
		public String toString() {
			return "Package Import " + this.packageName;
		}

	}

	/**
	 * Holder for the base package (name may be null to indicate no scanning).
	 * 就是一个有 packages 属性的封装类。
	 */
	static final class BasePackages {

		private final List<String> packages;

		private boolean loggedBasePackageInfo;

		BasePackages(String... names) {
			List<String> packages = new ArrayList<>();
			for (String name : names) {
				if (StringUtils.hasText(name)) {
					packages.add(name);
				}
			}
			this.packages = packages;
		}

		public List<String> get() {
			if (!this.loggedBasePackageInfo) {
				if (this.packages.isEmpty()) {
					if (logger.isWarnEnabled()) {
						logger.warn("@EnableAutoConfiguration was declared on a class "
								+ "in the default package. Automatic @Repository and "
								+ "@Entity scanning is not enabled.");
					}
				} else {
					if (logger.isDebugEnabled()) {
						String packageNames = StringUtils
								.collectionToCommaDelimitedString(this.packages);
						logger.debug("@EnableAutoConfiguration was declared on a class "
								+ "in the package '" + packageNames
								+ "'. Automatic @Repository and @Entity scanning is "
								+ "enabled.");
					}
				}
				this.loggedBasePackageInfo = true;
			}
			return this.packages;
		}

	}

}
