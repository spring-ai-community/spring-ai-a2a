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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;
import org.springaicommunity.a2a.server.A2AServer;
import org.springaicommunity.a2a.server.DefaultA2AServer;
import org.springaicommunity.a2a.server.agentexecution.A2AAgentModel;

import java.util.List;

/**
 * Auto-configuration for Spring AI A2A (Agent-to-Agent) protocol support.
 *
 * <p>
 * This auto-configuration will:
 * <ul>
 * <li>Create an {@link AgentCard} bean based on configuration properties if not already defined</li>
 * <li>Create an {@link A2AServer} bean if an {@link A2AAgentModel} is present</li>
 * <li>Expose A2A protocol endpoints for agent-to-agent communication</li>
 * </ul>
 *
 * <p>
 * Configuration example in application.yml:
 * <pre>
 * spring:
 *   ai:
 *     a2a:
 *       server:
 *         enabled: true
 *         base-path: /a2a
 *       agent:
 *         name: My Agent
 *         description: A sample agent
 *         version: 1.0.0
 *         protocol-version: 0.1.0
 *         capabilities:
 *           streaming: true
 *           push-notifications: false
 *           state-transition-history: false
 * </pre>
 *
 * <p>
 * Users only need to provide an {@link A2AAgentModel} bean and the starter will automatically
 * configure the A2A server.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(A2AProperties.class)
@ConditionalOnProperty(prefix = "spring.ai.a2a.server", name = "enabled", havingValue = "true", matchIfMissing = true)
public class A2AServerAutoConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(A2AServerAutoConfiguration.class);

	/**
	 * Create the AgentCard bean from configuration properties.
	 * <p>
	 * This method creates an {@link AgentCard} using properties-based configuration from
	 * application.yml. For advanced use cases requiring dynamic configuration, users can
	 * define a custom {@link AgentCard} bean which will take precedence due to
	 * {@code @ConditionalOnMissingBean}.
	 * <p>
	 * <strong>Simple Configuration (95% of use cases):</strong>
	 * <pre>
	 * spring:
	 *   ai:
	 *     a2a:
	 *       agent:
	 *         name: "My Agent"
	 *         description: "Agent description"
	 *         skills:
	 *           - skill-1
	 *           - skill-2
	 * </pre>
	 * <p>
	 * <strong>Advanced Configuration (5% of use cases):</strong>
	 * <pre>
	 * {@code @Bean}
	 * public AgentCard agentCard(SkillRegistry registry, {@code @Value}("${server.port}") int port) {
	 *     return AgentCard.builder()
	 *         .name("Dynamic Agent")
	 *         .skills(registry.getAvailableSkills())
	 *         .build();
	 * }
	 * </pre>
	 * @param properties the A2A configuration properties
	 * @param serverPort the server port
	 * @return the configured AgentCard
	 * @throws IllegalStateException if required fields (name, description) are not set
	 */
	@Bean
	@ConditionalOnMissingBean
	public AgentCard agentCard(A2AProperties properties, @Value("${server.port:8080}") int serverPort) {

		A2AProperties.Agent agentProps = properties.getAgent();
		String basePath = properties.getServer().getBasePath();

		// Validate required fields
		if (!StringUtils.hasText(agentProps.getName())) {
			throw new IllegalStateException(
					"Agent name is required. Set 'spring.ai.a2a.agent.name' in application.yml");
		}

		if (!StringUtils.hasText(agentProps.getDescription())) {
			throw new IllegalStateException(
					"Agent description is required. Set 'spring.ai.a2a.agent.description' in application.yml");
		}

		// Construct agent URL
		String agentUrl = "http://localhost:" + serverPort + basePath;

		// Build AgentCard directly from properties
		AgentCard agentCard = AgentCard.builder()
			.name(agentProps.getName())
			.description(agentProps.getDescription())
			.version(agentProps.getVersion())
			.protocolVersion(agentProps.getProtocolVersion())
			.capabilities(AgentCapabilities.builder()
				.streaming(agentProps.getCapabilities().isStreaming())
				.pushNotifications(agentProps.getCapabilities().isPushNotifications())
				.stateTransitionHistory(agentProps.getCapabilities().isStateTransitionHistory())
				.build())
			.defaultInputModes(agentProps.getDefaultInputModes())
			.defaultOutputModes(agentProps.getDefaultOutputModes())
			.skills(agentProps.getSkills().stream()
				.map(skillId -> io.a2a.spec.AgentSkill.builder()
					.id(skillId)
					.name(skillId)
					.description("")
					.tags(List.of())
					.build())
				.toList())
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", agentUrl)))
			.skills(List.of()) // Empty skills list by default
			.build();

		logger.info("Created AgentCard for agent '{}' with URL: {}", agentCard.name(), agentUrl);

		return agentCard;
	}

	/**
	 * Create the A2A Agent Server bean if an A2AAgentModel is present.
	 * <p>
	 * This enforces that Spring AI applications must use {@link A2AAgentModel}
	 * instead of the generic {@link io.a2a.server.agentexecution.AgentExecutor} from the
	 * A2A SDK. This ensures proper integration with Spring AI's lifecycle and capabilities.
	 * @param agentCard the agent card
	 * @param agentModel the A2A agent model
	 * @return the A2A agent server
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(A2AAgentModel.class)
	public DefaultA2AServer a2aAgentServer(AgentCard agentCard, A2AAgentModel agentModel) {
		logger.info("Creating A2AServer for agent: {}", agentCard.name());

		return new DefaultA2AServer(agentCard, agentModel);
	}

}
