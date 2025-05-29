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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Annotation to be used to autowire a grpc client.
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcClient {

	/**
	 * The name of the grpc client channel. This name will be used to get the
	 * {@link GrpcClientProperties config options} for this client.
	 *
	 * <p>
	 * <b>Example:</b> <code>@GrpcClient("myClient")</code>
	 * {@code spring.grpc.client.channels.myClient.address=static://localhost:9090}
	 * </p>
	 * @return The name of the grpc client channel.
	 */
	String value() default "default";

	/**
	 * The name of proxy channel. When calling other services using a channel name other
	 * than the default channel discovered by the server, you can specify the channel name
	 * used by the server discovery to be used here. In ClientInterceptor, you can use key
	 * (CHANNEL_NAME_KEY, PROXY_CHANNEL_NAME_KEY
	 * {@link GrpcClientAnnotationBeanPostProcessor}) to get the value of name and
	 * proxyChannel in the annotation.
	 * @return The name of proxy channel.
	 */
	String proxyChannel() default "";

}