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

package org.springaicommunity.a2a.integration;

import java.util.List;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Test Spring Boot application for A2A integration tests.
 *
 * <p>Uses the simplified architecture with auto-configuration:
 * <ul>
 * <li>Starts on port 58888 (configured in application.properties)</li>
 * <li>Auto-configures A2A server via spring-ai-a2a-server starter</li>
 * <li>Provides ChatClient bean for agent logic</li>
 * <li>Provides AgentCard bean for agent metadata</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class TestA2AApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestA2AApplication.class, args);
	}

	/**
	 * ChatClient bean for the test agent.
	 * <p>
	 * The A2A server auto-configuration will use this ChatClient to handle
	 * incoming requests.
	 */
	@Bean
	public ChatClient testAgent(ChatModel chatModel) {
		return ChatClient.builder(chatModel)
			.defaultSystem(getSystemPrompt())
			.build();
	}

	/**
	 * AgentCard bean with agent metadata.
	 * <p>
	 * Uses fixed port 58888 configured in application.properties.
	 */
	@Bean
	public AgentCard agentCard() {
		return AgentCard.builder()
			.name("Test Echo Agent")
			.description("Simple agent that echoes messages for testing")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(AgentCapabilities.builder()
				.streaming(false)
				.pushNotifications(false)
				.stateTransitionHistory(false)
				.build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(
				new AgentInterface("JSONRPC", "http://localhost:58888/a2a")))
			.build();
	}

	/**
	 * System prompt defining the test agent's behavior.
	 */
	private String getSystemPrompt() {
		return """
			You are a test echo agent for integration testing.

			Always respond with "Echo: " followed by the user's message.

			Example:
			User: Hello, agent!
			You: Echo: Hello, agent!
			""";
	}

}
