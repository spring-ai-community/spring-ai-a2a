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

package org.springaicommunity.a2a.server.controller;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.springaicommunity.a2a.core.MessageUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for handling synchronous message-based A2A communication.
 *
 * <p>This controller handles the {@code POST /a2a} endpoint for JSON-RPC
 * requests with the {@code sendMessage} method. It provides immediate,
 * synchronous responses using the injected {@link ChatClient}.
 *
 * <p><strong>Usage:</strong>
 * <ol>
 *   <li>Application configures a {@link ChatClient} bean with system prompt</li>
 *   <li>This controller injects the ChatClient and uses it to process messages</li>
 *   <li>No custom code needed - just configure ChatClient</li>
 * </ol>
 *
 * <p><strong>Example Configuration:</strong>
 * <pre>
 * &#64;Bean
 * public ChatClient chatClient(ChatModel chatModel) {
 *     return ChatClient.builder(chatModel)
 *         .defaultSystem("You are a helpful weather assistant...")
 *         .build();
 * }
 * </pre>
 *
 * <p><strong>JSON-RPC Request Example:</strong>
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "sendMessage",
 *   "params": {
 *     "message": {
 *       "role": "user",
 *       "parts": [{"type": "text", "text": "What's the weather in Paris?"}]
 *     }
 *   },
 *   "id": 1
 * }
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@RestController
public class A2AMessageController {

	private final ChatClient chatClient;

	public A2AMessageController(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	/**
	 * Handle JSON-RPC requests for message-based communication.
	 *
	 * <p>Supports the {@code sendMessage} method for synchronous message exchange.
	 * Extracts text from the incoming message, processes it with ChatClient, and
	 * returns the response as an agent message.
	 *
	 * @param requestBody the JSON-RPC request
	 * @return JSON-RPC response with the agent's message
	 */
	@PostMapping(path = "${spring.ai.a2a.server.base-path:/a2a}",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> handleMessage(@RequestBody Map<String, Object> requestBody) {
		try {
			String method = (String) requestBody.get("method");
			Object id = requestBody.get("id");

			// Normalize method name to lowercase for case-insensitive comparison
			String normalizedMethod = method != null ? method.toLowerCase() : "";

			if (!"sendmessage".equals(normalizedMethod)) {
				// This controller only handles sendMessage
				// Other methods (submitTask, getTask, etc.) are handled by A2ATaskController
				Map<String, Object> errorResponse = createErrorResponse(id, -32601,
						"Method not found: " + method);
				return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(errorResponse);
			}

			Map<String, Object> params = (Map<String, Object>) requestBody.get("params");
			Map<String, Object> response = handleSendMessage(params, id);

			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(response);
		}
		catch (Exception e) {
			Map<String, Object> errorResponse = createErrorResponse(requestBody.get("id"), -32603,
					"Internal error: " + e.getMessage());
			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResponse);
		}
	}

	/**
	 * Handle sendMessage JSON-RPC method.
	 */
	private Map<String, Object> handleSendMessage(Map<String, Object> params, Object id) {
		try {
			// Parse the incoming message
			Map<String, Object> messageMap = (Map<String, Object>) params.get("message");
			Message userMessage = parseMessage(messageMap);

			// Extract text from the message
			String userText = MessageUtils.extractText(userMessage);

			// Call ChatClient to get response
			String responseText = chatClient.prompt()
				.user(userText)
				.call()
				.content();

			// Create response message
			Message responseMessage = MessageUtils.assistantMessage(
				List.of(new TextPart(responseText))
			);

			// Build JSON-RPC success response
			Map<String, Object> result = new HashMap<>();
			result.put("message", serializeMessage(responseMessage));

			return createSuccessResponse(id, result);
		}
		catch (Exception e) {
			return createErrorResponse(id, -32603, "Error sending message: " + e.getMessage());
		}
	}

	/**
	 * Parse a message from JSON-RPC params map.
	 */
	private Message parseMessage(Map<String, Object> messageMap) {
		String roleStr = (String) messageMap.get("role");

		// Handle various role name formats (USER, user, ROLE_USER, etc.)
		String normalizedRole = roleStr.toUpperCase();
		if (normalizedRole.startsWith("ROLE_")) {
			normalizedRole = normalizedRole.substring(5); // Strip "ROLE_" prefix
		}

		Message.Role role = Message.Role.valueOf(normalizedRole);

		List<Map<String, Object>> partsList = (List<Map<String, Object>>) messageMap.get("parts");
		List<Part<?>> parts = new ArrayList<>();

		for (Map<String, Object> partMap : partsList) {
			String type = (String) partMap.get("type");
			if ("text".equals(type) || type == null) {
				String text = (String) partMap.get("text");
				if (text != null) {
					parts.add(new TextPart(text));
				}
			}
			// Add support for other part types as needed
		}

		return Message.builder().role(role).parts(parts).build();
	}

	/**
	 * Serialize a message to JSON-RPC response format.
	 */
	private Map<String, Object> serializeMessage(Message message) {
		Map<String, Object> messageMap = new HashMap<>();
		messageMap.put("role", message.role().name());

		List<Map<String, Object>> partsList = new ArrayList<>();
		if (message.parts() != null) {
			for (Part<?> part : message.parts()) {
				if (part instanceof TextPart textPart) {
					Map<String, Object> partMap = new HashMap<>();
					partMap.put("type", "text");
					partMap.put("text", textPart.text());
					partsList.add(partMap);
				}
				// Add serialization for other part types as needed
			}
		}
		messageMap.put("parts", partsList);

		return messageMap;
	}

	/**
	 * Create a JSON-RPC success response.
	 */
	private Map<String, Object> createSuccessResponse(Object id, Map<String, Object> result) {
		Map<String, Object> response = new HashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		response.put("result", result);
		return response;
	}

	/**
	 * Create a JSON-RPC error response.
	 */
	private Map<String, Object> createErrorResponse(Object id, int code, String message) {
		Map<String, Object> error = new HashMap<>();
		error.put("code", code);
		error.put("message", message);

		Map<String, Object> response = new HashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		response.put("error", error);
		return response;
	}

}
