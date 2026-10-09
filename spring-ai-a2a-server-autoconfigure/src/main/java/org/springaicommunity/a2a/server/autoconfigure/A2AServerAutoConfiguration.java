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

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler;
import org.a2aproject.sdk.compat03.spec.AgentCapabilities_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentSkill_v0_3;
import org.a2aproject.sdk.compat03.spec.TransportProtocol_v0_3;
import org.a2aproject.sdk.compat03.transport.jsonrpc.handler.JSONRPCHandler_v0_3;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.auth.TaskAuthorizationProvider;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.tasks.InMemoryPushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.InMemoryTaskStore;
import org.a2aproject.sdk.server.tasks.PushNotificationConfigStore;
import org.a2aproject.sdk.server.tasks.PushNotificationSender;
import org.a2aproject.sdk.server.tasks.TaskStateProvider;
import org.a2aproject.sdk.server.tasks.TaskStore;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.StreamingEventKind;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.controller.AgentCardController;
import org.springaicommunity.a2a.server.controller.MessageController;
import org.springaicommunity.a2a.server.controller.TaskController;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
 * @author Thorben Janssen
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

	@Bean
	@ConditionalOnMissingBean
	AgentCardController agentCardController(AgentCard agentCard) {
		return new AgentCardController(agentCard);
	}

	/**
	 * Provide the SDK's JSON-RPC handler that dispatches the JSON-RPC requests to the
	 * {@link RequestHandler}.
	 */
	@Bean
	@ConditionalOnMissingBean
	public JSONRPCHandler jsonRpcHandler(AgentCard agentCard, RequestHandler requestHandler,
			@Qualifier("a2aInternal") Executor executor) {
		return new JSONRPCHandler(agentCard, requestHandler, executor);
	}

	/**
	 * Provide the SDK's JSON-RPC handler for A2A v0.3 requests. It converts the requests
	 * to v1.0, dispatches them to the {@link RequestHandler} and converts the results
	 * back to v0.3.
	 */
	@Bean
	@ConditionalOnMissingBean
	public JSONRPCHandler_v0_3 jsonRpcHandlerV03(AgentCard agentCard, RequestHandler requestHandler,
			@Qualifier("a2aInternal") Executor executor) {
		return new JSONRPCHandler_v0_3(toV03AgentCard(agentCard), executor,
				new Convert_v0_3_To10RequestHandler(requestHandler));
	}

	@Bean
	@ConditionalOnMissingBean
	MessageController messageController(JSONRPCHandler jsonRpcHandler, JSONRPCHandler_v0_3 jsonRpcHandlerV03) {
		return new MessageController(jsonRpcHandler, jsonRpcHandlerV03);
	}

	/**
	 * Converts the agent card to v0.3. The {@link JSONRPCHandler_v0_3} uses it to check
	 * the agent's capabilities. An authenticated extended card isn't supported for v0.3
	 * requests.
	 */
	static AgentCard_v0_3 toV03AgentCard(AgentCard agentCard) {
		List<AgentSkill_v0_3> skills = agentCard.skills()
			.stream()
			.map(skill -> new AgentSkill_v0_3(skill.id(), skill.name(), skill.description(), skill.tags(),
					skill.examples(), skill.inputModes(), skill.outputModes(), null))
			.toList();

		return new AgentCard_v0_3.Builder().name(agentCard.name())
			.description(agentCard.description())
			.url(Objects.requireNonNullElse(AgentCardController.jsonRpcUrl(agentCard), ""))
			.version(agentCard.version())
			.documentationUrl(agentCard.documentationUrl())
			.capabilities(new AgentCapabilities_v0_3.Builder().streaming(agentCard.capabilities().streaming())
				.pushNotifications(agentCard.capabilities().pushNotifications())
				.build())
			.defaultInputModes(agentCard.defaultInputModes())
			.defaultOutputModes(agentCard.defaultOutputModes())
			.skills(skills)
			.supportsAuthenticatedExtendedCard(false)
			.iconUrl(agentCard.iconUrl())
			.preferredTransport(TransportProtocol_v0_3.JSONRPC.asString())
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	TaskController taskController(RequestHandler requestHandler) {
		return new TaskController(requestHandler);
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
	 * Provide MainEventBus for coordinating task events across SDK components.
	 */
	@Bean
	@ConditionalOnMissingBean
	public MainEventBus mainEventBus() {
		logger.info("Auto-configuring MainEventBus for A2A event coordination");
		return new MainEventBus();
	}

	/**
	 * Provide default QueueManager (InMemoryQueueManager). Requires MainEventBus for
	 * event routing in SDK 1.4.0+.
	 */
	@Bean
	@ConditionalOnMissingBean
	public QueueManager queueManager(TaskStore taskStore, MainEventBus mainEventBus) {
		logger.info("Auto-configuring InMemoryQueueManager for event queue management");
		return new InMemoryQueueManager((TaskStateProvider) taskStore, mainEventBus);
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
			public void sendNotification(StreamingEventKind event, Task task) {
				logger.debug("Push notification requested for task {} but sender is disabled", task.id());
			}
		};
	}

	/**
	 * Provide MainEventBusProcessor that bridges the MainEventBus to task storage and
	 * push notification dispatch. The processor background thread is started
	 * automatically when DefaultRequestHandler is built.
	 */
	@Bean
	@ConditionalOnMissingBean
	public MainEventBusProcessor mainEventBusProcessor(MainEventBus mainEventBus, TaskStore taskStore,
			PushNotificationSender pushNotificationSender, QueueManager queueManager) {
		logger.info("Auto-configuring MainEventBusProcessor for event processing");
		return new MainEventBusProcessor(mainEventBus, taskStore, pushNotificationSender, queueManager);
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
	 * {@link DefaultAgentExecutor} and implementing the {@code execute} method.
	 *
	 * <p>
	 * The {@link DefaultRequestHandler#builder()} replaces the removed
	 * {@code DefaultRequestHandler.create()} factory method in SDK 1.4.0. Calling
	 * {@code build()} automatically starts the {@link MainEventBusProcessor} background
	 * thread.
	 */
	@Bean
	@ConditionalOnMissingBean
	public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager,
			PushNotificationConfigStore pushConfigStore, MainEventBusProcessor mainEventBusProcessor,
			@Qualifier("a2aInternal") Executor executor, SpringA2AConfigProvider configProvider,
			ObjectProvider<TaskAuthorizationProvider> authorizationProvider) {

		logger.info("Creating DefaultRequestHandler with A2A SDK 1.4.0 components");

		// The builder doesn't read the SDK configuration. Without a
		// TaskAuthorizationProvider, the SDK denies all task operations unless
		// a2a.authorization.required is set to false.
		boolean authorizationRequired = Boolean.parseBoolean(configProvider.getValue("a2a.authorization.required"));

		return DefaultRequestHandler.builder()
			.agentExecutor(agentExecutor)
			.taskStore(taskStore)
			.queueManager(queueManager)
			.pushConfigStore(pushConfigStore)
			.mainEventBusProcessor(mainEventBusProcessor)
			.executor(executor)
			.eventConsumerExecutor(executor)
			.authorizationProvider(authorizationProvider.getIfAvailable())
			.authorizationRequired(authorizationRequired)
			.build();
	}

}
