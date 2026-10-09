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

package org.springaicommunity.a2a.server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for the round-trip tests that call the auto-configured A2A server with the
 * A2A Java SDK clients and with raw JSON-RPC requests.
 *
 * <p>
 * The agent echoes the received text without calling an LLM, so no API key is required.
 * The test application doesn't provide a {@code TaskAuthorizationProvider}, so it sets
 * {@code a2a.authorization.required=false}. All subclasses share the same application
 * context and server.
 *
 * @author Thorben Janssen
 * @since 0.4.0
 */
@SpringBootTest(
		classes = { AbstractA2AServerIntegrationTests.TestApplication.class,
				AbstractA2AServerIntegrationTests.TestConfig.class },
		webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
		properties = { "server.port=" + AbstractA2AServerIntegrationTests.PORT, "spring.ai.openai.api-key=not-used",
				"a2a.authorization.required=false" })
abstract class AbstractA2AServerIntegrationTests {

	static final int PORT = 58889;

	static final String AGENT_URL = "http://localhost:" + PORT;

	@SpringBootApplication
	static class TestApplication {

	}

	@TestConfiguration
	static class TestConfig {

		@Bean
		public ChatClient testChatClient(ChatModel chatModel) {
			return ChatClient.builder(chatModel).build();
		}

		@Bean
		public AgentCard testAgentCard() {
			return AgentCard.builder()
				.name("Echo Agent")
				.description("Returns the received text")
				.version("1.0.0")
				.capabilities(AgentCapabilities.builder().streaming(false).build())
				.defaultInputModes(List.of("text"))
				.defaultOutputModes(List.of("text"))
				.skills(List.of())
				.supportedInterfaces(List.of(new AgentInterface("JSONRPC", AGENT_URL)))
				.build();
		}

		/**
		 * Echoes the text of the received message without calling the LLM.
		 */
		@Bean
		public AgentExecutor testAgentExecutor(ChatClient testChatClient) {
			return new DefaultAgentExecutor(testChatClient, (chatClient, requestContext) -> DefaultAgentExecutor
				.extractTextFromMessage(requestContext.getMessage()));
		}

	}

	/**
	 * Sends a JSON-RPC request. Without {@code a2aVersion}, no {@code A2A-Version} header
	 * is sent, as A2A v0.3 clients do.
	 */
	static HttpResponse<String> post(String a2aVersion, String body) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(AGENT_URL + "/"))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body));
		if (a2aVersion != null) {
			request.header("A2A-Version", a2aVersion);
		}
		return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

	static HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(AGENT_URL + path))
			.header("Accept", "application/json")
			.GET()
			.build();
		return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
	}

	/**
	 * Asserts a JSON-RPC error response with HTTP status 200, the given error code and
	 * the id of the request.
	 */
	static void assertJsonRpcError(HttpResponse<String> response, String id, int code) {
		assertJsonRpcError(response, code);
		assertThat(response.body()).contains("\"id\":\"" + id + "\"");
	}

	/**
	 * Asserts a JSON-RPC error response with HTTP status 200 and the given error code.
	 */
	static void assertJsonRpcError(HttpResponse<String> response, int code) {
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.body()).contains("\"error\"").contains("\"code\":" + code);
	}

	/**
	 * Asserts a successful JSON-RPC response with HTTP status 200 and the id of the
	 * request.
	 */
	static void assertJsonRpcResult(HttpResponse<String> response, String id) {
		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.body()).contains("\"id\":\"" + id + "\"")
			.contains("\"result\"")
			.doesNotContain("\"error\"");
	}

}
