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

package org.springaicommunity.a2a.examples.directsdk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Direct SDK Travel Planner Example Application.
 *
 * <p>This example demonstrates using the A2A Java SDK directly without
 * A2AToolCallback or Spring AI ChatClient integration. It shows:
 * <ul>
 *   <li>Creating A2A clients using A2AClientUtils convenience methods
 *   <li>Using BiConsumer event handlers for synchronous execution
 *   <li>Coordinating multiple remote A2A agents programmatically
 *   <li>Combining results from multiple agents
 * </ul>
 *
 * <p><strong>Architecture:</strong>
 * <pre>
 * REST Controller
 *     ↓
 * TravelPlannerService
 *     ├─> WeatherAgentClient (direct SDK)
 *     └─> AccommodationAgentClient (direct SDK)
 * </pre>
 *
 * <p><strong>Contrast with A2AToolCallback approach:</strong>
 * <ul>
 *   <li><strong>A2AToolCallback:</strong> LLM decides when to call agents, automatic orchestration
 *   <li><strong>Direct SDK:</strong> Programmatic control, manual orchestration, no LLM needed
 * </ul>
 *
 * <p><strong>Running the application:</strong>
 * <pre>
 * # Start weather agent on port 10001
 * cd airbnb-planner-multiagent/weather-agent
 * mvn spring-boot:run
 *
 * # Start accommodation agent on port 10002
 * cd airbnb-planner-multiagent/accommodation-agent
 * mvn spring-boot:run
 *
 * # Start this application
 * mvn spring-boot:run
 *
 * # Test via REST
 * curl -X POST http://localhost:8080/api/travel/plan \
 *   -H 'Content-Type: application/json' \
 *   -d '{"destination": "Paris", "startDate": "2024-07-01", "endDate": "2024-07-05"}'
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class DirectSdkTravelPlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DirectSdkTravelPlannerApplication.class, args);
	}

}
