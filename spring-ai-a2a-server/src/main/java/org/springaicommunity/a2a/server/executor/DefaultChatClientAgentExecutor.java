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
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springaicommunity.a2a.server.util.A2AContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default AgentExecutor implementation using ChatClientExecutor pattern.
 * Bridges Spring AI ChatClient to A2A protocol with pluggable execution logic.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public class DefaultChatClientAgentExecutor implements AgentExecutor {

	private static final Logger logger = LoggerFactory.getLogger(DefaultChatClientAgentExecutor.class);

	private static final ChatClientExecutor DEFAULT_EXECUTOR = (chatClient, context) -> chatClient.prompt()
			.user(A2AContext.getUserMessage(context))
			.toolContext(context)
			.call()
			.content();

	private final ChatClient chatClient;

	private final ChatClientExecutor executor;

	/**
	 * Create executor with ChatClient (uses default execution logic).
	 */
	public DefaultChatClientAgentExecutor(ChatClient chatClient) {
		this(chatClient, DEFAULT_EXECUTOR);
	}

	/**
	 * Create executor with custom execution logic.
	 */
	public DefaultChatClientAgentExecutor(ChatClient chatClient, ChatClientExecutor executor) {
		this.chatClient = chatClient;
		this.executor = executor;
	}

	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		TaskUpdater updater = new TaskUpdater(context, eventQueue);

		try {
			if (context.getTask() == null) {
				updater.submit();
			}
			updater.startWork();

			// Build execution context from A2A RequestContext
			Map<String, Object> executionContext = buildExecutionContext(context);

			logger.debug("Executing task for user message: {}", A2AContext.getUserMessage(executionContext));

			// Execute using ChatClientExecutor
			String response = executor.execute(chatClient, executionContext);

			logger.debug("AI Response: {}", response);

			updater.addArtifact(List.of(new TextPart(response)), null, null, null);
			updater.complete();
		}
		catch (Exception e) {
			logger.error("Error executing agent task", e);
			throw new JSONRPCError(-32603, "Agent execution failed: " + e.getMessage(), null);
		}
	}

	private Map<String, Object> buildExecutionContext(RequestContext context) {
		Map<String, Object> executionContext = new HashMap<>();
		executionContext.put(A2AContext.USER_MESSAGE, A2AContext.extractTextFromMessage(context.getMessage()));
		executionContext.put(A2AContext.TASK_ID, context.getTaskId());
		executionContext.put(A2AContext.CONTEXT_ID, context.getContextId());
		executionContext.put(A2AContext.REQUEST_CONTEXT, context);
		executionContext.put(A2AContext.MESSAGE_PARTS, context.getMessage().getParts());
		return executionContext;
	}

	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		logger.info("Cancelling task: {}", context.getTaskId());
		TaskUpdater updater = new TaskUpdater(context, eventQueue);
		updater.cancel();
	}

}
