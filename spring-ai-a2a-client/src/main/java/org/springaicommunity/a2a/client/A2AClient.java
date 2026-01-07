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

package org.springaicommunity.a2a.client;

import org.springaicommunity.a2a.core.A2AEndpoint;
import org.springaicommunity.agents.model.AgentModel;
import org.springaicommunity.agents.model.StreamingAgentModel;

/**
 * Interface for A2A (Agent-to-Agent) client implementations following the AgentModel pattern.
 *
 * <p>
 * This interface defines the contract for communicating with remote A2A agents using
 * Spring AI-aligned task execution patterns. It extends {@link A2AEndpoint} for
 * agent metadata access, {@link AgentModel} for blocking execution, and
 * {@link StreamingAgentModel} for streaming execution.
 *
 * <p>
 * Implementations of this interface handle:
 * <ul>
 * <li>Agent discovery and card retrieval</li>
 * <li>Blocking task execution via {@link AgentModel#call(org.springaicommunity.agents.model.AgentTaskRequest)}</li>
 * <li>Streaming execution with progress tracking via {@link StreamingAgentModel#stream(org.springaicommunity.agents.model.AgentTaskRequest)}</li>
 * <li>Protocol-level communication (JSON-RPC, HTTP, etc.)</li>
 * </ul>
 *
 * <p>
 * <strong>Execution Patterns:</strong>
 *
 * <p>
 * <strong>Blocking Execution</strong> ({@link AgentModel#call(org.springaicommunity.agents.model.AgentTaskRequest)}):
 * <ul>
 * <li>Synchronous, blocking API</li>
 * <li>Returns {@link org.springaicommunity.agents.model.AgentResponse} when complete</li>
 * <li>Best for simple queries that complete quickly</li>
 * <li>Follows Spring AI conventions</li>
 * </ul>
 *
 * <p>
 * <strong>Streaming Execution</strong> ({@link StreamingAgentModel#stream(org.springaicommunity.agents.model.AgentTaskRequest)}):
 * <ul>
 * <li>Asynchronous, reactive API using Flux</li>
 * <li>Emits {@link org.springaicommunity.agents.model.AgentResponse} chunks with progress updates</li>
 * <li>Best for long-running tasks requiring progress feedback</li>
 * <li>Supports task status tracking</li>
 * </ul>
 *
 * <p>
 * <strong>Example - Blocking Execution:</strong>
 *
 * <pre class="code">
 * // Create a client for a remote agent
 * A2AClient weatherAgent = DefaultA2AClient.builder()
 *     .agentUrl("http://localhost:10001/a2a")
 *     .build();
 *
 * // Blocking execution
 * AgentResponse response = weatherAgent.call(
 *     AgentTaskRequest.of("What's the weather in San Francisco?")
 * );
 * System.out.println(response.getText());
 *
 * // Access agent metadata
 * AgentCard card = weatherAgent.getAgentCard();
 * System.out.println("Connected to: " + card.name());
 * </pre>
 *
 * <p>
 * <strong>Example - Streaming with Progress:</strong>
 *
 * <pre class="code">
 * // Streaming execution for long-running tasks
 * Flux&lt;AgentResponse&gt; stream = weatherAgent.stream(
 *     AgentTaskRequest.of("Analyze weather patterns for the past year")
 * );
 *
 * stream.subscribe(response -&gt; {
 *     System.out.println("Response: " + response.getText());
 * });
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AEndpoint
 * @see AgentModel
 * @see StreamingAgentModel
 * @see org.springaicommunity.agents.model.AgentResponse
 */
public interface A2AClient extends AgentModel, StreamingAgentModel, A2AEndpoint {

	// Note: call() and stream() methods are inherited from AgentModel and StreamingAgentModel

	/**
	 * Check if this agent client is available and ready to process tasks.
	 *
	 * <p>Default implementation checks if the agent card was successfully retrieved.
	 * Implementations can override to provide more sophisticated availability checks
	 * (e.g., health checks, connectivity tests).
	 *
	 * @return true if the agent is available, false otherwise
	 */
	@Override
	default boolean isAvailable() {
		return getAgentCard() != null;
	}

}
