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

package org.springaicommunity.a2a.starter;

import io.a2a.spec.AgentCard;
import io.a2a.spec.Part;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;
import org.springaicommunity.a2a.server.A2AAgentServer;
import org.springaicommunity.a2a.server.agentexecution.DefaultSpringAIAgentExecutor;
import org.springaicommunity.a2a.server.agentexecution.SpringAIAgentExecutor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link A2AAutoConfiguration}.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
class A2AAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(A2AAutoConfiguration.class));

	@Test
	void autoConfigurationRequiresMandatoryProperties() {
		// Without agent name and description, configuration should fail
		this.contextRunner.run(context -> {
			// AgentCard bean creation will fail due to missing properties
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining("spring.ai.a2a.agent.name must be configured");
		});
	}

	@Test
	void autoConfigurationRequiresAgentNameAndDescription() {
		this.contextRunner.withPropertyValues("spring.ai.a2a.server.enabled=true").run(context -> {
			assertThat(context).hasFailed();
			assertThat(context.getStartupFailure()).hasMessageContaining("spring.ai.a2a.agent.name must be configured");
		});
	}

	@Test
	void autoConfigurationCreatesAgentCardFromProperties() {
		this.contextRunner
			.withPropertyValues("spring.ai.a2a.server.enabled=true", "spring.ai.a2a.agent.name=Test Agent",
					"spring.ai.a2a.agent.description=A test agent", "spring.ai.a2a.agent.version=2.0.0",
					"spring.ai.a2a.agent.protocol-version=0.2.0",
					"spring.ai.a2a.agent.capabilities.streaming=false",
					"spring.ai.a2a.agent.capabilities.push-notifications=true", "server.port=9999")
			.run(context -> {
				assertThat(context).hasBean("agentCard");
				AgentCard agentCard = context.getBean(AgentCard.class);

				assertThat(agentCard.name()).isEqualTo("Test Agent");
				assertThat(agentCard.description()).isEqualTo("A test agent");
				assertThat(agentCard.version()).isEqualTo("2.0.0");
				assertThat(agentCard.protocolVersion()).isEqualTo("0.2.0");
				assertThat(agentCard.capabilities().streaming()).isFalse();
				assertThat(agentCard.capabilities().pushNotifications()).isTrue();
				assertThat(agentCard.supportedInterfaces()).hasSize(1);
				assertThat(agentCard.supportedInterfaces().get(0).url()).contains("9999");
			});
	}

	@Test
	void autoConfigurationUsesDefaultValues() {
		this.contextRunner
			.withPropertyValues("spring.ai.a2a.server.enabled=true", "spring.ai.a2a.agent.name=Test Agent",
					"spring.ai.a2a.agent.description=A test agent")
			.run(context -> {
				assertThat(context).hasBean("agentCard");
				AgentCard agentCard = context.getBean(AgentCard.class);

				assertThat(agentCard.version()).isEqualTo("1.0.0");
				assertThat(agentCard.protocolVersion()).isEqualTo("0.1.0");
				assertThat(agentCard.capabilities().streaming()).isTrue();
				assertThat(agentCard.capabilities().pushNotifications()).isFalse();
				assertThat(agentCard.capabilities().stateTransitionHistory()).isFalse();
			});
	}

	@Test
	void autoConfigurationCreatesA2AAgentServerWhenExecutorPresent() {
		this.contextRunner
			.withPropertyValues("spring.ai.a2a.server.enabled=true", "spring.ai.a2a.agent.name=Test Agent",
					"spring.ai.a2a.agent.description=A test agent")
			.withUserConfiguration(TestAgentExecutorConfiguration.class)
			.run(context -> {
				assertThat(context).hasBean("agentCard");
				assertThat(context).hasBean("a2aAgentServer");

				A2AAgentServer server = context.getBean(A2AAgentServer.class);
				assertThat(server).isNotNull();
				assertThat(server.getAgentCard().name()).isEqualTo("Test Agent");
			});
	}

	@Test
	void autoConfigurationDoesNotCreateServerWithoutExecutor() {
		this.contextRunner
			.withPropertyValues("spring.ai.a2a.server.enabled=true", "spring.ai.a2a.agent.name=Test Agent",
					"spring.ai.a2a.agent.description=A test agent")
			.run(context -> {
				assertThat(context).hasBean("agentCard");
				assertThat(context).doesNotHaveBean(A2AAgentServer.class);
			});
	}

	@Test
	void autoConfigurationCanBeDisabled() {
		this.contextRunner
			.withPropertyValues("spring.ai.a2a.server.enabled=false", "spring.ai.a2a.agent.name=Test Agent",
					"spring.ai.a2a.agent.description=A test agent")
			.withUserConfiguration(TestAgentExecutorConfiguration.class)
			.run(context -> {
				assertThat(context).doesNotHaveBean(AgentCard.class);
				assertThat(context).doesNotHaveBean(A2AAgentServer.class);
			});
	}

	@Test
	void autoConfigurationRespectsCustomAgentCard() {
		this.contextRunner
			.withPropertyValues("spring.ai.a2a.server.enabled=true", "spring.ai.a2a.agent.name=Test Agent",
					"spring.ai.a2a.agent.description=A test agent")
			.withUserConfiguration(CustomAgentCardConfiguration.class, TestAgentExecutorConfiguration.class)
			.run(context -> {
				assertThat(context).hasBean("agentCard");
				AgentCard agentCard = context.getBean(AgentCard.class);

				// Should use custom agent card, not auto-configured one
				assertThat(agentCard.name()).isEqualTo("Custom Agent");
			});
	}

	@Test
	void autoConfigurationRespectsCustomServer() {
		this.contextRunner
			.withPropertyValues("spring.ai.a2a.server.enabled=true", "spring.ai.a2a.agent.name=Test Agent",
					"spring.ai.a2a.agent.description=A test agent")
			.withUserConfiguration(CustomServerConfiguration.class, TestAgentExecutorConfiguration.class)
			.run(context -> {
				assertThat(context).hasBean("a2aAgentServer");
				A2AAgentServer server = context.getBean(A2AAgentServer.class);

				// Should use custom server
				assertThat(server.getAgentCard().name()).isEqualTo("Custom Server Agent");
			});
	}

	@Configuration
	static class TestAgentExecutorConfiguration {

		@Bean
		public SpringAIAgentExecutor testAgentExecutor() {
			return new DefaultSpringAIAgentExecutor(null) {
				@Override
				public String getSystemPrompt() {
					return "Test system prompt";
				}

				@Override
				public A2AResponse executeSynchronous(A2ARequest request) {
					return A2AResponse.text("Test response");
				}

				@Override
				public List<Part<?>> onExecute(String userInput,
						io.a2a.server.agentexecution.RequestContext context,
						io.a2a.server.tasks.TaskUpdater taskUpdater) {
					return List.of();
				}
			};
		}

	}

	@Configuration
	static class CustomAgentCardConfiguration {

		@Bean
		public AgentCard agentCard() {
			return AgentCard.builder()
				.name("Custom Agent")
				.description("Custom description")
				.version("1.0.0")
				.protocolVersion("0.1.0")
				.capabilities(io.a2a.spec.AgentCapabilities.builder().streaming(true).build())
				.defaultInputModes(List.of("text"))
				.defaultOutputModes(List.of("text"))
				.supportedInterfaces(List.of(new io.a2a.spec.AgentInterface("JSONRPC", "http://localhost:8080/a2a")))
				.skills(List.of())
				.build();
		}

	}

	@Configuration
	static class CustomServerConfiguration {

		@Bean
		public AgentCard agentCard() {
			return AgentCard.builder()
				.name("Custom Server Agent")
				.description("Custom server description")
				.version("1.0.0")
				.protocolVersion("0.1.0")
				.capabilities(io.a2a.spec.AgentCapabilities.builder().streaming(true).build())
				.defaultInputModes(List.of("text"))
				.defaultOutputModes(List.of("text"))
				.supportedInterfaces(List.of(new io.a2a.spec.AgentInterface("JSONRPC", "http://localhost:8080/a2a")))
				.skills(List.of())
				.build();
		}

		@Bean
		public A2AAgentServer a2aAgentServer(AgentCard agentCard, SpringAIAgentExecutor executor) {
			return new org.springaicommunity.a2a.server.DefaultA2AAgentServer(agentCard, executor);
		}

	}

}
