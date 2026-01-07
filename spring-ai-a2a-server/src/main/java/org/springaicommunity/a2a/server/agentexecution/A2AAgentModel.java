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
import java.util.Map;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import reactor.core.publisher.Flux;

import org.springaicommunity.agents.model.AgentGeneration;
import org.springaicommunity.agents.model.AgentGenerationMetadata;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.AgentResponse;
import org.springaicommunity.agents.model.AgentResponseMetadata;
import org.springaicommunity.agents.model.AgentTaskRequest;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Agent interface that bridges Spring AI's {@link AgentModel} with A2A protocol execution.
 *
 * <p>This interface serves as a multi-layer adapter:
 * <ul>
 * <li><strong>Spring AI AgentModel</strong>: Task-oriented {@link #call(AgentTaskRequest)}</li>
 * <li><strong>A2A SDK AgentExecutor</strong>: Protocol-level {@code execute(RequestContext, EventQueue)}</li>
 * <li><strong>Lifecycle Management</strong>: Optional hooks for customization</li>
 * </ul>
 *
 * <p><strong>Simple Usage (90% of cases):</strong>
 * <pre>
 * &#64;Bean
 * public A2AAgentModel weatherAgent(ChatModel chatModel) {
 *     return A2AAgentModel.builder()
 *         .chatClient(ChatClient.builder(chatModel).build())
 *         .systemPrompt("You are a weather assistant...")
 *         .build();
 * }
 * </pre>
 *
 * <p><strong>Custom Logic (10% of cases):</strong>
 * <pre>
 * &#64;Bean
 * public A2AAgentModel researchAgent(ChatModel chatModel, DocumentRepo repo) {
 *     return A2AAgentModel.builder()
 *         .chatClient(ChatClient.builder(chatModel).build())
 *         .systemPrompt("You are a research assistant...")
 *         .responseGenerator(userInput -> {
 *             // Custom logic
 *             List&lt;Document&gt; docs = repo.search(userInput);
 *             String context = formatDocs(docs);
 *             String response = chatClient.prompt()
 *                 .system("Research assistant...")
 *                 .user("Context: " + context + "\n\nQuery: " + userInput)
 *                 .call()
 *                 .content();
 *             return List.of(new TextPart(response));
 *         })
 *         .beforeComplete((parts, ctx) -> {
 *             // Validation or transformation
 *             return parts;
 *         })
 *         .build();
 * }
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see AgentModel
 * @see AgentExecutor
 * @see DefaultA2AAgentModel
 */
public interface A2AAgentModel extends AgentModel, AgentExecutor {

	/**
	 * Create a new builder for constructing A2AAgentModel instances.
	 * @return a new builder instance
	 */
	static A2AAgentModelBuilder builder() {
		return new DefaultA2AAgentModelBuilder();
	}

	/**
	 * Get the ChatClient instance used by this agent.
	 * <p>
	 * The ChatClient provides access to the underlying LLM and registered tools.
	 * Available for use within custom {@link #generateResponse(String)} implementations.
	 * @return the ChatClient instance
	 */
	ChatClient getChatClient();

	/**
	 * Get the system prompt for this agent.
	 * <p>
	 * The system prompt defines the agent's behavior and capabilities. It should describe:
	 * <ul>
	 * <li>What the agent does</li>
	 * <li>What tools/capabilities it has access to</li>
	 * <li>How it should format responses</li>
	 * </ul>
	 * @return the system prompt
	 */
	String getSystemPrompt();

	/**
	 * Generate response parts for the given user input.
	 * <p>
	 * This is the <strong>PRIMARY extension point</strong>. Override this method to:
	 * <ul>
	 * <li>Add pre/post-processing logic</li>
	 * <li>Integrate with external APIs or databases</li>
	 * <li>Implement multi-step reasoning</li>
	 * <li>Add validation or transformation</li>
	 * </ul>
	 * <p>
	 * <strong>Default implementation</strong> uses {@link #getChatClient()} with
	 * {@link #getSystemPrompt()} to generate a response. The ChatClient automatically
	 * handles tool calling during execution.
	 * @param userInput the user's input extracted from the request
	 * @return response parts (e.g., TextPart, ImagePart, DataPart)
	 * @throws Exception if response generation fails
	 */
	default List<Part<?>> generateResponse(String userInput) throws Exception {
		String systemPrompt = getSystemPrompt();
		String response = getChatClient().prompt().system(systemPrompt).user(userInput).call().content();
		return List.of(new TextPart(response));
	}

	/**
	 * Execute an agent task using the Spring AI AgentModel pattern.
	 * <p>
	 * This method implements the {@link AgentModel#call(AgentTaskRequest)} interface by
	 * delegating to {@link #generateResponse(String)}.
	 * @param request the agent task request containing goal and context
	 * @return the agent response with results
	 */
	@Override
	default AgentResponse call(AgentTaskRequest request) {
		try {
			String goal = request.goal();
			List<Part<?>> responseParts = generateResponse(goal);
			String responseText = org.springaicommunity.a2a.core.MessageUtils.extractText(responseParts);

			AgentGeneration generation = new AgentGeneration(responseText);
			return AgentResponse.builder()
				.results(List.of(generation))
				.metadata(new AgentResponseMetadata())
				.build();
		}
		catch (Exception e) {
			AgentGeneration errorGeneration = new AgentGeneration("Error: " + e.getMessage(),
					new AgentGenerationMetadata("ERROR", Map.of("error", e.getMessage())));
			return AgentResponse.builder()
				.results(List.of(errorGeneration))
				.metadata(new AgentResponseMetadata())
				.build();
		}
	}

	/**
	 * Check if this agent is available and ready to process tasks.
	 * @return true if the agent is available, false otherwise
	 */
	@Override
	default boolean isAvailable() {
		return true;
	}

	// ==================== Lifecycle Hooks ====================

	/**
	 * Called after execution completes but before artifacts are added to the task.
	 * <p>
	 * Override this method to:
	 * <ul>
	 * <li>Validate or modify response parts</li>
	 * <li>Add custom metadata or transformations</li>
	 * <li>Perform pre-completion checks</li>
	 * </ul>
	 * <p>
	 * Default implementation returns the parts unchanged.
	 * @param parts the response parts from execution
	 * @param context the request context
	 * @return the potentially modified response parts
	 * @throws Exception if pre-completion processing fails
	 */
	default List<Part<?>> beforeComplete(List<Part<?>> parts, RequestContext context) throws Exception {
		return parts;
	}

	/**
	 * Called after the task has been marked as completed.
	 * <p>
	 * Override this method to:
	 * <ul>
	 * <li>Perform cleanup operations</li>
	 * <li>Log completion metrics or analytics</li>
	 * <li>Trigger follow-up actions</li>
	 * </ul>
	 * <p>
	 * Default implementation does nothing.
	 * @param context the request context
	 */
	default void afterComplete(RequestContext context) {
		// Default: no-op
	}

	/**
	 * Handle task cancellation.
	 * <p>
	 * Default implementation cancels the task via TaskUpdater. Override for custom logic.
	 * @param context the request context
	 * @param taskUpdater the task updater for managing task state
	 * @throws JSONRPCError if cancellation fails
	 */
	default void onCancel(RequestContext context, TaskUpdater taskUpdater) throws JSONRPCError {
		taskUpdater.cancel();
	}

	/**
	 * Handle execution errors.
	 * <p>
	 * Default implementation fails the task via TaskUpdater. Override for custom error handling.
	 * @param error the exception that occurred
	 * @param context the request context
	 * @param taskUpdater the task updater for managing task state
	 */
	default void onError(Exception error, RequestContext context, TaskUpdater taskUpdater) {
		taskUpdater.fail();
	}

	// ==================== Execution Methods ====================

	/**
	 * Executes the agent synchronously and returns the response.
	 * <p>
	 * Implementation provided by {@link DefaultA2AAgentModel}.
	 * @param request the A2A SDK message request
	 * @return the A2A SDK message response
	 */
	Message executeSynchronous(Message request);

	/**
	 * Executes the agent asynchronously and returns a stream of responses.
	 * <p>
	 * Default implementation delegates to {@link #executeSynchronous(Message)}.
	 * Override for true streaming support.
	 * @param request the A2A SDK message request
	 * @return a Flux of A2A SDK message responses
	 */
	default Flux<Message> executeStreaming(Message request) {
		return Flux.just(executeSynchronous(request));
	}

}
