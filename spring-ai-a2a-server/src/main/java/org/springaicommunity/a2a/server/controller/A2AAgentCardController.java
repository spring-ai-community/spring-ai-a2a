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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exposing agent metadata via the AgentCard endpoint.
 *
 * <p>This controller provides the {@code GET /a2a/card} endpoint that returns
 * the agent's metadata (name, description, capabilities, etc.) as an AgentCard.
 *
 * <p>The agent metadata is provided by the user application as an {@link AgentCard}
 * bean. The user simply registers an AgentCard bean and this controller returns it.
 *
 * <p><strong>Usage - User registers AgentCard bean:</strong>
 * <pre>
 * &#64;Bean
 * public AgentCard agentCard() {
 *     return AgentCard.builder()
 *         .name("Weather Agent")
 *         .description("Provides weather forecasts and climate data")
 *         .version("1.0.0")
 *         .protocolVersion("0.1.0")
 *         .capabilities(AgentCapabilities.builder()
 *             .streaming(true)
 *             .build())
 *         .defaultInputModes(List.of("text"))
 *         .defaultOutputModes(List.of("text"))
 *         .skills(List.of())
 *         .supportedInterfaces(List.of(
 *             new AgentInterface("JSONRPC", "http://localhost:8080/a2a")))
 *         .build();
 * }
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@RestController
public class A2AAgentCardController {

	private final AgentCard agentCard;

	/**
	 * Constructor injection of user-provided AgentCard bean.
	 * @param agentCard the agent card bean registered by the user
	 */
	public A2AAgentCardController(AgentCard agentCard) {
		this.agentCard = agentCard;
	}

	/**
	 * Get the agent card describing this agent's capabilities and metadata.
	 *
	 * @return the agent card provided by the user application
	 */
	@GetMapping("/a2a/card")
	public AgentCard getAgentCard() {
		return agentCard;
	}

}
