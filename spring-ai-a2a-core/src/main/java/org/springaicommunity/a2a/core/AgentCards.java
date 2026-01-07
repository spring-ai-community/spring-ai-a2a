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

package org.springaicommunity.a2a.core;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;

import java.util.List;

/**
 * Utility class for creating AgentCard instances with sensible defaults.
 *
 * <p>This class eliminates boilerplate when creating AgentCards for common use cases.
 * Instead of manually building AgentCards with repetitive configuration, use the factory
 * methods to create minimal cards that will be fully discovered from the remote agent.
 *
 * <p><strong>Before (verbose):</strong>
 * <pre>
 * AgentCard card = AgentCard.builder()
 *     .name("Weather Agent")
 *     .description("Provides weather information")
 *     .version("1.0.0")
 *     .protocolVersion("0.1.0")
 *     .capabilities(AgentCapabilities.builder().build())
 *     .defaultInputModes(List.of("text"))
 *     .defaultOutputModes(List.of("text"))
 *     .skills(List.of())
 *     .supportedInterfaces(List.of(new AgentInterface("JSONRPC", url)))
 *     .build();
 * </pre>
 *
 * <p><strong>After (concise):</strong>
 * <pre>
 * AgentCard card = AgentCards.minimal("Weather Agent", "Provides weather information", url);
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public final class AgentCards {

	private static final String DEFAULT_VERSION = "1.0.0";

	private static final String DEFAULT_PROTOCOL_VERSION = "0.1.0";

	private static final List<String> DEFAULT_MODES = List.of("text");

	private AgentCards() {
		// Utility class
	}

	/**
	 * Create a minimal AgentCard with just name, description, and URL.
	 * <p>
	 * All other fields use sensible defaults: <ul> <li>version: "1.0.0"</li>
	 * <li>protocolVersion: "0.1.0"</li> <li>capabilities: empty</li> <li>modes: ["text"]</li>
	 * <li>skills: empty</li> <li>interface: JSONRPC</li> </ul>
	 * <p>
	 * The full agent card will be discovered from the remote agent during execution.
	 * @param name the agent name (used for tool naming and logging)
	 * @param description the agent description (shown to LLM as tool description)
	 * @param url the A2A endpoint URL (e.g., "http://localhost:10001/a2a")
	 * @return a minimal AgentCard suitable for client operations
	 */
	public static AgentCard minimal(String name, String description, String url) {
		return AgentCard.builder()
			.name(name)
			.description(description)
			.version(DEFAULT_VERSION)
			.protocolVersion(DEFAULT_PROTOCOL_VERSION)
			.capabilities(AgentCapabilities.builder().build())
			.defaultInputModes(DEFAULT_MODES)
			.defaultOutputModes(DEFAULT_MODES)
			.skills(List.of())
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", url)))
			.build();
	}

	/**
	 * Create a minimal AgentCard with custom capabilities.
	 * @param name the agent name
	 * @param description the agent description
	 * @param url the A2A endpoint URL
	 * @param capabilities the agent capabilities
	 * @return an AgentCard with specified capabilities
	 */
	public static AgentCard withCapabilities(String name, String description, String url,
			AgentCapabilities capabilities) {
		return AgentCard.builder()
			.name(name)
			.description(description)
			.version(DEFAULT_VERSION)
			.protocolVersion(DEFAULT_PROTOCOL_VERSION)
			.capabilities(capabilities)
			.defaultInputModes(DEFAULT_MODES)
			.defaultOutputModes(DEFAULT_MODES)
			.skills(List.of())
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", url)))
			.build();
	}

	/**
	 * Extract the agent URL from an AgentCard.
	 * <p>
	 * This method retrieves the first valid URL from the agent's supported interfaces.
	 * It's commonly used when connecting to remote agents or creating HTTP clients.
	 * @param agentCard the agent card
	 * @return the agent URL
	 * @throws IllegalStateException if the agent has no supported interfaces or no valid URL
	 */
	public static String getAgentUrl(AgentCard agentCard) {
		if (agentCard.supportedInterfaces() == null || agentCard.supportedInterfaces().isEmpty()) {
			throw new IllegalStateException("Agent " + agentCard.name() + " has no supported interfaces");
		}

		return agentCard.supportedInterfaces()
			.stream()
			.filter(iface -> iface != null && iface.url() != null)
			.map(AgentInterface::url)
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("Agent " + agentCard.name() + " has no valid URL"));
	}

	/**
	 * Check if the agent supports streaming responses.
	 * <p>
	 * Returns true if the agent's capabilities indicate streaming support, false otherwise.
	 * Agents without defined capabilities are assumed not to support streaming.
	 * @param agentCard the agent card
	 * @return true if streaming is supported, false otherwise
	 */
	public static boolean supportsStreaming(AgentCard agentCard) {
		return agentCard.capabilities() != null && agentCard.capabilities().streaming();
	}

	/**
	 * Check if the agent supports push notifications.
	 * <p>
	 * Returns true if the agent's capabilities indicate push notification support, false
	 * otherwise. Agents without defined capabilities are assumed not to support push
	 * notifications.
	 * @param agentCard the agent card
	 * @return true if push notifications are supported, false otherwise
	 */
	public static boolean supportsPushNotifications(AgentCard agentCard) {
		return agentCard.capabilities() != null && agentCard.capabilities().pushNotifications();
	}

}
