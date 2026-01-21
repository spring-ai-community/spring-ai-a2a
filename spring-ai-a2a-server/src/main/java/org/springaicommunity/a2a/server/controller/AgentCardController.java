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

import io.a2a.spec.AgentCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for serving the A2A agent card metadata.
 *
 * <p>This controller implements the A2A protocol endpoint for agent discovery.
 * The agent card provides essential metadata about the agent including its
 * capabilities, supported interfaces, and protocol version.
 *
 * <p><strong>Endpoint:</strong>
 * <ul>
 *   <li>{@code GET ${spring.ai.a2a.server.base-path:/a2a}/card} - Get agent metadata</li>
 * </ul>
 *
 * <p><strong>Example Response:</strong>
 * <pre>
 * {
 *   "name": "Spring AI A2A Agent",
 *   "description": "A2A agent powered by Spring AI",
 *   "version": "1.0.0",
 *   "protocolVersion": "0.1.0",
 *   "capabilities": {
 *     "streaming": false,
 *     "pushNotifications": false,
 *     "stateTransitionHistory": false,
 *     "supportedInputModes": []
 *   },
 *   "defaultInputModes": ["text"],
 *   "defaultOutputModes": ["text"],
 *   "additionalInterfaces": [
 *     {"type": "JSONRPC", "url": "http://localhost:8080/a2a"}
 *   ],
 *   "preferredTransport": "JSONRPC"
 * }
 * </pre>
 *
 * <p><strong>Customization:</strong>
 * <p>Applications can customize the agent card by providing their own {@link AgentCard} bean:
 * <pre>
 * &#64;Bean
 * public AgentCard customAgentCard() {
 *     return new AgentCard(
 *         "My Custom Agent",
 *         "A specialized agent for...",
 *         "http://localhost:8080/a2a",
 *         null, // provider
 *         "2.0.0", // version
 *         null, // documentationUrl
 *         new AgentCapabilities(true, false, false, List.of("text", "image")),
 *         List.of("text", "image"),
 *         List.of("text"),
 *         List.of(), // skills
 *         false, // supportsAuthenticatedExtendedCard
 *         null, // securitySchemes
 *         null, // security
 *         null, // iconUrl
 *         List.of(new AgentInterface("JSONRPC", "http://localhost:8080/a2a")),
 *         "JSONRPC",
 *         "0.1.0",
 *         null // signatures
 *     );
 * }
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see AgentCard
 */
@RestController
@RequestMapping("${spring.ai.a2a.server.base-path:/a2a}")
public class AgentCardController {

	private static final Logger logger = LoggerFactory.getLogger(AgentCardController.class);

	private final AgentCard agentCard;

	/**
	 * Create a new AgentCardController.
	 *
	 * @param agentCard the agent card metadata to serve
	 */
	public AgentCardController(AgentCard agentCard) {
		this.agentCard = agentCard;
	}

	/**
	 * Retrieve the agent card metadata.
	 *
	 * <p>This endpoint allows clients to discover the agent's capabilities,
	 * supported interfaces, and other metadata required for A2A protocol
	 * communication.
	 *
	 * <p>The agent card is typically cached by clients for the duration of
	 * their session to avoid repeated requests.
	 *
	 * @return the agent card metadata
	 */
	@GetMapping(
		path = "/card",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	public AgentCard getAgentCard() {
		logger.debug("Serving agent card: {}", agentCard.name());
		return agentCard;
	}

}
