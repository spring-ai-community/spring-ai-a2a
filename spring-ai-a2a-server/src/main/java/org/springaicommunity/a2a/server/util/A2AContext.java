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

package org.springaicommunity.a2a.server.util;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities for accessing A2A context data from Map.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public final class A2AContext {

	/** Context key for user message text. */
	public static final String USER_MESSAGE = "userMessage";

	/** Context key for A2A task ID. */
	public static final String TASK_ID = "taskId";

	/** Context key for A2A context ID. */
	public static final String CONTEXT_ID = "contextId";

	/** Context key for full A2A RequestContext. */
	public static final String REQUEST_CONTEXT = "requestContext";

	/** Context key for message parts. */
	public static final String MESSAGE_PARTS = "messageParts";

	private A2AContext() {
	}

	public static String getUserMessage(Map<String, Object> context) {
		return (String) context.getOrDefault(USER_MESSAGE, "");
	}

	public static String getTaskId(Map<String, Object> context) {
		return (String) context.get(TASK_ID);
	}

	public static String getContextId(Map<String, Object> context) {
		return (String) context.get(CONTEXT_ID);
	}

	public static RequestContext getRequestContext(Map<String, Object> context) {
		return (RequestContext) context.get(REQUEST_CONTEXT);
	}

	public static boolean isA2AContext(Map<String, Object> context) {
		return context.containsKey(REQUEST_CONTEXT);
	}

	@SuppressWarnings("unchecked")
	public static List<Part<?>> getMessageParts(Map<String, Object> context) {
		return (List<Part<?>>) context.getOrDefault(MESSAGE_PARTS, List.of());
	}

	/**
	 * Create tool context for Spring AI (contains only A2A metadata).
	 */
	public static Map<String, Object> toToolContext(Map<String, Object> context) {
		Map<String, Object> toolContext = new HashMap<>();
		if (context.containsKey(TASK_ID)) {
			toolContext.put(TASK_ID, context.get(TASK_ID));
		}
		if (context.containsKey(CONTEXT_ID)) {
			toolContext.put(CONTEXT_ID, context.get(CONTEXT_ID));
		}
		return Collections.unmodifiableMap(toolContext);
	}

	/**
	 * Extract text from A2A message parts.
	 */
	public static String extractTextFromMessage(Message message) {
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
