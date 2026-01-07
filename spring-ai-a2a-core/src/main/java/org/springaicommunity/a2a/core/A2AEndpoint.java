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

package org.springaicommunity.a2a.core;

import io.a2a.spec.AgentCard;
import org.springaicommunity.a2a.core.AgentCards;

/**
 * Base interface for all A2A (Agent-to-Agent) agents.
 *
 * <p>
 * This interface provides a unified abstraction for both local and remote A2A agents.
 * Implementations can represent:
 * <ul>
 * <li>Local agents (server-side) - Agents running in the current application</li>
 * <li>Remote agents ({@link org.springaicommunity.a2a.client.A2AClient}) - Agents running in other services</li>
 * </ul>
 *
 * <p>
 * This interface provides metadata access to agent capabilities. For message exchange
 * with remote agents, use {@link org.springaicommunity.a2a.client.A2AClient}.
 *
 * <p>
 * Example usage:
 *
 * <pre class="code">
 * // Connect to a remote agent
 * A2AClient weatherAgent = DefaultA2AClient.builder()
 *     .agentUrl("http://localhost:10001/a2a")
 *     .build();
 *
 * // Send a task request using AgentModel API
 * AgentTaskRequest request = AgentTaskRequest.builder(
 *     "What's the weather in San Francisco?", null
 * ).build();
 * AgentResponse response = weatherAgent.call(request);
 *
 * // Access agent metadata
 * AgentCard card = weatherAgent.getAgentCard();
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see org.springaicommunity.a2a.client.A2AClient
 */
public interface A2AEndpoint {

	/**
	 * Get the agent card containing agent metadata and capabilities.
	 * <p>
	 * The agent card describes the agent's identity, supported features, skills, and
	 * communication interfaces.
	 * @return the agent card
	 */
	AgentCard getAgentCard();

	/**
	 * Get the name of this agent.
	 * <p>
	 * This is a convenience method that extracts the name from the agent card.
	 * @return the agent name
	 */
	default String getName() {
		return getAgentCard().name();
	}

	/**
	 * Get the description of this agent.
	 * <p>
	 * This is a convenience method that extracts the description from the agent card.
	 * @return the agent description
	 */
	default String getDescription() {
		return getAgentCard().description();
	}

	/**
	 * Check if this agent supports streaming responses.
	 * <p>
	 * This is a convenience method that checks the agent card capabilities.
	 * @return true if streaming is supported
	 */
	default boolean supportsStreaming() {
		return AgentCards.supportsStreaming(getAgentCard());
	}

	/**
	 * Check if this agent supports push notifications.
	 * <p>
	 * This is a convenience method that checks the agent card capabilities.
	 * @return true if push notifications are supported
	 */
	default boolean supportsPushNotifications() {
		return AgentCards.supportsPushNotifications(getAgentCard());
	}

}
