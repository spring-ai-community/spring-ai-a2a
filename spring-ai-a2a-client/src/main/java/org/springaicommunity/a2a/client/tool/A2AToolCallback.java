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

package org.springaicommunity.a2a.client.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springaicommunity.a2a.core.MessageUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Spring AI ToolCallback for delegating tasks to remote A2A agents via HTTP.
 *
 * <p>This implementation exposes remote A2A agents as standard Spring AI tools,
 * allowing the LLM to autonomously delegate tasks to specialized agents running
 * as independent services. Remote agents are invoked via the A2A protocol using
 * a simple agent type → URL mapping.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Direct remote agent invocation via A2A protocol</li>
 *   <li>Event-driven response handling (MessageEvent and TaskEvent)</li>
 *   <li>Synchronous and asynchronous execution modes</li>
 *   <li>Automatic client caching per agent URL</li>
 *   <li>Configurable timeouts per agent</li>
 * </ul>
 *
 * <p><strong>Usage Example:</strong>
 * <pre>
 * // Register remote A2A agents as tools
 * Map&lt;String, String&gt; agentUrls = Map.of(
 *     "weather", "http://localhost:10001/a2a",
 *     "accommodation", "http://localhost:10002/a2a"
 * );
 *
 * ToolCallback a2aAgentTool = new A2AToolCallback(
 *     agentUrls,
 *     Duration.ofMinutes(2)
 * );
 *
 * // LLM can now call A2AAgent(subagent_type="weather", prompt="...")
 * ChatClient mainClient = ChatClient.builder(chatModel)
 *     .defaultToolCallbacks(a2aAgentTool)
 *     .build();
 * </pre>
 *
 * <p><strong>Integration Pattern:</strong>
 * <p>The LLM sees a single tool named "A2AAgent" with parameters:
 * <ul>
 *   <li><code>description</code> - Short task summary (3-5 words)</li>
 *   <li><code>prompt</code> - Full instructions for the remote agent</li>
 *   <li><code>subagent_type</code> - Type of agent to invoke (from agentUrls map)</li>
 *   <li><code>run_in_background</code> - Optional async execution flag</li>
 * </ul>
 *
 * <p>When the LLM calls the tool, this callback routes the request to the appropriate
 * remote A2A agent based on the subagent_type and returns the agent's response.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public class A2AToolCallback implements ToolCallback {

	/**
	 * Tool invocation parameters for A2A agent task delegation.
	 *
	 * @param description Short task summary (3-5 words)
	 * @param prompt Full instructions for the agent
	 * @param subagentType Type of agent to invoke (e.g., "weather", "accommodation")
	 * @param runInBackground Whether to execute asynchronously (default: false)
	 * @param includeProgress Whether to include progress updates in result (default: true)
	 * @param model Optional model override (e.g., "sonnet", "opus", "haiku")
	 * @param resume Optional task ID to resume previous session
	 */
	private record TaskParams(
			String description,
			String prompt,
			@JsonProperty("subagent_type") String subagentType,
			@JsonProperty("run_in_background") Boolean runInBackground,
			@JsonProperty("include_progress") Boolean includeProgress,
			String model,
			String resume) {

		/**
		 * Get runInBackground with default value.
		 */
		public boolean isRunInBackground() {
			return runInBackground != null && runInBackground;
		}

		/**
		 * Get includeProgress with default value.
		 */
		public boolean isIncludeProgress() {
			return includeProgress == null || includeProgress;
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(A2AToolCallback.class);

	private final Map<String, String> agentUrls;

	private final Duration defaultTimeout;

	private final ObjectMapper objectMapper = new ObjectMapper();

	private final ConcurrentHashMap<String, CompletableFuture<String>> backgroundTasks = new ConcurrentHashMap<>();

	/**
	 * Create A2A agent tool with agent URLs and default timeout.
	 * @param agentUrls map of agent type to A2A endpoint URL
	 * @param defaultTimeout default timeout for agent calls
	 */
	public A2AToolCallback(Map<String, String> agentUrls, Duration defaultTimeout) {
		this.agentUrls = new HashMap<>(agentUrls); // Defensive copy
		this.defaultTimeout = defaultTimeout != null ? defaultTimeout : Duration.ofMinutes(5);
		logger.info("A2AToolCallback initialized with {} agents: {}", agentUrls.size(), agentUrls.keySet());
	}

	/**
	 * Create A2A agent tool with agent URLs (default 5 minute timeout).
	 * @param agentUrls map of agent type to A2A endpoint URL
	 */
	public A2AToolCallback(Map<String, String> agentUrls) {
		this(agentUrls, Duration.ofMinutes(5));
	}

	@Override
	public ToolDefinition getToolDefinition() {
		String inputSchema = """
				{
				  "type": "object",
				  "properties": {
				    "description": {
				      "type": "string",
				      "description": "Short 3-5 word task summary"
				    },
				    "prompt": {
				      "type": "string",
				      "description": "Full instructions for the remote A2A agent"
				    },
				    "subagent_type": {
				      "type": "string",
				      "description": "Type of remote agent to invoke (e.g., weather, accommodation)"
				    },
				    "run_in_background": {
				      "type": "boolean",
				      "description": "Whether to run the task asynchronously (default: false)"
				    }
				  },
				  "required": ["description", "prompt", "subagent_type"]
				}
				""";

		return DefaultToolDefinition.builder()
			.name("A2AAgent")
			.description("""
					Delegate work to a specialized remote A2A agent for complex tasks.

					Use this when:
					- The task requires specialized knowledge or capabilities
					- The task is complex and needs dedicated focus
					- You want to delegate to a remote agent (weather, accommodation, etc.)
					- The remote agent has domain-specific expertise

					Available agent types: %s

					Remote agents run as independent services and may have different
					models or configurations optimized for their domain.
					""".formatted(agentUrls.keySet()))
			.inputSchema(inputSchema)
			.build();
	}

	@Override
	public String call(String functionArguments) {
		logger.debug("A2AAgent tool invoked with arguments: {}", functionArguments);

		try {
			TaskParams params = objectMapper.readValue(functionArguments, TaskParams.class);

			String agentUrl = agentUrls.get(params.subagentType());
			if (agentUrl == null) {
				return formatError("Remote A2A agent not found: " + params.subagentType(),
						new IllegalArgumentException("Available agents: " + agentUrls.keySet()));
			}

			if (params.isRunInBackground()) {
				return executeBackground(agentUrl, params);
			}
			else {
				return executeSynchronous(agentUrl, params);
			}
		}
		catch (Exception e) {
			logger.error("A2AAgent execution failed", e);
			return formatError("Failed to execute remote A2A agent", e);
		}
	}

	/**
	 * Execute task synchronously and wait for immediate response.
	 *
	 * <p>Uses the A2A SDK's event-driven API with a CountDownLatch to block until
	 * the response is received. Handles both MessageEvent and TaskEvent responses.
	 *
	 * <p>This method blocks until the agent responds or the configured
	 * timeout expires (default: 5 minutes).
	 *
	 * @param agentUrl the agent URL
	 * @param params the task parameters including prompt and agent type
	 * @return formatted result containing the agent's response
	 */
	private String executeSynchronous(String agentUrl, TaskParams params) {
		logger.info("Executing synchronous A2A task: {} (type: {})", params.description(), params.subagentType());

		// Create A2A Message
		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart(params.prompt())))
			.build();

		// Use CountDownLatch to block until response is received
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		// Create event consumer
		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			try {
				if (event instanceof MessageEvent messageEvent) {
					// Store the message directly
					responseRef.set(messageEvent.getMessage());
					latch.countDown();
				}
				else if (event instanceof TaskEvent taskEvent) {
					// For task completion, extract message from artifacts
					if (isTaskComplete(taskEvent)) {
						Message response = extractMessageFromTaskEvent(taskEvent);
						responseRef.set(response);
						latch.countDown();
					}
				}
			}
			catch (Exception e) {
				errorRef.set(e);
				latch.countDown();
			}
		};

		// Create client with consumer
		Client client = createClient(agentUrl, List.of(consumer));

		// Send message
		client.sendMessage(request);

		// Wait for response with timeout
		try {
			boolean completed = latch.await(defaultTimeout.toSeconds(), TimeUnit.SECONDS);

			if (!completed) {
				throw new RuntimeException("Timeout waiting for A2A agent response after " + defaultTimeout);
			}

			if (errorRef.get() != null) {
				throw new RuntimeException("Error during A2A execution", errorRef.get());
			}

			Message response = responseRef.get();
			logger.info("A2A task completed: {} (status: {})", params.description(),
					response != null ? "SUCCESS" : "FAILED");

			// Synchronous execution returns only final result without progress updates
			return formatResult(params, List.of(), response);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException("Interrupted while waiting for A2A response", e);
		}
	}

	/**
	 * Execute task in background and return task ID.
	 *
	 * <p>This method uses streaming execution to collect progress updates
	 * from the remote agent. The task runs in a background CompletableFuture
	 * and can be queried using the TaskOutput tool.
	 *
	 * @param agentUrl the agent URL
	 * @param params the task parameters including prompt and agent type
	 * @return formatted message with task ID for later retrieval
	 */
	private String executeBackground(String agentUrl, TaskParams params) {
		String taskId = UUID.randomUUID().toString();

		logger.info("Starting background A2A task: {} (id: {})", params.description(), taskId);

		CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
			// Create A2A Message
			Message request = Message.builder()
				.role(Message.Role.USER)
				.parts(List.of(new TextPart(params.prompt())))
				.build();

			List<String> progressUpdates = new ArrayList<>();
			AtomicReference<Message> finalResponse = new AtomicReference<>();
			CountDownLatch latch = new CountDownLatch(1);

			BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
				try {
					if (event instanceof MessageEvent messageEvent) {
						String text = MessageUtils.extractText(messageEvent.getMessage().parts());
						if (text != null) {
							progressUpdates.add(text);
						}
						finalResponse.set(messageEvent.getMessage());
						latch.countDown();
					}
					else if (event instanceof TaskEvent taskEvent) {
						if (isTaskComplete(taskEvent)) {
							Message response = extractMessageFromTaskEvent(taskEvent);
							String text = MessageUtils.extractText(response.parts());
							if (text != null) {
								progressUpdates.add(text);
							}
							finalResponse.set(response);
							latch.countDown();
						}
					}
				}
				catch (Exception e) {
					logger.error("Error processing event", e);
				}
			};

			// Create client with consumer
			Client client = createClient(agentUrl, List.of(consumer));

			// Send message
			client.sendMessage(request);

			try {
				latch.await(defaultTimeout.toSeconds(), TimeUnit.SECONDS);
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}

			return formatResult(params, progressUpdates, finalResponse.get());
		});

		backgroundTasks.put(taskId, future);

		future.whenComplete((result, error) -> {
			if (error != null) {
				logger.error("Background A2A task failed: {}", taskId, error);
			}
			else {
				logger.info("Background A2A task completed: {}", taskId);
			}
		});

		return String.format("""
				## Background Task Started

				Task ID: `%s`
				Description: %s
				Agent Type: %s (remote A2A)

				**To get the result, use the TaskOutput tool:**
				```
				TaskOutputTool(task_id: "%s")
				```

				The task is running in the background. You can continue with other work.
				""", taskId, params.description(), params.subagentType(), taskId);
	}

	/**
	 * Get background task output by ID.
	 * @param taskId the task ID
	 * @return task result or status message
	 */
	public String getTaskOutput(String taskId) {
		CompletableFuture<String> future = backgroundTasks.get(taskId);
		if (future == null) {
			return String.format("## Task Not Found\n\nTask ID `%s` not found.", taskId);
		}

		if (!future.isDone()) {
			return String.format("""
					## Background Task Running

					Task ID: `%s`
					Status: In progress...

					The task has not completed yet. Check again shortly.
					""", taskId);
		}

		try {
			String result = future.get();
			return String.format("""
					## Background Task Completed

					Task ID: `%s`

					%s
					""", taskId, result);
		}
		catch (Exception e) {
			return String.format("""
					## Background Task Failed

					Task ID: `%s`

					**Error:**
					```
					%s
					```
					""", taskId, e.getMessage());
		}
	}

	/**
	 * Create A2A SDK client for the given URL with event consumers.
	 *
	 * <p>Note: Clients cannot be cached because consumers must be registered
	 * at build time. Each invocation needs its own client with its own consumers.
	 */
	private Client createClient(String agentUrl, List<BiConsumer<ClientEvent, AgentCard>> consumers) {
		// Create AgentCard for the URL
		AgentCard agentCard = AgentCard.builder()
			.name("Remote Agent")
			.description("Remote A2A agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.supportedInterfaces(List.of(new io.a2a.spec.AgentInterface("JSONRPC", agentUrl)))
			.build();

		// Configure for synchronous mode
		io.a2a.client.config.ClientConfig clientConfig = new io.a2a.client.config.ClientConfig.Builder()
			.setStreaming(false)
			.setAcceptedOutputModes(List.of("text"))
			.build();

		// Build client with consumers
		return Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(io.a2a.client.transport.jsonrpc.JSONRPCTransport.class,
				new io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig())
			.addConsumers(consumers)
			.build();
	}

	/**
	 * Check if a TaskEvent represents a completed task.
	 */
	private boolean isTaskComplete(TaskEvent taskEvent) {
		TaskState state = taskEvent.getTask().status().state();
		return state == TaskState.COMPLETED || state == TaskState.FAILED || state == TaskState.CANCELED;
	}

	/**
	 * Extract message from TaskEvent artifacts.
	 */
	private Message extractMessageFromTaskEvent(TaskEvent taskEvent) {
		if (taskEvent.getTask().artifacts() == null || taskEvent.getTask().artifacts().isEmpty()) {
			return Message.builder()
				.role(Message.Role.AGENT)
				.parts(List.of(new TextPart("Task completed but no artifacts found")))
				.build();
		}

		// Extract text from first artifact
		List<Part<?>> parts = taskEvent.getTask().artifacts().get(0).parts();
		return Message.builder()
			.role(Message.Role.AGENT)
			.parts(parts)
			.build();
	}

	/**
	 * Format the final result with optional progress updates.
	 */
	private String formatResult(TaskParams params, List<String> progressUpdates, Message finalResponse) {
		if (!params.isIncludeProgress() && finalResponse != null) {
			return MessageUtils.extractText(finalResponse.parts());
		}

		StringBuilder result = new StringBuilder();
		result.append(String.format("## Task Result: %s\n\n", params.description()));
		result.append(String.format("**Agent Type:** %s (remote A2A)\n\n", params.subagentType()));

		if (!progressUpdates.isEmpty() && params.isIncludeProgress()) {
			result.append("**Progress:**\n");
			for (String update : progressUpdates) {
				result.append("- ").append(update).append("\n");
			}
			result.append("\n");
		}

		if (finalResponse != null) {
			result.append(MessageUtils.extractText(finalResponse.parts()));
		}
		else {
			result.append("*Task completed but no response received*");
		}

		return result.toString();
	}

	private String formatError(String message, Exception e) {
		return String.format("""
				## Remote A2A Agent Error

				**Error:** %s

				**Details:**
				```
				%s
				```

				Please check the agent URL and parameters.
				""", message, e.getMessage());
	}

}
