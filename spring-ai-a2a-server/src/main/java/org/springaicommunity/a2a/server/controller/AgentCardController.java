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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for A2A agent card metadata.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
@RestController
public class AgentCardController {

	private static final Logger logger = LoggerFactory.getLogger(AgentCardController.class);

	private final AgentCard agentCard;

	public AgentCardController(AgentCard agentCard) {
		this.agentCard = agentCard;
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
