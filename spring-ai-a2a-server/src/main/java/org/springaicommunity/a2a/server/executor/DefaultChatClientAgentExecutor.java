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

package org.springaicommunity.a2a.server.executor;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

/**
 * Default implementation of {@link AgentExecutor} that bridges Spring AI's
 * {@link ChatClient} to the A2A protocol.
 *
 * <p>This implementation handles the most common use case: synchronous ChatClient
 * invocation with text input/output and proper A2A task lifecycle management using
 * {@link TaskUpdater}.
 *
 * <p><strong>Usage:</strong>
 * <p>This executor is automatically configured when a {@link ChatClient} bean is present
 * and no custom {@link AgentExecutor} bean is provided. It extracts text from incoming
 * A2A messages, passes it to the ChatClient, and returns the response as an artifact.
 *
 * <p><strong>Example Application Configuration:</strong>
 * <pre>
 * &#64;Bean
 * public ChatClient myAgent(ChatModel chatModel) {
 *     return ChatClient.builder(chatModel)
 *         .defaultSystem("You are a helpful assistant...")
 *         .build();
 * }
 * </pre>
 *
 * <p><strong>Custom AgentExecutor:</strong>
 * <p>For more complex scenarios (progress tracking, multi-step reasoning, state management),
 * provide a custom {@link AgentExecutor} bean:
 *
 * <pre>
 * &#64;Bean
 * public AgentExecutor customExecutor(ChatClient.Builder builder, MyTools tools) {
 *     ChatClient chatClient = builder.clone()
 *         .defaultSystem("Custom system prompt...")
 *         .defaultTools(tools)
 *         .build();
 *
 *     return new AgentExecutor() {
 *         &#64;Override
 *         public void execute(RequestContext context, EventQueue eventQueue) {
 *             TaskUpdater updater = new TaskUpdater(context, eventQueue);
 *             updater.startWork();
 *
 *             // Send progress updates
 *             updater.addMessage(
 *                 List.of(new TextPart("Working on your request...")),
 *                 "assistant"
 *             );
 *
 *             // Multi-step processing
 *             String response = chatClient.prompt()
 *                 .user(extractText(context.getMessage()))
 *                 .call()
 *                 .content();
 *
 *             updater.addArtifact(List.of(new TextPart(response)));
 *             updater.complete();
 *         }
 *
 *         &#64;Override
 *         public void cancel(RequestContext context, EventQueue eventQueue) {
 *             new TaskUpdater(context, eventQueue).cancel();
 *         }
 *     };
 * }
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see AgentExecutor
 * @see ChatClient
 * @see TaskUpdater
 */
public class DefaultChatClientAgentExecutor implements AgentExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DefaultChatClientAgentExecutor.class);

	private final ChatClient chatClient;

	/**
	 * Create a new DefaultChatClientAgentExecutor.
	 *
	 * @param chatClient the ChatClient to use for processing messages
	 */
	public DefaultChatClientAgentExecutor(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Execute an agent task using the configured ChatClient.
	 *
	 * <p>This method:
	 * <ol>
	 *   <li>Creates a TaskUpdater for lifecycle management</li>
	 *   <li>Marks the task as submitted (if new) and starts work</li>
	 *   <li>Extracts text from the incoming message</li>
	 *   <li>Calls ChatClient to process the message</li>
	 *   <li>Adds the response as an artifact</li>
	 *   <li>Marks the task as complete</li>
	 * </ol>
	 *
	 * @param context the request context containing the message and task information
	 * @param eventQueue the event queue for sending progress updates and results
	 * @throws JSONRPCError if an error occurs during execution
	 */
	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		TaskUpdater updater = new TaskUpdater(context, eventQueue);

		try {
			// Mark task as submitted and start working
			if (context.getTask() == null) {
				updater.submit();
			}
			updater.startWork();

			// Extract text from the message
			String userMessage = extractTextFromMessage(context.getMessage());

			logger.debug("Executing task for user message: {}", userMessage);

			// Call ChatClient to process the message
			String response = this.chatClient.prompt()
				.user(userMessage)
				.call()
				.content();

			logger.debug("AI Response: {}", response);

			// Add response as artifact and complete the task
			updater.addArtifact(
				List.of(new TextPart(response)),
				null, // name
				null, // mimeType
				null  // metadata
			);
			updater.complete();
		}
		catch (Exception e) {
			logger.error("Error executing agent task", e);
			// Let TaskUpdater handle the error
			throw new JSONRPCError(-32603, "Agent execution failed: " + e.getMessage(), null);
		}
	}

	/**
	 * Cancel a running task.
	 *
	 * <p>This implementation simply marks the task as cancelled using TaskUpdater.
	 * Note that ChatClient calls are synchronous and cannot be interrupted mid-execution,
	 * so cancellation takes effect for future operations only.
	 *
	 * @param context the request context for the task to cancel
	 * @param eventQueue the event queue for sending the cancellation event
	 * @throws JSONRPCError if an error occurs during cancellation
	 */
	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		logger.info("Cancelling task: {}", context.getTaskId());
		TaskUpdater updater = new TaskUpdater(context, eventQueue);
		updater.cancel();
	}

	/**
	 * Extract text content from an A2A message.
	 *
	 * <p>Concatenates all {@link TextPart}s in the message into a single string.
	 *
	 * @param message the A2A message
	 * @return the extracted text, or empty string if no text parts found
	 */
	private String extractTextFromMessage(Message message) {
		if (message == null || message.getParts() == null) {
			return "";
		}

		StringBuilder textBuilder = new StringBuilder();
		for (Part<?> part : message.getParts()) {
			if (part instanceof TextPart textPart) {
				textBuilder.append(textPart.getText());
			}
		}
		return textBuilder.toString().trim();
	}

}
