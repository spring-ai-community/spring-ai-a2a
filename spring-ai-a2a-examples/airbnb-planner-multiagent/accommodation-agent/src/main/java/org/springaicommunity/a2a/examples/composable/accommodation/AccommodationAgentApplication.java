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

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springaicommunity.a2a.server.executor.DefaultChatClientAgentExecutor;

/**
 * Accommodation Agent - A specialized A2A agent for accommodation search and recommendations.
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

	@Bean
	public AgentCard agentCard() {
		return new AgentCard.Builder().name("Accommodation Agent")
			.description("Helps with searching accommodations, hotels, and lodging")
			.url("http://localhost:10002/a2a")
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(new AgentSkill.Builder().id("accommodation_search")
				.name("Search accommodation")
				.description("Helps with accommodation search including hotels, Airbnb, and other lodging options")
				.tags(List.of("accommodation", "hotels", "airbnb"))
				.examples(List.of("Find a hotel in Paris for 3 nights", "Search for accommodation in New York"))
				.build()))
			.protocolVersion("0.3.0")
			.build();
	}

	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, AccommodationTools accommodationTools) {

		ChatClient chatClient = chatClientBuilder.clone()
			.defaultSystem(ACCOMMODATION_SYSTEM_INSTRUCTION)
			.defaultTools(accommodationTools)
			.build();

		return new DefaultChatClientAgentExecutor(chatClient);
	}

}
