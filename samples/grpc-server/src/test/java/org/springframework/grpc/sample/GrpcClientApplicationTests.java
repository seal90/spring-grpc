package org.springframework.grpc.sample;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.autoconfigure.client.GrpcClient;
import org.springframework.grpc.autoconfigure.client.GrpcClientAnnotationBeanPostProcessor;
import org.springframework.grpc.client.BlockingStubFactory;
import org.springframework.grpc.client.BlockingV2StubFactory;
import org.springframework.grpc.client.FutureStubFactory;
import org.springframework.grpc.client.GlobalClientInterceptor;
import org.springframework.grpc.client.ImportGrpcClients;
import org.springframework.grpc.client.SimpleStubFactory;
import org.springframework.grpc.sample.proto.HelloReply;
import org.springframework.grpc.sample.proto.HelloRequest;
import org.springframework.grpc.sample.proto.SimpleGrpc;
import org.springframework.grpc.test.AutoConfigureInProcessTransport;

import io.grpc.stub.AbstractStub;

public class GrpcClientApplicationTests {

	@Nested
	@SpringBootTest
	@AutoConfigureInProcessTransport
	class NoAutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Test
		void noStubIsCreated() {
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleFutureStub")).isFalse();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).isEmpty();
		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.default-channel.address=0.0.0.0:9090")
	@AutoConfigureInProcessTransport
	class DefaultAutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Test
		void onlyDefaultStubIsCreated() {
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isTrue();
			assertThat(context.getBean(SimpleGrpc.SimpleBlockingStub.class)).isNotNull();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleFutureStub")).isFalse();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).hasSize(1);
		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.default-channel.address=0.0.0.0:9090")
	@AutoConfigureInProcessTransport
	class SpecificAutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Test
		void stubOfCorrectTypeIsCreated() {
			assertThat(context.containsBeanDefinition("simpleFutureStub")).isTrue();
			assertThat(context.getBean(SimpleGrpc.SimpleFutureStub.class)).isNotNull();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isFalse();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).hasSize(1);
		}

		@TestConfiguration
		@ImportGrpcClients(basePackageClasses = SimpleGrpc.class, factory = FutureStubFactory.class)
		static class TestConfig {

		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.default-channel.address=0.0.0.0:9090")
	@AutoConfigureInProcessTransport
	class BlockingV2AutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Test
		void stubOfCorrectTypeIsCreated() {
			assertThat(context.containsBeanDefinition("simpleBlockingV2Stub")).isTrue();
			assertThat(context.getBean(SimpleGrpc.SimpleBlockingV2Stub.class)).isNotNull();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isFalse();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).hasSize(1);
		}

		@TestConfiguration
		@ImportGrpcClients(basePackageClasses = SimpleGrpc.class, factory = BlockingV2StubFactory.class)
		static class TestConfig {

		}

	}

	@Nested
	@SpringBootTest
	@AutoConfigureInProcessTransport
	class ExplicitImportClientsWithNoFactory {

		@Autowired
		private ApplicationContext context;

		@Test
		void stubOfCorrectTypeIsCreated() {
			assertThat(context.containsBeanDefinition("simpleBlockingStub")).isTrue();
			assertThat(context.getBean(SimpleGrpc.SimpleBlockingStub.class)).isNotNull();
			assertThat(context.containsBeanDefinition("simpleStub")).isFalse();
			assertThat(context.containsBeanDefinition("simpleFutureStub")).isFalse();
			assertThat(context.getBeanNamesForType(AbstractStub.class)).hasSize(1);
		}

		@TestConfiguration
		@ImportGrpcClients
		static class TestConfig {

		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.default-channel.address=0.0.0.0:9090")
	@AutoConfigureInProcessTransport
	class AllStubAutowiredClients {

		@Autowired
		private ApplicationContext context;

		@Autowired
		private SimpleGrpc.SimpleBlockingStub simpleBlockingStub;

		@Autowired
		private SimpleGrpc.SimpleBlockingV2Stub simpleBlockingV2Stub;

		@Autowired
		private SimpleGrpc.SimpleFutureStub simpleFutureStub;

		@Autowired
		private SimpleGrpc.SimpleStub simpleStub;

		@Test
		void stubsCreatedWithRightName() {
			assertNotNull(context.getBeansOfType(SimpleGrpc.SimpleBlockingStub.class).get("simpleBlockingStub"));
			assertNotNull(context.getBeansOfType(SimpleGrpc.SimpleBlockingV2Stub.class).get("simpleBlockingV2Stub"));
			assertNotNull(context.getBeansOfType(SimpleGrpc.SimpleFutureStub.class).get("simpleFutureStub"));
			assertNotNull(context.getBeansOfType(SimpleGrpc.SimpleStub.class).get("simpleStub"));
			assertThat(context.getBeanNamesForType(AbstractStub.class)).hasSize(4);

			assertNotNull(simpleBlockingStub);
			assertNotNull(simpleBlockingV2Stub);
			assertNotNull(simpleFutureStub);
			assertNotNull(simpleStub);
		}

		@TestConfiguration
		@ImportGrpcClients.Container(value = {
				@ImportGrpcClients(basePackageClasses = SimpleGrpc.class, factory = BlockingStubFactory.class),
				@ImportGrpcClients(basePackageClasses = SimpleGrpc.class, factory = BlockingV2StubFactory.class),
				@ImportGrpcClients(basePackageClasses = SimpleGrpc.class, factory = FutureStubFactory.class),
				@ImportGrpcClients(basePackageClasses = SimpleGrpc.class, factory = SimpleStubFactory.class), })
		static class TestConfig {

		}

	}

	@Nested
	@SpringBootTest(properties = "spring.grpc.client.default-channel.address=0.0.0.0:9090")
	@AutoConfigureInProcessTransport
	@Configuration(proxyBeanMethods = false)
	class GrpcClientAnnotation {

		@Autowired
		private ApplicationContext context;

		@GrpcClient
		private Channel channel;

		@GrpcClient
		private SimpleGrpc.SimpleBlockingStub simpleBlockingStub;

		@TestConfiguration
		static class Config {

			@Bean
			@GlobalClientInterceptor
			public ClientInterceptor namePassingClientInterceptor() {
				return new ClientInterceptor() {
					@Override
					public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
							CallOptions callOptions, Channel next) {

						String name = callOptions.getOption(GrpcClientAnnotationBeanPostProcessor.CHANNEL_NAME_KEY);
						assertEquals("default", name);

						String proxyChannelName = callOptions
							.getOption(GrpcClientAnnotationBeanPostProcessor.PROXY_CHANNEL_NAME_KEY);
						assertEquals("", proxyChannelName);

						return new ForwardingClientCall.SimpleForwardingClientCall<>(
								next.newCall(method, callOptions)) {
							@Override
							public void start(Listener responseListener, Metadata headers) {

								// Passing serviceName, if necessary you can use
								// proxyChannelName to judge and use different keys to
								// pass data
								headers.put(Metadata.Key.of("USER_DEFINE_NAME", Metadata.ASCII_STRING_MARSHALLER),
										name);

								super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
										responseListener) {
									@Override
									public void onHeaders(Metadata headers) {
										String instance = headers.get(Metadata.Key.of("USER_DEFINE_INSTANCE_NAME",
												Metadata.ASCII_STRING_MARSHALLER));
										assertEquals("ONE", instance);
										super.onHeaders(headers);
									}
								}, headers);
							}
						};
					}
				};
			}

		}

		@Test
		void stubAutowireSuccess() {
			assertNotNull(channel);
			assertNotNull(simpleBlockingStub);
			HelloReply helloReply = simpleBlockingStub.sayHello(HelloRequest.newBuilder().setName("Alien").build());
			assertEquals("Hello ==> Alien", helloReply.getMessage());
		}
	}

}
