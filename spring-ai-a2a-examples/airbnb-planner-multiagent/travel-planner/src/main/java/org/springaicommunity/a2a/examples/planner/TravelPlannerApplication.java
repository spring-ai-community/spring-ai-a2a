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

package org.springaicommunity.a2a.examples.planner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

/**
 * Travel Planner orchestrator application.
 *
 * Routes user requests to specialized remote A2A agents (weather and accommodation).
 *
 * @author Ilayaperumal Gopinathan
 */
@SpringBootApplication
public class TravelPlannerApplication {

	private static final Logger logger = LoggerFactory.getLogger(TravelPlannerApplication.class);

	private static final String ROUTING_SYSTEM_PROMPT = """
			**Role:** You are an expert Travel Planning Assistant. Your primary function is to help users plan trips by delegating requests to specialized remote agents.

			**Core Directives:**

			* **Task Delegation:** Use the `sendMessage` function to delegate tasks to remote agents.
			* **Contextual Awareness:** Provide complete context when delegating to remote agents.
			* **Autonomous Operations:** Never ask for permission before calling remote agents.
			* **Complete Responses:** Always present the full response from remote agents to the user.
			* **Tool Reliance:** Only use available tools - never invent information.
			* **Focused Delegation:** Provide only relevant information to each agent.

			**Available Agents:**
			%s
			""";

	public static void main(String[] args) {
		SpringApplication.run(TravelPlannerApplication.class, args);
	}

	@Bean
	public ChatClient routingChatClient(ChatClient.Builder chatClientBuilder,
			RemoteAgentConnections remoteAgentConnections) {

		String systemPrompt = String.format(ROUTING_SYSTEM_PROMPT,
				remoteAgentConnections.getAgentDescriptions());

		logger.info("Initializing routing ChatClient with agents: {}",
				remoteAgentConnections.getAgentNames());

		return chatClientBuilder
				.defaultSystem(systemPrompt)
				.defaultTools(remoteAgentConnections)
				.build();
	}

	/**
	 * Interactive console mode for Travel Planner.
	 *
	 * Allows users to enter travel planning queries directly from the console.
	 * Type 'exit' or 'quit' to terminate the session.
	 */
	@Bean
	public CommandLineRunner interactiveConsole(ChatClient routingChatClient) {
		return args -> {
			Scanner scanner = new Scanner(System.in);

			System.out.println("\n=== Travel Planning Assistant ===");
			System.out.println("Ask me anything about trip planning, weather, or accommodations!");
			System.out.println("Type 'exit' or 'quit' to end the session.\n");

			while (true) {
				System.out.print("You: ");
				String userInput = scanner.nextLine().trim();

				if (userInput.equalsIgnoreCase("exit") || userInput.equalsIgnoreCase("quit")) {
					System.out.println("\nGoodbye! Happy travels!");
					break;
				}

				if (userInput.isEmpty()) {
					continue;
				}

				try {
					logger.info("User query: {}", userInput);
					String response = routingChatClient.prompt()
							.user(userInput)
							.call()
							.content();

					System.out.println("\nAssistant: " + response + "\n");
					logger.info("Response sent to user");
				}
				catch (Exception e) {
					logger.error("Error processing request: {}", e.getMessage(), e);
					System.out.println("\nSorry, I encountered an error processing your request. Please try again.\n");
				}
			}

			scanner.close();
		};
	}

}
