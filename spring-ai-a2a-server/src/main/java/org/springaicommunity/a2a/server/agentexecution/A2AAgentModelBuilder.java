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
import io.a2a.spec.Part;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * Builder interface for creating {@link A2AAgentModel} instances.
 *
 * <p>This builder provides a fluent API for configuring agents with varying levels of
 * customization. Most users will only need to provide a {@link ChatClient} and system prompt,
 * while advanced users can override response generation and lifecycle hooks.
 *
 * <p><strong>Simple Usage (90% of cases):</strong>
 * <pre>
 * A2AAgentModel weatherAgent = A2AAgentModel.builder()
 *     .chatClient(ChatClient.builder(chatModel).build())
 *     .systemPrompt("You are a weather assistant...")
 *     .build();
 * </pre>
 *
 * <p><strong>With Auto-Injected Tools:</strong>
 * <pre>
 * // Tools are auto-injected by Spring when using ChatModel + List&lt;ToolCallback&gt;
 * A2AAgentModel travelAgent = A2AAgentModel.builder()
 *     .chatModel(chatModel)  // ChatModel will auto-wire ToolCallbacks
 *     .tools(toolCallbacks)  // Include MCP tools, FileSystemTools, etc.
 *     .systemPrompt("You are a travel planner with access to file operations...")
 *     .build();
 * </pre>
 *
 * <p><strong>Custom Response Generation:</strong>
 * <pre>
 * A2AAgentModel researchAgent = A2AAgentModel.builder()
 *     .chatClient(ChatClient.builder(chatModel).build())
 *     .systemPrompt("You are a research assistant...")
 *     .responseGenerator(userInput -> {
 *         // Custom pre-processing
 *         List&lt;Document&gt; docs = documentRepo.search(userInput);
 *         String context = formatDocs(docs);
 *
 *         // Call LLM with enriched context
 *         String response = chatClient.prompt()
 *             .system("Research assistant...")
 *             .user("Context: " + context + "\n\nQuery: " + userInput)
 *             .call()
 *             .content();
 *
 *         return List.of(new TextPart(response));
 *     })
 *     .build();
 * </pre>
 *
 * <p><strong>With Lifecycle Hooks:</strong>
 * <pre>
 * A2AAgentModel validatedAgent = A2AAgentModel.builder()
 *     .chatClient(ChatClient.builder(chatModel).build())
 *     .systemPrompt("You are a content generator...")
 *     .beforeComplete((parts, context) -> {
 *         // Validate or transform response before completion
 *         return validateAndTransform(parts);
 *     })
 *     .afterComplete(context -> {
 *         // Log metrics, cleanup resources, etc.
 *         logger.info("Task completed: {}", context.getTask().getId());
 *     })
 *     .onError((error, context) -> {
 *         // Custom error handling
 *         logger.error("Task failed: {}", error.getMessage());
 *         return null;
 *     })
 *     .build();
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AAgentModel
 * @see DefaultA2AAgentModel
 */
public interface A2AAgentModelBuilder {

	/**
	 * Set the ChatClient for LLM interactions.
	 * <p>
	 * <strong>Required:</strong> Either this method or {@link #chatModel(ChatModel, List)}
	 * must be called.
	 * @param chatClient the configured ChatClient (with or without tools)
	 * @return this builder
	 */
	A2AAgentModelBuilder chatClient(ChatClient chatClient);

	/**
	 * Set the ChatModel and tools for LLM interactions.
	 * <p>
	 * This is a convenience method that automatically builds a ChatClient with the provided
	 * tools. This is the recommended approach when using Spring Boot auto-configuration with
	 * auto-injected tools.
	 * <p>
	 * <strong>Required:</strong> Either this method or {@link #chatClient(ChatClient)} must be
	 * called.
	 * @param chatModel the ChatModel for LLM interactions
	 * @param tools the list of tool callbacks (can be auto-injected by Spring)
	 * @return this builder
	 */
	A2AAgentModelBuilder chatModel(ChatModel chatModel, List<ToolCallback> tools);

	/**
	 * Set the system prompt for the agent.
	 * <p>
	 * The system prompt defines the agent's behavior, capabilities, and response format.
	 * <p>
	 * <strong>Required:</strong> This method must be called.
	 * @param systemPrompt the system prompt text
	 * @return this builder
	 */
	A2AAgentModelBuilder systemPrompt(String systemPrompt);

	/**
	 * Set a custom response generator function.
	 * <p>
	 * Override this to implement custom logic for generating responses:
	 * <ul>
	 * <li>Pre-processing user input</li>
	 * <li>Fetching additional context (e.g., from a database)</li>
	 * <li>Multi-step reasoning</li>
	 * <li>Custom LLM interactions</li>
	 * </ul>
	 * <p>
	 * <strong>Optional:</strong> If not provided, the default implementation will use
	 * {@link ChatClient} with the system prompt.
	 * @param responseGenerator function that takes user input and returns response parts
	 * @return this builder
	 */
	A2AAgentModelBuilder responseGenerator(Function<String, List<Part<?>>> responseGenerator);

	/**
	 * Set a pre-completion hook that runs before artifacts are added to the task.
	 * <p>
	 * Use this hook to:
	 * <ul>
	 * <li>Validate response parts</li>
	 * <li>Transform or enrich responses</li>
	 * <li>Add metadata</li>
	 * <li>Perform pre-completion checks</li>
	 * </ul>
	 * <p>
	 * <strong>Optional:</strong> If not provided, response parts pass through unchanged.
	 * @param beforeComplete function that takes parts and context, returns (possibly modified)
	 * parts
	 * @return this builder
	 */
	A2AAgentModelBuilder beforeComplete(BiFunction<List<Part<?>>, RequestContext, List<Part<?>>> beforeComplete);

	/**
	 * Set a post-completion hook that runs after the task is marked as complete.
	 * <p>
	 * Use this hook to:
	 * <ul>
	 * <li>Perform cleanup operations</li>
	 * <li>Log completion metrics</li>
	 * <li>Trigger follow-up actions</li>
	 * <li>Update external systems</li>
	 * </ul>
	 * <p>
	 * <strong>Optional:</strong> If not provided, no post-completion action is taken.
	 * @param afterComplete consumer that takes the request context
	 * @return this builder
	 */
	A2AAgentModelBuilder afterComplete(Consumer<RequestContext> afterComplete);

	/**
	 * Set an error handling hook.
	 * <p>
	 * Use this hook to:
	 * <ul>
	 * <li>Log detailed error information</li>
	 * <li>Transform errors</li>
	 * <li>Trigger alerts or notifications</li>
	 * <li>Implement custom retry logic</li>
	 * </ul>
	 * <p>
	 * <strong>Optional:</strong> If not provided, the default error handling (mark task as
	 * failed) will be used.
	 * <p>
	 * <strong>Note:</strong> The default implementation marks the task as failed via
	 * TaskUpdater. Custom implementations should also ensure task state is updated appropriately.
	 * @param onError function that takes error and context, returns null
	 * @return this builder
	 */
	A2AAgentModelBuilder onError(BiFunction<Exception, RequestContext, Void> onError);

	/**
	 * Build the A2AAgentModel instance.
	 * <p>
	 * <strong>Validation:</strong>
	 * <ul>
	 * <li>Either {@link #chatClient(ChatClient)} or
	 * {@link #chatModel(ChatModel, List)} must be called</li>
	 * <li>{@link #systemPrompt(String)} must be called</li>
	 * </ul>
	 * @return the configured A2AAgentModel instance
	 * @throws IllegalStateException if required fields are not set
	 */
	A2AAgentModel build();

}
