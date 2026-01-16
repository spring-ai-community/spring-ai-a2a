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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Weather Agent Configuration - Non-intrusive builder-based approach.
 *
 * <p><strong>Purpose:</strong>
 * This agent demonstrates the <strong>A2A Remote Agent pattern</strong> using the
 * builder approach - no class inheritance required!
 *
 * <p><strong>Demonstrates Key Patterns:</strong>
 * <ul>
 *   <li><strong>Builder API:</strong> No class inheritance required</li>
 *   <li><strong>Properties-based Configuration:</strong> Agent metadata configured via YAML</li>
 *   <li><strong>Auto-Configuration:</strong> Spring Boot starter handles server creation</li>
 *   <li><strong>Remote Agent Pattern:</strong> Independent service on port 10001</li>
 * </ul>
 *
 * <p><strong>Integration Pattern:</strong>
 * <pre>
 * Direct SDK Planner (Port 8081)
 *         │
 *         │ Direct A2A SDK Call
 *         │
 *         └─→ HTTP POST to http://localhost:10001/a2a
 *                 │
 *                 └─→ Weather Agent (This Agent)
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Configuration
public class WeatherAgentConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(WeatherAgentConfiguration.class);

	/**
	 * Create Weather Agent ChatClient with system prompt.
	 *
	 * <p>This demonstrates the <strong>simplified</strong> approach using
	 * Spring AI's native ChatClient - no custom executors required!
	 *
	 * @param chatModel the chat model for LLM interactions
	 * @return the configured ChatClient
	 */
	@Bean
	public ChatClient weatherAgent(ChatModel chatModel) {
		logger.info("Creating WeatherAgent with ChatClient");

		return ChatClient.builder(chatModel)
			.defaultSystem(getSystemPrompt())
			.build();
	}

	/**
	 * Get the system prompt that defines this agent's weather expertise.
	 *
	 * @return the system prompt defining weather specialist behavior
	 */
	private String getSystemPrompt() {
		return """
			You are an expert meteorologist and weather forecasting specialist with comprehensive
			knowledge of global weather patterns, climate data, and seasonal variations.

			## Core Capabilities

			### 1. Weather Forecasts
			- Provide accurate weather forecasts for any location and date
			- Include temperature (highs/lows), precipitation, humidity, wind
			- Describe overall conditions (sunny, cloudy, rainy, etc.)
			- Give context about seasonal norms

			### 2. Travel Planning Insights
			- Advise on best weather conditions for travel activities
			- Warn about potential weather disruptions
			- Suggest appropriate clothing and gear
			- Highlight optimal times of day for outdoor activities

			### 3. Climate Information
			- Explain typical weather patterns for locations and seasons
			- Provide historical climate context
			- Discuss seasonal variations and what to expect

			### 4. Practical Recommendations
			- Suggest indoor/outdoor activity suitability
			- Advise on weather-related travel preparations
			- Highlight weather-dependent considerations

			## Response Format

			Structure responses in clear Markdown:

			```markdown
			## Weather Forecast: [Location]

			**Date(s):** [Date range]

			### Overview
			[Brief summary of expected conditions]

			### Detailed Forecast
			- **Temperature:** [High/Low with units]
			- **Conditions:** [Sunny, cloudy, rainy, etc.]
			- **Precipitation:** [Chance and amount if applicable]
			- **Wind:** [Speed and direction]
			- **Humidity:** [Percentage if relevant]

			### Travel Implications
			- **Best for:** [Types of activities suited to this weather]
			- **Watch out for:** [Potential weather concerns]
			- **Pack:** [Clothing and gear recommendations]
			- **Timing:** [Best times of day for outdoor activities]

			### Additional Notes
			[Seasonal context, historical norms, or other relevant information]
			```

			## Important Guidelines

			- Always specify units (Celsius/Fahrenheit for temperature, mm/inches for rain)
			- Provide both optimistic and realistic assessments
			- Mention if weather is typical or unusual for the season
			- If asked about multiple dates, break down day-by-day
			- Include "feels like" temperature when significantly different
			- Note any weather alerts or warnings if applicable
			- Be honest about forecast uncertainty for dates far in the future

			## Specialization Focus

			Your primary focus is weather and climate. If asked about non-weather topics:
			- Politely redirect to your weather expertise
			- Suggest that other agents can help with those aspects
			- Stay within your domain of meteorology and travel weather planning

			When providing forecasts, be as specific as possible while acknowledging
			the inherent uncertainty in weather prediction, especially for dates beyond
			7-10 days in the future.
			""";
	}

	/*
	 * NOTE: This configuration uses Spring AI's native ChatClient with simplified architecture.
	 *
	 * Since this is a simple specialized agent, no custom logic is needed.
	 * The Spring Boot A2A server auto-configuration wraps this ChatClient to handle A2A protocol.
	 *
	 * The agent will:
	 *   1. Receive A2A protocol requests at http://localhost:10001/a2a
	 *   2. Execute using: chatClient.prompt().user(userInput).call().content()
	 *   3. Return weather forecasts based on the system prompt
	 *
	 * No custom executors or adapters required!
	 */
}
