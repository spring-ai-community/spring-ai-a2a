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

package org.springaicommunity.a2a.integration;

import java.util.List;

import org.springaicommunity.a2a.server.agentexecution.A2AAgentModel;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;

import org.springaicommunity.a2a.server.A2AServer;
import org.springaicommunity.a2a.server.DefaultA2AServer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.ApiKey;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Test Spring Boot application for A2A integration tests.
 *
 * <p>
 * This application demonstrates A2A agent configuration using the AgentModel pattern:
 * <ul>
 * <li>Starts an embedded server on a fixed port (58888)</li>
 * <li>Creates agent using A2AAgentModel.builder() pattern</li>
 * <li>Creates {@link AgentCard} with skills</li>
 * <li>Exposes A2A endpoint at /a2a</li>
 * <li>Uses ChatModel from OpenAI when OPENAI_API_KEY is set</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class TestA2AApplication {

	public static void main(String[] args) {
		SpringApplication.run(TestA2AApplication.class, args);
	}

	/**
	 * Register the test agent using the A2AAgentModel builder pattern.
	 * <p>
	 * This demonstrates the new builder-based approach for creating agents.
	 * The agent is created declaratively using A2AAgentModel.builder().
	 * @param chatModel the chat model for LLM interactions
	 * @return A2AAgentModel test agent
	 */
	@Bean
	public A2AAgentModel agentExecutor(org.springframework.ai.chat.model.ChatModel chatModel) {
		return A2AAgentModel.builder()
			.chatClient(ChatClient.builder(chatModel).build())
			.systemPrompt(getSystemPrompt())
			.build();
	}

	/**
	 * Get the system prompt that defines the test agent's behavior.
	 *
	 * @return the system prompt for the test agent
	 */
	private String getSystemPrompt() {
		return """
			You are a test echo agent for integration testing.

			You support the following skills based on keywords in the user input:
			- ECHO (default): Respond with "Echo: " followed by the user's message
			- UPPERCASE: Convert the entire message to uppercase and return it
			- ANALYZE: Provide a brief analysis of the text

			Important formatting rules:
			- For ECHO: Always prefix with "Echo: " (case-sensitive)
			- For UPPERCASE: Return ONLY the uppercased text, no prefix
			- For ANALYZE: Provide a brief analysis

			Detect which skill to use based on keywords in the user input:
			- If input contains "uppercase" or "UPPERCASE" → use UPPERCASE skill
			- If input contains "analyze" or "ANALYZE" → use ANALYZE skill
			- Otherwise → use ECHO skill (default)
			""";
	}

	/**
	 * Create AgentCard with agent metadata and skills.
	 * <p>
	 * Uses a fixed port (58888) configured in application.properties to allow static
	 * agent card configuration.
	 * @return AgentCard with agent metadata and skills
	 */
	@Bean
	public AgentCard agentCard() {
		return AgentCard.builder()
			.name("Test Echo Agent")
			.description("Simple agent that echoes messages for testing")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(AgentCapabilities.builder()
				.streaming(true)
				.pushNotifications(false)
				.stateTransitionHistory(false)
				.build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", "http://localhost:58888/a2a")))
			.skills(List.of(
					AgentSkill.builder()
						.id("echo")
						.name("Echo")
						.description("Echoes back the input message")
						.tags(List.of("test", "echo"))
						.build(),
					AgentSkill.builder()
						.id("uppercase")
						.name("Uppercase")
						.description("Converts text to uppercase")
						.tags(List.of("test", "transformation"))
						.build(),
					AgentSkill.builder()
						.id("stream")
						.name("Stream")
						.description("Returns words as a stream")
						.tags(List.of("test", "streaming"))
						.build(),
					AgentSkill.builder()
						.id("ai-analyze")
						.name("Analyze")
						.description("Analyzes text using ChatClient")
						.tags(List.of("test", "ai"))
						.build()))
			.build();
	}

	/**
	 * Create A2AServer that exposes the agent via HTTP endpoints.
	 * @param agentCard the agent card
	 * @param agentExecutor the agent executor (A2AAgentModel)
	 * @return A2AServer instance
	 */
	@Bean
	public A2AServer a2aAgentServer(AgentCard agentCard, A2AAgentModel agentExecutor) {
		return new DefaultA2AServer(agentCard, agentExecutor);
	}

	/**
	 * OpenAI API bean.
	 *
	 * <p>
	 * This bean is only created when the OPENAI_API_KEY environment variable is set.
	 * @return OpenAiApi instance configured with the API key
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public OpenAiApi openAiApi() {
		return OpenAiApi.builder().apiKey(getApiKey()).build();
	}

	/**
	 * OpenAI ChatModel bean.
	 *
	 * <p>
	 * This bean is only created when the OPENAI_API_KEY environment variable is set.
	 * @param api the OpenAiApi instance
	 * @return OpenAiChatModel instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public OpenAiChatModel openAiChatModel(OpenAiApi api) {
		return OpenAiChatModel.builder()
			.openAiApi(api)
			.defaultOptions(OpenAiChatOptions.builder().model(OpenAiApi.ChatModel.GPT_4_O_MINI).build())
			.build();
	}

	/**
	 * ChatClient bean for the test agent to use.
	 *
	 * <p>
	 * This bean is only created when the OPENAI_API_KEY environment variable is set.
	 * @param chatModel the OpenAiChatModel instance
	 * @return ChatClient instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public ChatClient chatClient(OpenAiChatModel chatModel) {
		return ChatClient.builder(chatModel).build();
	}

	private ApiKey getApiKey() {
		String apiKey = System.getenv("OPENAI_API_KEY");
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException(
					"You must provide an API key. Put it in an environment variable under the name OPENAI_API_KEY");
		}
		return new SimpleApiKey(apiKey);
	}

}
