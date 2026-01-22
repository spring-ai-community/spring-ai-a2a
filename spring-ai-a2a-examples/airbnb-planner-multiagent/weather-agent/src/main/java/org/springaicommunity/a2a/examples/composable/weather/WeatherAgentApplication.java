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

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Weather Agent - A specialized A2A agent for weather forecasting.
 *
 * <p>Demonstrates minimal-configuration A2A agent setup. Provides AgentCard
 * and ChatClient beans, auto-configuration handles the rest.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class WeatherAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherAgentApplication.class, args);
	}

	/**
	 * Define AgentCard metadata for this agent.
	 */
	@Bean
	public AgentCard agentCard() {
		return new AgentCard(
			"Weather Agent",
			"Helps with weather forecasts and climate data",
			"http://localhost:10001/a2a",
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
			List.of(new AgentInterface("JSONRPC", "http://localhost:10001/a2a")),
			"JSONRPC",
			"0.3.0",
			null
		);
	}

	/**
	 * Create ChatClient with weather tools.
	 */
	@Bean
	public ChatClient weatherChatClient(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {
		return chatClientBuilder.clone()
			.defaultTools(weatherTools)
			.build();
	}

}
