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

package org.springaicommunity.a2a.examples.composable.airbnb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springaicommunity.a2a.client.tool.A2AToolCallback;

import java.time.Duration;
import java.util.Map;

/**
 * Configuration for A2A remote agent tools.
 *
 * <p>This demonstrates the current recommended pattern for integrating remote A2A agents:
 * <ul>
 *   <li>Use {@link A2AToolCallback} to wrap remote A2A agents as Spring AI ToolCallbacks</li>
 *   <li>Register multiple remote agents via a Map of URLs</li>
 *   <li>LLM autonomously decides when to delegate to remote agents</li>
 *   <li>Framework handles A2A protocol communication automatically</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Configuration
public class A2AToolConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(A2AToolConfiguration.class);

	/**
	 * Register remote A2A agents as a unified ToolCallback.
	 *
	 * <p>This creates a single tool that can delegate to multiple remote A2A agents
	 * based on the {@code subagent_type} parameter provided by the LLM.
	 *
	 * <p><strong>How it works:</strong>
	 * <ol>
	 *   <li>LLM sees one "A2AAgent" tool in its tool list</li>
	 *   <li>LLM calls tool with parameters: description, prompt, subagent_type</li>
	 *   <li>A2AToolCallback routes to correct URL based on subagent_type</li>
	 *   <li>Sends A2A protocol request to remote agent over HTTP</li>
	 *   <li>Returns remote agent's response to LLM</li>
	 * </ol>
	 *
	 * @param accommodationAgentUrl URL of the accommodation agent (from application.yml)
	 * @return ToolCallback that delegates to remote A2A agents
	 */
	@Bean
	public ToolCallback a2aRemoteAgentTool(
			@Value("${a2a.agents.accommodation.url:http://localhost:10002/a2a}") String accommodationAgentUrl) {

		logger.info("Configuring A2A remote agent tool");
		logger.info("  • accommodation → {}", accommodationAgentUrl);

		// Map of agent type to A2A endpoint URL
		Map<String, String> agentUrls = Map.of(
			"accommodation", accommodationAgentUrl
			// Add more remote agents here as needed
			// "weather", weatherAgentUrl,
			// "transportation", transportationAgentUrl
		);

		return new A2AToolCallback(agentUrls, Duration.ofMinutes(2));
	}
}
