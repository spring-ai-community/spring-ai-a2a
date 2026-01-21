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

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.config.A2AConfigProvider;
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
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.executor.DefaultChatClientAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Spring Boot auto-configuration for A2A Server.
 *
 * <p>This auto-configuration automatically enables A2A protocol support when:
 * <ul>
 *   <li>Spring AI ChatClient is on the classpath</li>
 *   <li>A ChatClient bean is present in the application context</li>
 * </ul>
 *
 * <p><strong>What it provides:</strong>
 * <ul>
 *   <li>A2A protocol controllers ({@code /a2a} endpoint)</li>
 *   <li>Agent card metadata endpoint</li>
 *   <li>Task API support for long-running operations</li>
 * </ul>
 *
 * <p><strong>Usage:</strong>
 * Simply add {@code spring-ai-a2a-server} as a dependency and create a ChatClient bean:
 * <pre>
 * &#64;Bean
 * public ChatClient myAgent(ChatModel chatModel) {
 *     return ChatClient.builder(chatModel)
 *         .defaultSystem("You are a helpful assistant...")
 *         .build();
 * }
 * </pre>
 *
 * The A2A server will automatically expose your agent at {@code http://localhost:PORT/a2a}
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@ConditionalOnProperty(prefix = "spring.ai.a2a.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(A2AServerProperties.class)
@ComponentScan(basePackages = {
	"org.springaicommunity.a2a.server.controller"
})
public class A2AServerAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(A2AServerAutoConfiguration.class);

	/**
	 * Provide default AgentCard if none is configured by the user.
	 *
	 * <p>Users can override this by providing their own AgentCard bean with
	 * custom metadata, capabilities, and supported interfaces.
	 *
	 * @return default agent card
	 */
	@Bean
	@ConditionalOnMissingBean
	public AgentCard agentCard() {
		return new AgentCard(
			"Spring AI A2A Agent", // name
			"A2A agent powered by Spring AI", // description
			"http://localhost:8080/a2a", // url
			null, // provider
			"1.0.0", // version
			null, // documentationUrl
			new AgentCapabilities(false, false, false, List.of()), // capabilities
			List.of("text"), // defaultInputModes
			List.of("text"), // defaultOutputModes
			List.of(), // skills
			false, // supportsAuthenticatedExtendedCard
			null, // securitySchemes
			null, // security
			null, // iconUrl
			List.of(new AgentInterface("JSONRPC", "http://localhost:8080/a2a")), // additionalInterfaces
			"JSONRPC", // preferredTransport
			"0.1.0", // protocolVersion
			null // signatures
		);
	}

	/**
	 * Provide default TaskStore for task management.
	 *
	 * <p>Uses the A2A SDK's InMemoryTaskStore implementation by default.
	 * Override by providing your own TaskStore bean in a @Configuration class.
	 *
	 * <p><strong>Example override:</strong>
	 * <pre>
	 * &#64;Bean
	 * public TaskStore taskStore() {
	 *     return new RedisTaskStore(redisTemplate);
	 * }
	 * </pre>
	 *
	 * @return in-memory task store
	 */
	@Bean
	@ConditionalOnMissingBean
	public TaskStore taskStore() {
		logger.info("Auto-configuring InMemoryTaskStore for task management");
		return new InMemoryTaskStore();
	}

	/**
	 * Provide default A2AConfigProvider for configuration management.
	 *
	 * <p>Uses the A2A SDK's DefaultValuesConfigProvider implementation by default.
	 * Override by providing your own A2AConfigProvider bean in a @Configuration class.
	 *
	 * <p><strong>Example override:</strong>
	 * <pre>
	 * &#64;Bean
	 * public A2AConfigProvider configProvider(Environment environment) {
	 *     return new SpringEnvironmentConfigProvider(environment);
	 * }
	 * </pre>
	 *
	 * @return default config provider
	 */
	@Bean
	@ConditionalOnMissingBean
	public A2AConfigProvider configProvider() {
		logger.info("Auto-configuring DefaultValuesConfigProvider for configuration");
		return new DefaultValuesConfigProvider();
	}

	/**
	 * Provide default QueueManager for managing event queues per task.
	 *
	 * <p>Uses the A2A SDK's InMemoryQueueManager implementation by default.
	 * Override by providing your own QueueManager bean for distributed scenarios.
	 *
	 * @param taskStore the task store (which also implements TaskStateProvider)
	 * @return queue manager
	 */
	@Bean
	@ConditionalOnMissingBean
	public QueueManager queueManager(TaskStore taskStore) {
		logger.info("Auto-configuring InMemoryQueueManager for event queue management");
		// InMemoryTaskStore implements TaskStateProvider
		return new InMemoryQueueManager((TaskStateProvider) taskStore);
	}

	/**
	 * Provide default PushNotificationConfigStore for push notification configuration.
	 *
	 * <p>Uses the A2A SDK's InMemoryPushNotificationConfigStore implementation by default.
	 *
	 * @return push notification config store
	 */
	@Bean
	@ConditionalOnMissingBean
	public PushNotificationConfigStore pushNotificationConfigStore() {
		logger.info("Auto-configuring InMemoryPushNotificationConfigStore");
		return new InMemoryPushNotificationConfigStore();
	}

	/**
	 * Provide default PushNotificationSender.
	 *
	 * <p>No-op implementation by default. Override to enable actual push notifications.
	 *
	 * @return push notification sender
	 */
	@Bean
	@ConditionalOnMissingBean
	public PushNotificationSender pushNotificationSender() {
		logger.info("Auto-configuring no-op PushNotificationSender (override to enable)");
		return new PushNotificationSender() {
			@Override
			public void sendNotification(Task task) {
				// No-op implementation - push notifications not enabled by default
				logger.debug("Push notification requested for task {} but sender is disabled", task.getId());
			}
		};
	}

	/**
	 * Provide internal executor for async agent operations.
	 *
	 * <p>Configured with thread pool settings from A2AConfigProvider.
	 *
	 * @param configProvider the config provider for thread pool settings
	 * @return executor for async operations
	 */
	@Bean
	@Qualifier("a2aInternal")
	@ConditionalOnMissingBean(name = "a2aInternalExecutor")
	public Executor a2aInternalExecutor(A2AConfigProvider configProvider) {
		int corePoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.core-pool-size"));
		int maxPoolSize = Integer.parseInt(configProvider.getValue("a2a.executor.max-pool-size"));
		long keepAliveSeconds = Long.parseLong(configProvider.getValue("a2a.executor.keep-alive-seconds"));

		logger.info("Creating A2A internal executor: corePoolSize={}, maxPoolSize={}, keepAliveSeconds={}",
				corePoolSize, maxPoolSize, keepAliveSeconds);

		AtomicInteger threadCounter = new AtomicInteger(1);
		ThreadPoolExecutor executor = new ThreadPoolExecutor(
			corePoolSize,
			maxPoolSize,
			keepAliveSeconds,
			TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(),
			runnable -> {
				Thread thread = new Thread(runnable);
				thread.setName("a2a-agent-executor-" + threadCounter.getAndIncrement());
				thread.setDaemon(false); // Non-daemon threads as per A2A spec
				return thread;
			}
		);

		return executor;
	}

	/**
	 * Provide default AgentExecutor that bridges ChatClient to A2A protocol.
	 *
	 * <p>Uses {@link DefaultChatClientAgentExecutor} which wraps ChatClient invocations
	 * with proper A2A task lifecycle management using TaskUpdater.
	 *
	 * <p>Override by providing your own AgentExecutor bean for custom logic:
	 * <pre>
	 * &#64;Bean
	 * public AgentExecutor customExecutor(ChatClient.Builder builder, MyTools tools) {
	 *     ChatClient chatClient = builder.clone()
	 *         .defaultSystem("Custom prompt...")
	 *         .defaultTools(tools)
	 *         .build();
	 *
	 *     return new AgentExecutor() {
	 *         public void execute(RequestContext context, EventQueue eventQueue) {
	 *             TaskUpdater updater = new TaskUpdater(context, eventQueue);
	 *             updater.startWork();
	 *             // Custom logic here
	 *             updater.complete();
	 *         }
	 *
	 *         public void cancel(RequestContext context, EventQueue eventQueue) {
	 *             new TaskUpdater(context, eventQueue).cancel();
	 *         }
	 *     };
	 * }
	 * </pre>
	 *
	 * @param chatClient the ChatClient to wrap
	 * @return agent executor
	 */
	@Bean
	@ConditionalOnMissingBean
	public AgentExecutor agentExecutor(ChatClient chatClient) {
		logger.info("Auto-configuring DefaultChatClientAgentExecutor wrapping ChatClient");
		return new DefaultChatClientAgentExecutor(chatClient);
	}

	/**
	 * Provide RequestHandler that wires all A2A SDK components together.
	 *
	 * <p>This is the core of the A2A server implementation. It handles:
	 * <ul>
	 *   <li>Protocol-level request/response handling</li>
	 *   <li>Task lifecycle management</li>
	 *   <li>Event queue coordination</li>
	 *   <li>Push notification delivery</li>
	 * </ul>
	 *
	 * @param agentExecutor the agent executor for processing messages
	 * @param taskStore the task store for persistence
	 * @param queueManager the queue manager for event queues
	 * @param pushConfigStore the push notification config store
	 * @param pushSender the push notification sender
	 * @param executor the internal executor for async operations
	 * @return request handler
	 */
	@Bean
	@ConditionalOnMissingBean
	public RequestHandler requestHandler(
			AgentExecutor agentExecutor,
			TaskStore taskStore,
			QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore,
			PushNotificationSender pushSender,
			@Qualifier("a2aInternal") Executor executor) {

		logger.info("Creating DefaultRequestHandler with A2A SDK components");

		return DefaultRequestHandler.create(
			agentExecutor,
			taskStore,
			queueManager,
			pushConfigStore,
			pushSender,
			executor
		);
	}

}
