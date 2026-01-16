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

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Travel Planner Service demonstrating direct A2A SDK usage.
 *
 * <p>This service coordinates with two remote A2A agents using the SDK directly:
 * <ul>
 *   <li><strong>Weather Agent:</strong> Provides weather forecasts
 *   <li><strong>Accommodation Agent:</strong> Provides hotel recommendations
 * </ul>
 *
 * <p><strong>Key Pattern:</strong> Direct SDK usage with BiConsumer event handlers
 * and CountDownLatch for synchronous blocking.
 *
 * <p><strong>Comparison with A2AToolCallback:</strong>
 * <table border="1">
 * <tr>
 *   <th>Aspect</th>
 *   <th>Direct SDK (this example)</th>
 *   <th>A2AToolCallback</th>
 * </tr>
 * <tr>
 *   <td>Orchestration</td>
 *   <td>Manual (programmatic)</td>
 *   <td>Automatic (LLM-driven)</td>
 * </tr>
 * <tr>
 *   <td>Control Flow</td>
 *   <td>Explicit method calls</td>
 *   <td>LLM decides when to call</td>
 * </tr>
 * <tr>
 *   <td>Dependencies</td>
 *   <td>A2A SDK only</td>
 *   <td>Spring AI + A2A SDK</td>
 * </tr>
 * <tr>
 *   <td>Use Case</td>
 *   <td>Deterministic workflows</td>
 *   <td>Conversational AI</td>
 * </tr>
 * </table>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Service
public class TravelPlannerService {

	private static final Logger logger = LoggerFactory.getLogger(TravelPlannerService.class);

	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	private final String weatherAgentUrl;
	private final String accommodationAgentUrl;

	public TravelPlannerService(
			@Value("${weather.agent.url:http://localhost:10001/a2a}") String weatherAgentUrl,
			@Value("${accommodation.agent.url:http://localhost:10002/a2a}") String accommodationAgentUrl) {
		this.weatherAgentUrl = weatherAgentUrl;
		this.accommodationAgentUrl = accommodationAgentUrl;
	}

	/**
	 * Plan a trip by coordinating with weather and accommodation agents.
	 *
	 * <p>This method demonstrates:
	 * <ol>
	 *   <li>Sequential coordination - first weather, then accommodation
	 *   <li>Direct SDK usage for each agent call
	 *   <li>Combining results programmatically
	 * </ol>
	 *
	 * @param request the travel planning request
	 * @return comprehensive travel plan with weather and accommodation info
	 */
	public TravelPlanResponse planTrip(TravelPlanRequest request) {
		logger.info("Planning trip to {} from {} to {}",
				request.destination(), request.startDate(), request.endDate());

		try {
			// Step 1: Get weather forecast
			String weatherInfo = getWeatherForecast(request.destination(), request.startDate());

			// Step 2: Get accommodation recommendations
			String accommodationInfo = getAccommodationRecommendations(
					request.destination(), request.startDate(), request.endDate());

			// Step 3: Combine results
			String plan = formatTravelPlan(request, weatherInfo, accommodationInfo);

			return new TravelPlanResponse(true, plan, null);
		}
		catch (Exception e) {
			logger.error("Failed to plan trip", e);
			return new TravelPlanResponse(false, null, e.getMessage());
		}
	}

	/**
	 * Get weather forecast using direct SDK call to weather agent.
	 *
	 * <p><strong>Direct SDK Pattern:</strong>
	 * <pre>
	 * 1. Create AgentCard for remote agent
	 * 2. Create message request
	 * 3. Setup CountDownLatch and AtomicReference
	 * 4. Create BiConsumer event handler
	 * 5. Build client with consumer registered
	 * 6. Send message and wait
	 * 7. Extract response
	 * </pre>
	 */
	private String getWeatherForecast(String destination, String date) throws Exception {
		logger.info("Getting weather forecast for {} on {}", destination, date);

		// 1. Create AgentCard for weather agent
		AgentCard agentCard = AgentCard.builder()
			.name("Weather Agent")
			.description("Weather forecast agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.supportedInterfaces(List.of(
				new io.a2a.spec.AgentInterface("JSONRPC", weatherAgentUrl)
			))
			.build();

		// 2. Create client configuration
		io.a2a.client.config.ClientConfig clientConfig =
			new io.a2a.client.config.ClientConfig.Builder()
				.setStreaming(false)
				.setAcceptedOutputModes(List.of("text"))
				.build();

		// 3. Create message request
		String query = String.format("What is the weather forecast for %s on %s?", destination, date);
		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart(query)))
			.build();

		// 4. Setup synchronous response handling
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		// 5. Create event consumer
		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			try {
				if (event instanceof MessageEvent messageEvent) {
					responseRef.set(messageEvent.getMessage());
					latch.countDown();
				}
			}
			catch (Exception e) {
				errorRef.set(e);
				latch.countDown();
			}
		};

		// 6. Build client with consumer registered
		Client client = Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(List.of(consumer))
			.build();

		// 7. Send message and wait
		client.sendMessage(request);
		boolean completed = latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

		// 8. Check for errors and extract response
		if (!completed) {
			throw new RuntimeException("Weather agent request timed out after " + TIMEOUT);
		}

		if (errorRef.get() != null) {
			throw new RuntimeException("Weather agent error", errorRef.get());
		}

		Message response = responseRef.get();
		if (response == null) {
			throw new RuntimeException("No response from weather agent");
		}

		String weatherInfo = extractTextFromMessage(response);
		logger.info("Received weather info: {}", weatherInfo.substring(0, Math.min(100, weatherInfo.length())));

		return weatherInfo;
	}

	/**
	 * Get accommodation recommendations using direct SDK call to accommodation agent.
	 *
	 * <p>Uses the same direct SDK pattern as weather forecast.
	 */
	private String getAccommodationRecommendations(String destination, String startDate, String endDate)
			throws Exception {

		logger.info("Getting accommodation recommendations for {} from {} to {}",
				destination, startDate, endDate);

		// 1. Create AgentCard for accommodation agent
		AgentCard agentCard = AgentCard.builder()
			.name("Accommodation Agent")
			.description("Hotel and accommodation recommendations agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.supportedInterfaces(List.of(
				new io.a2a.spec.AgentInterface("JSONRPC", accommodationAgentUrl)
			))
			.build();

		// 2. Create client configuration
		io.a2a.client.config.ClientConfig clientConfig =
			new io.a2a.client.config.ClientConfig.Builder()
				.setStreaming(false)
				.setAcceptedOutputModes(List.of("text"))
				.build();

		// 3. Create request
		String query = String.format(
				"Find the best hotels in %s for dates %s to %s. Focus on location, amenities, and value.",
				destination, startDate, endDate);

		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart(query)))
			.build();

		// 4. Setup response handling
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		// 5. Create consumer
		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			try {
				if (event instanceof MessageEvent messageEvent) {
					responseRef.set(messageEvent.getMessage());
					latch.countDown();
				}
			}
			catch (Exception e) {
				errorRef.set(e);
				latch.countDown();
			}
		};

		// 6. Build client with consumer
		Client client = Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(List.of(consumer))
			.build();

		// 7. Send and wait
		client.sendMessage(request);
		boolean completed = latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

		// 8. Extract response
		if (!completed) {
			throw new RuntimeException("Accommodation agent request timed out after " + TIMEOUT);
		}

		if (errorRef.get() != null) {
			throw new RuntimeException("Accommodation agent error", errorRef.get());
		}

		Message response = responseRef.get();
		if (response == null) {
			throw new RuntimeException("No response from accommodation agent");
		}

		String accommodationInfo = extractTextFromMessage(response);
		logger.info("Received accommodation info: {}",
				accommodationInfo.substring(0, Math.min(100, accommodationInfo.length())));

		return accommodationInfo;
	}

	/**
	 * Extract text content from A2A Message.
	 */
	private String extractTextFromMessage(Message message) {
		if (message == null || message.parts() == null || message.parts().isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (Object part : message.parts()) {
			if (part instanceof TextPart textPart) {
				result.append(textPart.text());
			}
		}
		return result.toString();
	}

	/**
	 * Format complete travel plan by combining agent responses.
	 */
	private String formatTravelPlan(TravelPlanRequest request, String weatherInfo, String accommodationInfo) {
		return String.format("""
			# Travel Plan for %s

			**Dates:** %s to %s

			## Weather Forecast

			%s

			## Accommodation Recommendations

			%s

			---
			*Generated using direct A2A SDK integration*
			""",
			request.destination(),
			request.startDate(),
			request.endDate(),
			weatherInfo,
			accommodationInfo
		);
	}

	/**
	 * Travel planning request.
	 */
	public record TravelPlanRequest(String destination, String startDate, String endDate) {}

	/**
	 * Travel planning response.
	 */
	public record TravelPlanResponse(boolean success, String plan, String error) {}

}
