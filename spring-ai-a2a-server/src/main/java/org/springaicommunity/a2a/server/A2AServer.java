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

package org.springaicommunity.a2a.server;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import io.a2a.spec.AgentCard;

import org.springaicommunity.a2a.core.A2AEndpoint;

/**
 * Interface for A2A (Agent-to-Agent) server implementations.
 *
 * <p>
 * This interface defines the contract for A2A server components that expose agent
 * functionality via A2A protocol endpoints. It extends {@link A2AEndpoint} to provide agent
 * metadata access and adds server-specific HTTP endpoint methods.
 *
 * <p>
 * Implementations of this interface handle:
 * <ul>
 * <li>Exposing A2A protocol endpoints (JSON-RPC, HTTP, etc.)</li>
 * <li>Agent card discovery via RFC 8615 well-known URIs</li>
 * <li>Request routing to agent executors</li>
 * <li>Task lifecycle management</li>
 * <li>Protocol-level error handling</li>
 * </ul>
 *
 * <p>
 * The server acts as a Spring MVC {@code @RestController}, automatically providing
 * endpoints for:
 * <ul>
 * <li>{@code GET /.well-known/agent-card.json} - Agent card discovery via
 * {@link #getWellKnownAgentCard()}</li>
 * <li>{@code POST /a2a} - JSON-RPC request handling via
 * {@link #handleJsonRpcRequest(Map)}</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AEndpoint
 */
public interface A2AServer extends A2AEndpoint {

	/**
	 * Returns the agent card via the RFC 8615 well-known URI discovery endpoint.
	 * <p>
	 * This endpoint follows the A2A protocol specification for agent card discovery using
	 * the well-known URI pattern defined in RFC 8615. Implementations may choose to
	 * dynamically populate the supportedInterfaces based on runtime configuration or
	 * return a static agent card.
	 * @return the agent card as JSON
	 */
	AgentCard getWellKnownAgentCard();

	/**
	 * Handles A2A JSON-RPC requests.
	 * <p>
	 * This method processes JSON-RPC 2.0 requests according to the A2A protocol
	 * specification. It supports operations such as:
	 * <ul>
	 * <li>submitTask - Create a new task</li>
	 * <li>sendMessage - Send a message to the agent</li>
	 * <li>getTask - Retrieve task status and results</li>
	 * </ul>
	 * @param requestBody the JSON-RPC request
	 * @return the JSON-RPC response
	 */
	ResponseEntity<Map<String, Object>> handleJsonRpcRequest(Map<String, Object> requestBody);

}
