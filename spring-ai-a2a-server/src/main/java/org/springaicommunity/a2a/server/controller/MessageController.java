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

import io.a2a.server.ServerCallContext;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.EventKind;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * REST controller for handling A2A message sending.
 *
 * <p>This controller implements the A2A protocol endpoint for synchronous
 * message execution. It acts as a thin adapter between REST and the A2A SDK's
 * {@link RequestHandler}.
 *
 * <p><strong>Endpoint:</strong>
 * <ul>
 *   <li>{@code POST ${spring.ai.a2a.server.base-path:/a2a}} - Send a message to the agent</li>
 * </ul>
 *
 * <p><strong>Request Format:</strong>
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "sendMessage",
 *   "params": {
 *     "message": {
 *       "role": "user",
 *       "parts": [{"type": "text", "text": "Hello agent"}],
 *       "messageId": "msg-001"
 *     }
 *   },
 *   "id": 1
 * }
 * </pre>
 *
 * <p><strong>Response Format:</strong>
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": 1,
 *   "result": {
 *     "message": {
 *       "role": "agent",
 *       "parts": [{"type": "text", "text": "Hello user"}]
 *     }
 *   }
 * }
 * </pre>
 *
 * <p><strong>Architecture:</strong>
 * <pre>
 * REST Request → MessageController → RequestHandler → AgentExecutor → ChatClient
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 * @see RequestHandler
 * @see io.a2a.server.agentexecution.AgentExecutor
 */
@RestController
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

	private final RequestHandler requestHandler;

	/**
	 * Create a new MessageController.
	 *
	 * @param requestHandler the A2A SDK request handler
	 */
	public MessageController(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	/**
	 * Handle sendMessage JSON-RPC requests.
	 *
	 * <p>This method:
	 * <ol>
	 *   <li>Receives the JSON-RPC request with A2A SDK types</li>
	 *   <li>Creates a ServerCallContext (for auth, state, extensions)</li>
	 *   <li>Delegates to RequestHandler which orchestrates the agent execution</li>
	 *   <li>Returns the JSON-RPC response with the result</li>
	 * </ol>
	 *
	 * <p>The RequestHandler handles all protocol details including:
	 * <ul>
	 *   <li>Task lifecycle management</li>
	 *   <li>AgentExecutor invocation</li>
	 *   <li>Event queue coordination</li>
	 *   <li>Error handling and JSON-RPC error responses</li>
	 * </ul>
	 *
	 * @param request the sendMessage request
	 * @return the sendMessage response
	 * @throws JSONRPCError if the request fails
	 */
	@PostMapping(
		path = "${spring.ai.a2a.server.base-path:/a2a}",
		consumes = MediaType.APPLICATION_JSON_VALUE,
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public SendMessageResponse sendMessage(@RequestBody SendMessageRequest request) throws JSONRPCError {

		MessageSendParams params = request.getParams();
		logger.info("Received sendMessage request - id: {}", request.getId());

		try {
			// Create server call context
			// TODO: Add support for auth context, state, and extensions
			ServerCallContext context = new ServerCallContext(
				null,      // auth context (not used yet)
				Map.of(),  // state
				Set.of()   // extensions
			);

			// Delegate to SDK's RequestHandler - handles all protocol logic
			EventKind result = requestHandler.onMessageSend(params, context);

			logger.info("Message processed successfully - id: {}", request.getId());
			return new SendMessageResponse(request.getId(), result);
		}
		catch (JSONRPCError e) {
			logger.error("Error processing message - id: {}", request.getId(), e);
			throw e;
		}
		catch (Exception e) {
			logger.error("Unexpected error processing message - id: {}", request.getId(), e);
			throw new JSONRPCError(-32603, "Internal error: " + e.getMessage(), null);
		}
	}

}
