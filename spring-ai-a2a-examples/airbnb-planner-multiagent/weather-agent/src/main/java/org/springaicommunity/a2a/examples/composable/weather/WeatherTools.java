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

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Weather Tools - Mock implementation for weather data.
 *
 * @author Ilayaperumal Gopinathan
 */
@Component
public class WeatherTools {

	@Tool(description = "Get current weather for a location")
	public String getCurrentWeather(
			@ToolParam(description = "The city and state, e.g. San Francisco, CA") String location) {
		// Mock implementation - replace with real API call
		return String.format("Current weather in %s: Sunny, 72°F (22°C), Humidity: 45%%", location);
	}

	@Tool(description = "Get weather forecast for a location")
	public String getWeatherForecast(
			@ToolParam(description = "The city and state, e.g. San Francisco, CA") String location,
			@ToolParam(description = "Number of days for forecast (1-7)") int days) {
		// Mock implementation - replace with real API call
		StringBuilder forecast = new StringBuilder();
		forecast.append(String.format("Weather forecast for %s (%d days):\n", location, days));
		for (int i = 1; i <= Math.min(days, 7); i++) {
			forecast.append(String.format("Day %d: Partly cloudy, High: %d°F, Low: %d°F\n",
				i, 70 + i, 55 + i));
		}
		return forecast.toString();
	}

}
