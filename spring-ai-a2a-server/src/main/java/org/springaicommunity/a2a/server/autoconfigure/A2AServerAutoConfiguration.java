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

package org.springaicommunity.a2a.server.autoconfigure;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.util.List;

/**
 * Spring Boot auto-configuration for A2A Server.
 *
 * <p>This auto-configuration automatically enables A2A protocol support when:
 * <ul>
 *   <li>Spring AI ChatClient is on the classpath</li>
 *   <li>A ChatClient bean is present in the application context</li>
 * </ul>
 *
 * <p><strong>What it provides:</strong>
 * <ul>
 *   <li>A2A protocol controllers ({@code /a2a} endpoint)</li>
 *   <li>Agent card metadata endpoint</li>
 *   <li>Task API support for long-running operations</li>
 * </ul>
 *
 * <p><strong>Usage:</strong>
 * Simply add {@code spring-ai-a2a-server} as a dependency and create a ChatClient bean:
 * <pre>
 * &#64;Bean
 * public ChatClient myAgent(ChatModel chatModel) {
 *     return ChatClient.builder(chatModel)
 *         .defaultSystem("You are a helpful assistant...")
 *         .build();
 * }
 * </pre>
 *
 * The A2A server will automatically expose your agent at {@code http://localhost:PORT/a2a}
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@AutoConfiguration
@ConditionalOnClass(ChatClient.class)
@ComponentScan(basePackages = {
	"org.springaicommunity.a2a.server.controller",
	"org.springaicommunity.a2a.server.repository"
})
public class A2AServerAutoConfiguration {

	/**
	 * Provide default AgentCard if none is configured by the user.
	 *
	 * <p>Users can override this by providing their own AgentCard bean with
	 * custom metadata, capabilities, and supported interfaces.
	 *
	 * @return default agent card
	 */
	@Bean
	@ConditionalOnMissingBean
	public AgentCard agentCard() {
		return AgentCard.builder()
			.name("Spring AI A2A Agent")
			.description("A2A agent powered by Spring AI")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(AgentCapabilities.builder()
				.streaming(false)
				.pushNotifications(false)
				.stateTransitionHistory(false)
				.build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(
				new AgentInterface("JSONRPC", "http://localhost:8080/a2a")))
			.build();
	}

}
