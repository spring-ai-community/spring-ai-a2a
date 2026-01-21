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

package org.springaicommunity.a2a.examples.composable.weather;

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
 * Weather Agent - A specialized A2A agent for weather forecasting.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class WeatherAgentApplication {

	private static final String WEATHER_SYSTEM_INSTRUCTION = """
			You are a specialized weather forecast assistant.
			Your primary function is to utilize the provided tools to retrieve and relay weather information in response to user queries.
			You must rely exclusively on these tools for data and refrain from inventing information.
			Ensure that all responses include the detailed output from the tools used and are formatted in Markdown.
			""";

	public static void main(String[] args) {
		SpringApplication.run(WeatherAgentApplication.class, args);
	}

	@Bean
	public AgentCard agentCard() {
		return new AgentCard.Builder().name("Weather Agent")
			.description("Helps with weather forecasts and climate data")
			.url("http://localhost:10001/a2a")
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(new AgentSkill.Builder().id("weather_search")
				.name("Search weather")
				.description("Helps with weather in cities, states, and countries")
				.tags(List.of("weather"))
				.examples(List.of("weather in LA, CA", "weather in Paris in July"))
				.build()))
			.protocolVersion("0.3.0")
			.build();
	}

	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {

		ChatClient chatClient = chatClientBuilder.clone()
			.defaultSystem(WEATHER_SYSTEM_INSTRUCTION)
			.defaultTools(weatherTools)
			.build();

		return new DefaultChatClientAgentExecutor(chatClient);
	}

}
