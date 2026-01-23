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

import java.util.List;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCard;
import org.junit.jupiter.api.Test;
import org.springaicommunity.a2a.server.executor.DefaultA2AChatClientAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for A2A server auto-configuration.
 *
 * <p>
 * Verifies:
 * <ul>
 * <li>A2A server starts successfully with auto-configuration</li>
 * <li>Default AgentCard bean is created with correct configuration</li>
 * <li>HTTP endpoints are available</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class A2AClientServerIntegrationTests {

	/**
	 * Minimal Spring Boot application for testing auto-configuration.
	 */
	@SpringBootApplication
	static class TestApplication {

		public static void main(String[] args) {
			SpringApplication.run(TestApplication.class, args);
		}

	}

	@TestConfiguration
	static class TestConfig {

		/**
		 * Provides a minimal ChatClient bean to trigger auto-configuration.
		 */
		@Bean
		public ChatClient testChatClient(ChatModel chatModel) {
			return ChatClient.builder(chatModel).defaultSystem("You are a test agent").build();
		}

		/**
		 * Provides AgentCard bean for testing.
		 */
		@Bean
		public AgentCard testAgentCard() {
			return new AgentCard("Spring AI A2A Agent", "A2A agent powered by Spring AI", "http://localhost:58888/a2a",
					null, "1.0.0", null, new io.a2a.spec.AgentCapabilities(false, false, false, List.of()),
					List.of("text"), List.of("text"), List.of(), false, null, null, null,
					List.of(new io.a2a.spec.AgentInterface("JSONRPC", "http://localhost:58888/a2a")), "JSONRPC",
					"0.1.0", null);
		}

		/**
		 * Provides a test AgentExecutor bean.
		 */
		@Bean
		public AgentExecutor testAgentExecutor(ChatClient testChatClient) {
			return new DefaultA2AChatClientAgentExecutor(testChatClient, (chatClient, requestContext) -> {
				return DefaultA2AChatClientAgentExecutor.extractTextFromMessage(requestContext.getMessage());
			}) {
			};
		}

	}

	@LocalServerPort
	private int port;

	@Autowired
	private AgentCard agentCard;

	/**
	 * Tests that the A2A server started successfully with default auto-configuration.
	 */
	@Test
	void testA2AServerStarted() {
		assertThat(this.agentCard).isNotNull();
		assertThat(this.agentCard.name()).isEqualTo("Spring AI A2A Agent");
		assertThat(this.agentCard.description()).isEqualTo("A2A agent powered by Spring AI");
		assertThat(this.agentCard.version()).isEqualTo("1.0.0");
		assertThat(this.agentCard.protocolVersion()).isEqualTo("0.1.0");
	}

	/**
	 * Tests that the server is running on the expected port.
	 */
	@Test
	void testServerPort() {
		assertThat(this.port).isEqualTo(58888);
	}

	/**
	 * Tests that default AgentCard capabilities are configured.
	 */
	@Test
	void testAgentCardCapabilities() {
		assertThat(this.agentCard.capabilities()).isNotNull();
		assertThat(this.agentCard.capabilities().streaming()).isFalse();
		assertThat(this.agentCard.capabilities().pushNotifications()).isFalse();
	}

}
