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

import java.util.List;

import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AgentCardController}.
 *
 * @author Thorben Janssen
 * @since 0.4.0
 */
class AgentCardControllerTests {

	private static final String JSON_RPC_URL = "http://localhost:8080/jsonrpc";

	private static final String GRPC_URL = "http://localhost:9090";

	@Test
	void addsUrlOfJsonRpcInterfaceIfUrlIsMissing() {
		AgentCard agentCard = agentCard(new AgentInterface("GRPC", GRPC_URL),
				new AgentInterface("JSONRPC", JSON_RPC_URL));

		AgentCard servedCard = new AgentCardController(agentCard).getAgentCard();

		assertThat(servedCard.url()).isEqualTo(JSON_RPC_URL);
		assertThat(servedCard.supportedInterfaces()).isEqualTo(agentCard.supportedInterfaces());
	}

	@Test
	void keepsExistingUrl() {
		AgentCard agentCard = AgentCard.builder(agentCard(new AgentInterface("JSONRPC", JSON_RPC_URL)))
			.url("http://localhost:8080/legacy")
			.build();

		AgentCard servedCard = new AgentCardController(agentCard).getAgentCard();

		assertThat(servedCard).isSameAs(agentCard);
		assertThat(servedCard.url()).isEqualTo("http://localhost:8080/legacy");
	}

	@Test
	void keepsAgentCardWithoutJsonRpcInterface() {
		AgentCard agentCard = agentCard(new AgentInterface("GRPC", GRPC_URL));

		AgentCard servedCard = new AgentCardController(agentCard).getAgentCard();

		assertThat(servedCard).isSameAs(agentCard);
		assertThat(servedCard.url()).isNull();
	}

	@Test
	void servesSameAgentCardOnBothEndpoints() {
		AgentCardController controller = new AgentCardController(
				agentCard(new AgentInterface("JSONRPC", JSON_RPC_URL)));

		assertThat(controller.getAgentCardV1()).isSameAs(controller.getAgentCard());
		assertThat(controller.getAgentCardV1().url()).isEqualTo(JSON_RPC_URL);
	}

	@Test
	void jsonRpcUrlPrefersUrlOfAgentCard() {
		AgentCard agentCard = AgentCard.builder(agentCard(new AgentInterface("JSONRPC", JSON_RPC_URL)))
			.url("http://localhost:8080/legacy")
			.build();

		assertThat(AgentCardController.jsonRpcUrl(agentCard)).isEqualTo("http://localhost:8080/legacy");
	}

	@Test
	void jsonRpcUrlIsNullWithoutUrlAndJsonRpcInterface() {
		assertThat(AgentCardController.jsonRpcUrl(agentCard(new AgentInterface("GRPC", GRPC_URL)))).isNull();
	}

	private static AgentCard agentCard(AgentInterface... supportedInterfaces) {
		return AgentCard.builder()
			.name("Test Agent")
			.description("Agent for tests")
			.version("1.0.0")
			.capabilities(AgentCapabilities.builder().build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(supportedInterfaces))
			.build();
	}

}
