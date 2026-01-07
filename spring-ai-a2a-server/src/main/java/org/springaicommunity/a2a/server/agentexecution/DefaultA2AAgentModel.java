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

package org.springaicommunity.a2a.server.agentexecution;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskState;
import reactor.core.publisher.Flux;

import org.springaicommunity.a2a.core.MessageUtils;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Default concrete implementation of {@link A2AAgentModel} using builder-provided functions.
 *
 * <p>This class serves as the standard implementation for creating A2A agents. Unlike
 * {@code DefaultSpringAIAgentExecutor} (which was abstract and required subclassing),
 * this class is concrete and configured entirely via builder pattern with functional interfaces.
 *
 * <p><strong>Design Philosophy:</strong>
 * <ul>
 * <li><strong>Composition over Inheritance</strong>: Use builder with functions instead of subclassing</li>
 * <li><strong>90% Zero-Config</strong>: Simple agents need only ChatClient + system prompt</li>
 * <li><strong>10% Custom Logic</strong>: Override response generation or lifecycle hooks as needed</li>
 * </ul>
 *
 * <p><strong>Simple Usage (90% of cases):</strong>
 * <pre>
 * A2AAgentModel weatherAgent = A2AAgentModel.builder()
 *     .chatClient(ChatClient.builder(chatModel).build())
 *     .systemPrompt("You are a weather assistant...")
 *     .build();
 * </pre>
 *
 * <p><strong>Custom Logic (10% of cases):</strong>
 * <pre>
 * A2AAgentModel researchAgent = A2AAgentModel.builder()
 *     .chatClient(ChatClient.builder(chatModel).build())
 *     .systemPrompt("You are a research assistant...")
 *     .responseGenerator(userInput -> {
 *         // Custom logic: fetch documents, enrich context, etc.
 *         List&lt;Document&gt; docs = documentRepo.search(userInput);
 *         String context = formatDocs(docs);
 *         String response = chatClient.prompt()
 *             .system("Research assistant...")
 *             .user("Context: " + context + "\n\nQuery: " + userInput)
 *             .call()
 *             .content();
 *         return List.of(new TextPart(response));
 *     })
 *     .beforeComplete((parts, ctx) -> {
 *         // Validation or transformation
 *         return parts;
 *     })
 *     .build();
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AAgentModel
 * @see A2AAgentModelBuilder
 */
public final class DefaultA2AAgentModel implements A2AAgentModel {

	private final ChatClient chatClient;

	private final String systemPrompt;

	// Builder-provided functions (nullable - use defaults if not provided)
	private final Function<String, List<Part<?>>> responseGenerator;

	private final BiFunction<List<Part<?>>, RequestContext, List<Part<?>>> beforeCompleteHook;

	private final Consumer<RequestContext> afterCompleteHook;

	private final BiFunction<Exception, RequestContext, Void> onErrorHook;

	// Thread-local storage for execution context
	private RequestContext currentContext;

	private TaskUpdater currentTaskUpdater;

	/**
	 * Package-private constructor used by builder.
	 * @param chatClient the ChatClient for LLM interactions
	 * @param systemPrompt the system prompt
	 * @param responseGenerator optional custom response generator
	 * @param beforeCompleteHook optional pre-completion hook
	 * @param afterCompleteHook optional post-completion hook
	 * @param onErrorHook optional error handling hook
	 */
	DefaultA2AAgentModel(ChatClient chatClient, String systemPrompt,
			Function<String, List<Part<?>>> responseGenerator,
			BiFunction<List<Part<?>>, RequestContext, List<Part<?>>> beforeCompleteHook,
			Consumer<RequestContext> afterCompleteHook, BiFunction<Exception, RequestContext, Void> onErrorHook) {
		this.chatClient = chatClient;
		this.systemPrompt = systemPrompt;
		this.responseGenerator = responseGenerator;
		this.beforeCompleteHook = beforeCompleteHook;
		this.afterCompleteHook = afterCompleteHook;
		this.onErrorHook = onErrorHook;
	}

	@Override
	public ChatClient getChatClient() {
		return this.chatClient;
	}

	@Override
	public String getSystemPrompt() {
		return this.systemPrompt;
	}

	/**
	 * Get the current RequestContext during execution.
	 * <p>
	 * Available during {@link #generateResponse(String)} execution. Provides access to:
	 * <ul>
	 * <li>contextId - conversation context identifier</li>
	 * <li>taskId - specific task identifier</li>
	 * <li>Task metadata and state</li>
	 * </ul>
	 * @return the current request context, or null if not in execution
	 */
	public RequestContext getRequestContext() {
		return this.currentContext;
	}

	/**
	 * Get the TaskUpdater for sending progress updates during execution.
	 * <p>
	 * Available during {@link #generateResponse(String)} execution. Enables:
	 * <ul>
	 * <li>Sending progress updates: {@code getTaskUpdater().sendStatus(TaskState.RUNNING,
	 * "Processing...")}</li>
	 * <li>Adding intermediate artifacts during processing</li>
	 * <li>Updating task metadata</li>
	 * </ul>
	 * @return the task updater, or null if not in execution
	 */
	public TaskUpdater getTaskUpdater() {
		return this.currentTaskUpdater;
	}

	// ==================== Override lifecycle hooks if custom hooks provided
	// ====================

	@Override
	public List<Part<?>> generateResponse(String userInput) throws Exception {
		// Use custom response generator if provided, otherwise use default
		if (this.responseGenerator != null) {
			return this.responseGenerator.apply(userInput);
		}
		// Default implementation from interface
		return A2AAgentModel.super.generateResponse(userInput);
	}

	@Override
	public List<Part<?>> beforeComplete(List<Part<?>> parts, RequestContext context) throws Exception {
		// Use custom hook if provided
		if (this.beforeCompleteHook != null) {
			return this.beforeCompleteHook.apply(parts, context);
		}
		// Default implementation from interface
		return A2AAgentModel.super.beforeComplete(parts, context);
	}

	@Override
	public void afterComplete(RequestContext context) {
		// Use custom hook if provided
		if (this.afterCompleteHook != null) {
			this.afterCompleteHook.accept(context);
		}
		// Default implementation from interface
		A2AAgentModel.super.afterComplete(context);
	}

	@Override
	public void onError(Exception error, RequestContext context, TaskUpdater taskUpdater) {
		// Use custom hook if provided
		if (this.onErrorHook != null) {
			this.onErrorHook.apply(error, context);
		}
		// Default implementation from interface
		A2AAgentModel.super.onError(error, context, taskUpdater);
	}

	// ==================== A2A SDK AgentExecutor Implementation ====================

	/**
	 * Execute the agent using the A2A SDK interface.
	 * <p>
	 * This method implements the full agent execution lifecycle:
	 * <ol>
	 * <li>Creating a {@link TaskUpdater} from the context and event queue</li>
	 * <li>Managing task lifecycle (submit, start, complete)</li>
	 * <li>Delegating to {@link #generateResponse(String)} for response generation</li>
	 * <li>Calling {@link #beforeComplete(List, RequestContext)} for pre-completion
	 * processing</li>
	 * <li>Converting results to A2A artifacts</li>
	 * <li>Marking task as complete</li>
	 * <li>Calling {@link #afterComplete(RequestContext)} for post-completion cleanup</li>
	 * <li>Handling errors via {@link #onError(Exception, RequestContext, TaskUpdater)}</li>
	 * </ol>
	 * @param context the request context from A2A SDK
	 * @param eventQueue the event queue for publishing events
	 * @throws JSONRPCError if execution fails
	 */
	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		// Store context and taskUpdater for access during generateResponse()
		this.currentContext = context;
		this.currentTaskUpdater = new TaskUpdater(context, eventQueue);

		try {
			// Submit and start task if it's a new task
			if (context.getTask() == null) {
				this.currentTaskUpdater.submit();
			}
			this.currentTaskUpdater.startWork();

			// Extract user input from the request
			String userInput = context.getUserInput(" ");

			// Generate response - can be customized via builder
			List<Part<?>> responseParts = this.generateResponse(userInput);

			// Call beforeComplete hook - allows modification/validation of parts
			responseParts = this.beforeComplete(responseParts, context);

			// Add response as artifacts
			if (responseParts != null && !responseParts.isEmpty()) {
				this.currentTaskUpdater.addArtifact(responseParts);
			}

			// Complete the task
			this.currentTaskUpdater.complete();

			// Call afterComplete hook for cleanup/logging
			this.afterComplete(context);
		}
		catch (Exception e) {
			this.onError(e, context, this.currentTaskUpdater);
		}
		finally {
			// Clear execution context
			this.currentContext = null;
			this.currentTaskUpdater = null;
		}
	}

	/**
	 * Cancel the agent execution using the A2A SDK interface.
	 * @param context the request context from A2A SDK
	 * @param eventQueue the event queue for publishing events
	 * @throws JSONRPCError if cancellation fails
	 */
	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		TaskUpdater taskUpdater = new TaskUpdater(context, eventQueue);
		this.onCancel(context, taskUpdater);
	}

	// ==================== Synchronous and Streaming Execution ====================

	/**
	 * Executes the agent synchronously and returns the response.
	 * <p>
	 * This method provides synchronous execution support by reusing the streaming
	 * implementation. It:
	 * <ul>
	 * <li>Delegates to {@link #executeStreaming(Message)}</li>
	 * <li>Collects all streaming responses into a single response</li>
	 * <li>Blocks until execution completes</li>
	 * <li>Returns the collected response synchronously</li>
	 * </ul>
	 * @param request the A2A message request
	 * @return the A2A message response
	 */
	@Override
	public Message executeSynchronous(Message request) {
		try {
			// Reuse streaming execution and block to collect all responses
			List<Part<?>> allParts = executeStreaming(request)
				.flatMapIterable(message -> message.parts() != null ? message.parts() : List.of())
				.collectList()
				.block();

			return MessageUtils.assistantMessage(allParts != null ? allParts : List.of());
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to execute agent", e);
		}
	}

	/**
	 * Executes the agent asynchronously and returns a stream of responses.
	 * <p>
	 * This method provides streaming execution support using the extracted
	 * {@link A2AStreamingEventQueue} utility. It:
	 * <ul>
	 * <li>Creates a streaming EventQueue that converts events to Flux</li>
	 * <li>Invokes the execute method via A2A SDK interface</li>
	 * <li>Streams artifacts as they are emitted</li>
	 * <li>Returns a Flux of Message objects</li>
	 * </ul>
	 * @param request the A2A message request
	 * @return a Flux of A2A message responses emitted incrementally
	 */
	@Override
	public Flux<Message> executeStreaming(Message request) {
		return Flux.create(sink -> {
			try {
				// Create a streaming event queue that emits events to the Flux
				A2AStreamingEventQueue eventQueue = new A2AStreamingEventQueue(sink);

				// Build request context from Message
				RequestContext context = buildRequestContext(request);

				// Execute the agent asynchronously
				this.execute(context, eventQueue);

				// The sink will be completed by the A2AStreamingEventQueue when task
				// completes
			}
			catch (Exception e) {
				sink.error(e);
			}
		});
	}

	/**
	 * Builds a RequestContext from a Message.
	 * <p>
	 * Helper method to avoid code duplication between synchronous and streaming execution
	 * paths.
	 * @param message the A2A message request
	 * @return the RequestContext
	 */
	private RequestContext buildRequestContext(Message message) {
		RequestContext.Builder contextBuilder = new RequestContext.Builder();

		// Create MessageSendParams from the message (contextId/taskId already in Message)
		contextBuilder.setParams(new io.a2a.spec.MessageSendParams(message, null, null));

		return contextBuilder.build();
	}

}
