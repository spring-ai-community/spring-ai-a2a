/*
 * Copyright 2025 - 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springaicommunity.a2a.examples.host;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Manages connections to remote A2A agents. Resolves agent cards at startup and provides
 * tool methods for ChatClient.
 *
 * @author Christian Tzolov
 */
@Service
public class RemoteAgentConnections {

	private static final Logger logger = LoggerFactory.getLogger(RemoteAgentConnections.class);

	private final Map<String, AgentCard> cards = new HashMap<>();

	public RemoteAgentConnections(@Value("${remote.agents.urls}") List<String> agentUrls) {
		for (String url : agentUrls) {
			try {
				logger.info("Resolving agent card from: {}", url);

				String path = new URI(url).getPath();

				AgentCard card = A2A.getAgentCard(url, path + ".well-known/agent-card.json", null);

				this.cards.put(card.name(), card);

				logger.info("Discovered agent: {} at {}", card.name(), url);
			}
			catch (Exception e) {
				logger.error("Failed to connect to agent at {}: {}", url, e.getMessage());
			}
		}
	}

	/**
	 * Sends a task to a remote agent and returns the response.
	 * @param agentName The name of the agent to send the task to
	 * @param task The task description to send
	 * @return The response from the remote agent
	 */
	@Tool(description = "Sends a task to a remote agent. Use this to delegate work to specialized agents.")
	public String sendMessage(@ToolParam(description = "The name of the agent to send the task to") String agentName,
			@ToolParam(
					description = "The comprehensive task description and context to send to the agent") String task) {

		logger.info("Sending message to agent '{}': {}", agentName, task);

		AgentCard agentCard = this.cards.get(agentName);
		if (agentCard == null) {
			String availableAgents = String.join(", ", this.cards.keySet());
			return String.format("Agent '%s' not found. Available agents: %s", agentName, availableAgents);
		}

		try {
			// Create the message
			Message message = new Message.Builder().role(Message.Role.USER)
				.parts(List.of(new TextPart(task, null)))
				.build();

			// Use CompletableFuture to wait for the response
			CompletableFuture<String> responseFuture = new CompletableFuture<>();
			AtomicReference<String> responseText = new AtomicReference<>("");

			BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
				if (event instanceof TaskEvent taskEvent) {
					Task completedTask = taskEvent.getTask();
					logger.info("Received task response: status={}", completedTask.getStatus().state());

					// Extract text from artifacts
					if (completedTask.getArtifacts() != null) {
						StringBuilder sb = new StringBuilder();
						for (Artifact artifact : completedTask.getArtifacts()) {
							if (artifact.parts() != null) {
								for (Part<?> part : artifact.parts()) {
									if (part instanceof TextPart textPart) {
										sb.append(textPart.getText());
									}
								}
							}
						}
						responseText.set(sb.toString());
					}
					responseFuture.complete(responseText.get());
				}
			};

			// Create client with consumer via builder
			ClientConfig clientConfig = new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build();
			Client client = Client.builder(agentCard)
				.clientConfig(clientConfig)
				.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
				.addConsumers(List.of(consumer))
				.build();

			client.sendMessage(message);

			// Wait for response (with timeout)
			String result = responseFuture.get(60, java.util.concurrent.TimeUnit.SECONDS);
			logger.info("Agent '{}' response: {}", agentName, result);
			return result;
		}
		catch (Exception e) {
			logger.error("Error sending message to agent '{}': {}", agentName, e.getMessage());
			return String.format("Error communicating with agent '%s': %s", agentName, e.getMessage());
		}
	}

	/**
	 * Returns a JSON-formatted description of all available agents for the system prompt.
	 */
	public String getAgentDescriptions() {
		return this.cards.values()
			.stream()
			.map(card -> String.format("{\"name\": \"%s\", \"description\": \"%s\"}", card.name(),
					card.description() != null ? card.description() : "No description"))
			.collect(Collectors.joining("\n"));
	}

	/**
	 * Returns the list of available agent names.
	 */
	public List<String> getAgentNames() {
		return List.copyOf(this.cards.keySet());
	}

}
