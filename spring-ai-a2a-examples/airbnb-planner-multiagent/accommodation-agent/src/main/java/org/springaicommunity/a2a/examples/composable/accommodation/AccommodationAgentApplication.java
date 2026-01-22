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

package org.springaicommunity.a2a.examples.composable.accommodation;

import java.util.List;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springaicommunity.chatclient.executor.ChatClientExecutor;

/**
 * Accommodation Agent - A specialized A2A agent for accommodation search and recommendations.
 *
 * <p>Demonstrates ChatClientExecutor pattern for custom execution logic.
 * Provides AgentCard, ChatClient, and ChatClientExecutor beans.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class AccommodationAgentApplication {

	private static final String ACCOMMODATION_SYSTEM_INSTRUCTION = """
			You are a specialized assistant for accommodation search and recommendations.
			Your primary function is to utilize the provided tools to search for accommodations and answer related questions.
			You must rely exclusively on these tools for information; do not invent listings or prices.
			Ensure that your Markdown-formatted response includes all relevant tool output, with particular emphasis on providing direct links to listings.
			""";

	public static void main(String[] args) {
		SpringApplication.run(AccommodationAgentApplication.class, args);
	}

	/**
	 * Define AgentCard metadata for this agent.
	 */
	@Bean
	public AgentCard agentCard() {
		return new AgentCard(
			"Accommodation Agent",
			"Helps with searching accommodations, hotels, and lodging",
			"http://localhost:10002/a2a",
			null,
			"1.0.0",
			null,
			new AgentCapabilities(false, false, false, List.of()),
			List.of("text"),
			List.of("text"),
			List.of(),
			false,
			null,
			null,
			null,
			List.of(new AgentInterface("JSONRPC", "http://localhost:10002/a2a")),
			"JSONRPC",
			"0.3.0",
			null
		);
	}

	/**
	 * Create ChatClient with accommodation tools and system prompt.
	 */
	@Bean
	public ChatClient accommodationChatClient(ChatClient.Builder chatClientBuilder,
			AccommodationTools accommodationTools) {

		return chatClientBuilder.clone()
			.defaultSystem(ACCOMMODATION_SYSTEM_INSTRUCTION)
			.defaultTools(accommodationTools)
			.build();
	}

	/**
	 * Define custom ChatClientExecutor with protocol-agnostic execution logic.
	 *
	 * <p>This executor receives the user message directly (no protocol coupling).
	 * Auto-configuration will use this executor to create the AgentExecutor.
	 */
	@Bean
	public ChatClientExecutor chatClientExecutor() {
		return (chatClient, userMessage, context) -> chatClient.prompt()
			.user(userMessage)
			.toolContext(context)
			.call()
			.content();
	}

}
