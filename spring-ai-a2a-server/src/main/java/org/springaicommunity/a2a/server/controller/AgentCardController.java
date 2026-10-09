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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.a2aproject.sdk.compat03.conversion.A2AProtocol_v0_3;
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
 * <p>
 * The JSON-RPC endpoint handles A2A v0.3 and v1.0 requests, and section 5.2 of the A2A
 * specification requires agents to declare all supported protocols. The controller
 * therefore adds an A2A v0.3 interface for each JSON-RPC interface of the agent card that
 * doesn't declare one.
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
		this.agentCard = withV03JsonRpcInterfaces(withJsonRpcUrl(agentCard));
	}

	/**
	 * Returns the agent card with an additional A2A v0.3 interface for each of its
	 * JSON-RPC interfaces that isn't declared for A2A v0.3 yet. The added interfaces
	 * follow the declared ones, so the agent card keeps its preferred interface.
	 */
	static AgentCard withV03JsonRpcInterfaces(AgentCard agentCard) {
		List<AgentInterface> supportedInterfaces = new ArrayList<>(agentCard.supportedInterfaces());
		for (AgentInterface agentInterface : agentCard.supportedInterfaces()) {
			if (!TransportProtocol.JSONRPC.asString().equals(agentInterface.protocolBinding())) {
				continue;
			}
			AgentInterface v03Interface = new AgentInterface(agentInterface.protocolBinding(), agentInterface.url(),
					agentInterface.tenant(), A2AProtocol_v0_3.PROTOCOL_VERSION);
			if (supportedInterfaces.stream().noneMatch(declared -> isSameInterface(declared, v03Interface))) {
				supportedInterfaces.add(v03Interface);
			}
		}
		if (supportedInterfaces.size() == agentCard.supportedInterfaces().size()) {
			return agentCard;
		}
		return AgentCard.builder(agentCard).supportedInterfaces(supportedInterfaces).build();
	}

	/**
	 * Compares two interfaces and handles a missing tenant like an empty one.
	 */
	private static boolean isSameInterface(AgentInterface first, AgentInterface second) {
		return first.protocolBinding().equals(second.protocolBinding()) && first.url().equals(second.url())
				&& Objects.requireNonNullElse(first.tenant(), "")
					.equals(Objects.requireNonNullElse(second.tenant(), ""))
				&& first.protocolVersion().equals(second.protocolVersion());
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
