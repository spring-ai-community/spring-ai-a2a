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

import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for A2A agent card metadata.
 *
 * <p>
 * A2A v0.3 clients require the agent card's {@code url}, which A2A v1.0 replaced with
 * {@code supportedInterfaces}. If the agent card doesn't define a {@code url}, the
 * controller sets it to the URL of the agent's JSON-RPC interface, so that A2A v0.3
 * clients can use the agent card as well.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Thorben Janssen
 * @since 0.1.0
 */
@RestController
public class AgentCardController {

	private static final Logger logger = LoggerFactory.getLogger(AgentCardController.class);

	private final AgentCard agentCard;

	public AgentCardController(AgentCard agentCard) {
		this.agentCard = withJsonRpcUrl(agentCard);
	}

	/**
	 * Returns the agent card with its {@code url} set to the URL of its JSON-RPC
	 * interface, if it doesn't define a {@code url} yet.
	 */
	static AgentCard withJsonRpcUrl(AgentCard agentCard) {
		String url = jsonRpcUrl(agentCard);
		if (agentCard.url() != null || url == null) {
			return agentCard;
		}
		return AgentCard.builder(agentCard).url(url).build();
	}

	/**
	 * Returns the {@code url} of the agent card or, if it isn't set, the URL of its
	 * JSON-RPC interface. Returns {@code null} if the agent card defines neither.
	 */
	public static String jsonRpcUrl(AgentCard agentCard) {
		if (agentCard.url() != null) {
			return agentCard.url();
		}
		return agentCard.supportedInterfaces()
			.stream()
			.filter(agentInterface -> TransportProtocol.JSONRPC.asString().equals(agentInterface.protocolBinding()))
			.map(AgentInterface::url)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Returns agent card metadata.
	 */
	@GetMapping(path = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public AgentCard getAgentCard() {
		logger.debug("Serving agent card: {}", this.agentCard.name());
		return this.agentCard;
	}

	/**
	 * Alternative endpoint for getting the agent card. Some A2A implementations may use
	 * this endpoint.
	 * @return the agent card in JSON format
	 */
	@GetMapping(path = "/card", produces = MediaType.APPLICATION_JSON_VALUE)
	public AgentCard getAgentCardV1() {
		logger.debug("Serving agent card via /card: {}", this.agentCard.name());
		return this.agentCard;
	}

}
