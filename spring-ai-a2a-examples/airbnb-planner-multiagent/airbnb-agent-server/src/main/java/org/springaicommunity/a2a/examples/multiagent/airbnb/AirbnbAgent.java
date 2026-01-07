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

package org.springaicommunity.a2a.examples.multiagent.airbnb;

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
import org.springaicommunity.a2a.server.agentexecution.SpringAIAgentExecutor;

/**
 * Airbnb Agent Server Application.
 *
 * <p>
 * This application demonstrates how to create an A2A agent server using the Spring Boot Starter:
 * <ul>
 * <li>Uses Spring Boot auto-configuration via {@code spring-boot-starter-spring-ai-a2a}</li>
 * <li>Agent card configured via {@code application.properties}</li>
 * <li>Implements {@link SpringAIAgentExecutor} to provide agent logic</li>
 * <li>A2A endpoints automatically exposed at {@code /a2a}</li>
 * </ul>
 *
 * <p>
 * Environment variables required:
 * <ul>
 * <li>OPENAI_API_KEY - Your OpenAI API key</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootApplication
public class AirbnbAgent {

	public static void main(String[] args) {
		SpringApplication.run(AirbnbAgent.class, args);
	}

	/**
	 * Create the SpringAIAgentExecutor bean.
	 * <p>
	 * The Spring Boot Starter will automatically detect this bean and create the A2AAgentServer.
	 * The AgentCard configuration is loaded from application.properties.
	 * @param chatClient the ChatClient for LLM interactions
	 * @return the SpringAIAgentExecutor
	 */
	@Bean
	public SpringAIAgentExecutor agentExecutor(@Autowired(required = false) ChatClient chatClient) {
		return new AirbnbAgentExecutor(chatClient);
	}

	/**
	 * OpenAI API bean.
	 * @return OpenAiApi instance
	 */
	@Bean
	@ConditionalOnProperty(name = "OPENAI_API_KEY")
	public OpenAiApi openAiApi() {
		return OpenAiApi.builder().apiKey(getApiKey()).build();
	}

	/**
	 * OpenAI ChatModel bean.
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
	 * ChatClient bean.
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
