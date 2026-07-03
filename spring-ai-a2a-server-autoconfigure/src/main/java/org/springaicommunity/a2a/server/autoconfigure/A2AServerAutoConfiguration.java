/*
 * Copyright 2025-2026 the original author or authors.
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

package org.springaicommunity.a2a.server.autoconfigure;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.config.DefaultValuesConfigProvider;
import io.a2a.server.events.InMemoryQueueManager;
import io.a2a.server.events.QueueManager;
import io.a2a.server.requesthandlers.DefaultRequestHandler;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.tasks.InMemoryPushNotificationConfigStore;
import io.a2a.server.tasks.InMemoryTaskStore;
import io.a2a.server.tasks.PushNotificationConfigStore;
import io.a2a.server.tasks.PushNotificationSender;
import io.a2a.server.tasks.TaskStateProvider;
import io.a2a.server.tasks.TaskStore;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.core.A2ARequestProcessor;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springaicommunity.a2a.server.web.reactive.A2AReactiveRoutes;
import org.springaicommunity.a2a.server.web.servlet.A2AServletRoutes;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Spring Boot auto-configuration for A2A Server.
 *
 * <p>
 * Automatically enables A2A protocol support when Spring AI ChatClient is on the
 * classpath. Provides A2A controllers, agent card metadata, and task API support.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@ConditionalOnProperty(prefix = A2AServerProperties.CONFIG_PREFIX, name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties(A2AServerProperties.class)
public class A2AServerAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(A2AServerAutoConfiguration.class);

	/**
	 * Log AgentCard at startup. Applications MUST provide AgentCard bean.
	 */
	@Autowired
	public void logAgentCard(AgentCard agentCard) {
		logger.info("Using AgentCard: {} (version: {})", agentCard.name(), agentCard.version());
	}

	/**
	 * Provide the shared, web-stack agnostic A2A request processor.
	 */
	@Bean
	@ConditionalOnMissingBean
	public A2ARequestProcessor a2aRequestProcessor(RequestHandler requestHandler) {
		return new A2ARequestProcessor(requestHandler);
	}

	/**
	 * Registers the A2A functional routes on a servlet web stack (WebMvc.fn).
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	static class A2AServletRoutesConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "a2aServletRouterFunction")
		org.springframework.web.servlet.function.RouterFunction<org.springframework.web.servlet.function.ServerResponse> a2aServletRouterFunction(
				A2ARequestProcessor a2aRequestProcessor, AgentCard agentCard) {
			logger.info("Registering A2A servlet (WebMvc.fn) routes");
			return A2AServletRoutes.routes(a2aRequestProcessor, agentCard);
		}

	}

	/**
	 * Registers the A2A functional routes on a reactive web stack (WebFlux.fn).
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
	static class A2AReactiveRoutesConfiguration {

		@Bean
		@ConditionalOnMissingBean(name = "a2aReactiveRouterFunction")
		org.springframework.web.reactive.function.server.RouterFunction<org.springframework.web.reactive.function.server.ServerResponse> a2aReactiveRouterFunction(
				A2ARequestProcessor a2aRequestProcessor, AgentCard agentCard) {
			logger.info("Registering A2A reactive (WebFlux.fn) routes");
			return A2AReactiveRoutes.routes(a2aRequestProcessor, agentCard);
		}

	}

	/**
	 * Provide default TaskStore (InMemoryTaskStore).
	 */
	@Bean
	@ConditionalOnMissingBean
	public TaskStore taskStore() {
		logger.info("Auto-configuring InMemoryTaskStore for task management");
		return new InMemoryTaskStore();
	}

	@Bean
	DefaultValuesConfigProvider defaultValuesConfigProvider() {
		return new DefaultValuesConfigProvider();
	}

	/**
	 * Configuration provider for A2A settings. If a property is not found in the Spring
	 * Environment, it falls back to default values provided by
	 * DefaultValuesConfigProvider.
	 */
	@Bean
	public SpringA2AConfigProvider configProvider(Environment environment,
			DefaultValuesConfigProvider defaultValuesConfigProvider) {
		logger.info("Auto-configuring SpringA2AConfigProvider for configuration");
		return new SpringA2AConfigProvider(environment, defaultValuesConfigProvider);
	}

	/**
	 * Provide default QueueManager (InMemoryQueueManager).
	 */
	@Bean
	@ConditionalOnMissingBean
	public QueueManager queueManager(TaskStore taskStore) {
		logger.info("Auto-configuring InMemoryQueueManager for event queue management");
		return new InMemoryQueueManager((TaskStateProvider) taskStore);
	}

	/**
	 * Provide default PushNotificationConfigStore (InMemoryPushNotificationConfigStore).
	 */
	@Bean
	@ConditionalOnMissingBean
	public PushNotificationConfigStore pushNotificationConfigStore() {
		logger.info("Auto-configuring InMemoryPushNotificationConfigStore");
		return new InMemoryPushNotificationConfigStore();
	}

	/**
	 * Provide default PushNotificationSender (no-op).
	 */
	@Bean
	@ConditionalOnMissingBean
	public PushNotificationSender pushNotificationSender() {
		logger.info("Auto-configuring no-op PushNotificationSender (override to enable)");
		return new PushNotificationSender() {
			@Override
			public void sendNotification(Task task) {
				logger.debug("Push notification requested for task {} but sender is disabled", task.getId());
			}
		};
	}

	/**
	 * Provide internal executor for async agent operations.
	 */
	@Bean
	@Qualifier("a2aInternal")
	@ConditionalOnMissingBean(name = "a2aInternalExecutor")
	public Executor a2aInternalExecutor(SpringA2AConfigProvider configProvider) {
		int corePoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.core-pool-size"));
		int maxPoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.max-pool-size"));
		long keepAliveSeconds = Long.parseLong(configProvider.getValue("a2a.executor.keep-alive-seconds"));

		logger.info("Creating A2A internal executor: corePoolSize={}, maxPoolSize={}, keepAliveSeconds={}",
				corePoolSize, maxPoolSize, keepAliveSeconds);

		AtomicInteger threadCounter = new AtomicInteger(1);
		ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepAliveSeconds,
				TimeUnit.SECONDS, new LinkedBlockingQueue<>(), runnable -> {
					Thread thread = new Thread(runnable);
					thread.setName("a2a-agent-executor-" + threadCounter.getAndIncrement());
					thread.setDaemon(false); // Non-daemon threads as per A2A spec
					return thread;
				});

		return executor;
	}

	/**
	 * Provide RequestHandler wiring all A2A SDK components together.
	 *
	 * <p>
	 * Note: Applications must provide their own {@link AgentExecutor} bean by extending
	 * {@link DefaultAgentExecutor} and implementing the {@code executeAsMessage} method.
	 */
	@Bean
	@ConditionalOnMissingBean
	public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore, PushNotificationSender pushSender,
			@Qualifier("a2aInternal") Executor executor) {

		logger.info("Creating DefaultRequestHandler with A2A SDK components");

		return DefaultRequestHandler.create(agentExecutor, taskStore, queueManager, pushConfigStore, pushSender,
				executor);
	}

}
