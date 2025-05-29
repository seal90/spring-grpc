/*
 * Copyright 2024-2024 the original author or authors.
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

package org.springframework.grpc.autoconfigure.client;

import java.lang.reflect.Field;
import java.lang.reflect.Member;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ClientInterceptors;
import io.grpc.MethodDescriptor;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.grpc.client.GrpcClientFactory;
import org.springframework.util.ReflectionUtils;

import io.grpc.Channel;
import io.grpc.stub.AbstractStub;

/**
 * Bean post processor for {@link GrpcClient} annotations.
 */
public class GrpcClientAnnotationBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware {

	public static final String CHANNEL_NAME = "SPRING_CHANNEL_NAME";

	public static final String PROXY_CHANNEL_NAME = "SPRING_PROXY_CHANNEL_NAME";

	public static final CallOptions.Key<String> CHANNEL_NAME_KEY = CallOptions.Key.create(CHANNEL_NAME);

	public static final CallOptions.Key<String> PROXY_CHANNEL_NAME_KEY = CallOptions.Key.create(PROXY_CHANNEL_NAME);

	// Using the same instance as GrpcClientFactoryPostProcessor ?
	private GrpcClientFactory grpcClientFactory;

	/**
	 * Process the bean's fields annotated with {@link GrpcClient}.
	 * @param bean the new bean instance
	 * @param beanName the name of the bean
	 * @return the bean instance to use, either the original or a wrapped one
	 * @throws BeansException – in case of errors
	 */
	@Override
	public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
		Class<?> clazz = bean.getClass();
		do {
			processFields(clazz, bean);

			clazz = clazz.getSuperclass();
		}
		while (clazz != null);
		return bean;
	}

	/**
	 * Processes the bean's fields annotated with {@link GrpcClient}.
	 * @param clazz The class to process.
	 * @param bean The bean to process.
	 */
	private void processFields(final Class<?> clazz, final Object bean) {
		for (final Field field : clazz.getDeclaredFields()) {
			final GrpcClient annotation = AnnotationUtils.findAnnotation(field, GrpcClient.class);
			if (annotation != null) {
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, bean, processInjectionPoint(field, field.getType(), annotation));
			}
		}
	}

	/**
	 * Processes the given injection point and computes the appropriate value for the
	 * injection.
	 * @param <T> The type of the value to be injected.
	 * @param injectionTarget The target of the injection.
	 * @param injectionType The class that will be used to compute injection.
	 * @param annotation The annotation on the target with the metadata for the injection.
	 * @return The value to be injected for the given injection point.
	 */
	protected <T> T processInjectionPoint(final Member injectionTarget, final Class<T> injectionType,
			final GrpcClient annotation) {
		final String channelName = annotation.value();
		final String proxyChannelName = annotation.proxyChannel();

		String finalChannelName = channelName(channelName, proxyChannelName);
		if (Channel.class.equals(injectionType)) {
			return handleChannel(channelName, proxyChannelName, finalChannelName, injectionType);
		}
		else if (AbstractStub.class.isAssignableFrom(injectionType)) {
			return handleAbstractStub(channelName, proxyChannelName, finalChannelName, injectionType);
		}
		else {
			if (injectionTarget != null) {
				throw new InvalidPropertyException(injectionTarget.getDeclaringClass(), injectionTarget.getName(),
						"Unsupported type " + injectionType.getName());
			}
			else {
				throw new BeanInstantiationException(injectionType, "Unsupported grpc stub or channel type");
			}
		}
	}

	/**
	 * Handles the injection of Channel Type.
	 * @param channelName {@link GrpcClient} annotation value.
	 * @param proxyChannelName {@link GrpcClient} annotation proxyChannel.
	 * @param finalChannelName the resolved channel name
	 * @param injectionType the expected channel class
	 * @return the Channel
	 * @param <T> The type of the value to be injected.
	 */
	private <T> T handleChannel(String channelName, String proxyChannelName, String finalChannelName,
			Class<T> injectionType) {
		if (!proxyChannelName.isEmpty()) {
			throw new BeanInstantiationException(injectionType, "GrpcClient cannot use proxyChannel with Channel");
		}

		// Prioritize obtaining channel from the ApplicabilityContext ?
		Channel channel = grpcClientFactory.getChannel(finalChannelName);
		channel = ClientInterceptors.intercept(channel, new ClientInterceptor() {
			@Override
			public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> method,
					final CallOptions callOptions, final Channel next) {
				CallOptions withNameCallOptions = callOptions.withOption(CHANNEL_NAME_KEY, channelName);
				return next.newCall(method, withNameCallOptions);
			}
		});
		return injectionType.cast(channel);
	}

	/**
	 * Handles the injection of AbstractStub Type.
	 * @param channelName {@link GrpcClient} annotation value.
	 * @param proxyChannelName {@link GrpcClient} annotation proxyChannel.
	 * @param finalChannelName the resolved channel name
	 * @param injectionType the expected channel class
	 * @return the AbstractStub
	 * @param <T> The type of the value to be injected.
	 */
	private <T> T handleAbstractStub(String channelName, String proxyChannelName, String finalChannelName,
			Class<T> injectionType) {
		// Prioritize obtaining channel from the ApplicabilityContext, then build stub?
		AbstractStub<?> stub = (AbstractStub<?>) grpcClientFactory.getClient(finalChannelName, injectionType, null);
		stub = stub.withOption(CHANNEL_NAME_KEY, channelName);
		stub = stub.withOption(PROXY_CHANNEL_NAME_KEY, proxyChannelName);
		return injectionType.cast(stub);
	}

	/**
	 * Computes the channel name to use.
	 * @param channelName {@link GrpcClient} annotation value.
	 * @param proxyChannelName {@link GrpcClient} annotation proxyChannel.
	 * @return The channel name to use.
	 */
	private String channelName(String channelName, String proxyChannelName) {
		if (!proxyChannelName.isEmpty()) {
			return proxyChannelName;
		}
		if (!channelName.isEmpty()) {
			return channelName;
		}
		return "default";
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.grpcClientFactory = new GrpcClientFactory(applicationContext);
	}

}