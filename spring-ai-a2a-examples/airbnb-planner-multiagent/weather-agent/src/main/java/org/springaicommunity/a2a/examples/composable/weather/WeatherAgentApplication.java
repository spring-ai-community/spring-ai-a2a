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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Weather Agent Application.
 *
 * <p>This is a specialized A2A remote agent providing weather forecasting capabilities.
 *
 * <p><strong>Running the Agent:</strong>
 * <pre>
 * mvn spring-boot:run
 * </pre>
 *
 * <p><strong>Agent Endpoint:</strong>
 * <ul>
 *   <li>A2A Protocol: http://localhost:10001/a2a</li>
 * </ul>
 *
 * <p><strong>Testing:</strong>
 * <pre>
 * curl -X POST http://localhost:10001/a2a \
 *   -H 'Content-Type: application/json' \
 *   -d '{
 *     "jsonrpc": "2.0",
 *     "method": "sendMessage",
 *     "params": {
 *       "message": {
 *         "role": "user",
 *         "parts": [{"text": "What is the weather in Paris in July?"}]
 *       }
 *     },
 *     "id": 1
 *   }'
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class WeatherAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(WeatherAgentApplication.class, args);
		System.out.println("\n" +
			"☀️ Weather Agent Started!\n" +
			"A2A Endpoint: http://localhost:10001/a2a\n");
	}

}
