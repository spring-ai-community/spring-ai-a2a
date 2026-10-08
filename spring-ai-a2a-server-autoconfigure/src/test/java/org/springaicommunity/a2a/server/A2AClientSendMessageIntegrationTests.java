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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientEvent;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.TaskUpdateEvent;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip tests that call the auto-configured A2A server with the A2A Java SDK client
 * and with raw JSON-RPC requests.
 *
 * <p>
 * The agent echoes the received text without calling an LLM, so no API key is required.
 * The test application doesn't provide a {@code TaskAuthorizationProvider}, so it sets
 * {@code a2a.authorization.required=false}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
		properties = { "server.port=" + A2AClientSendMessageIntegrationTests.PORT, "spring.ai.openai.api-key=not-used",
				"a2a.authorization.required=false" })
class A2AClientSendMessageIntegrationTests {

	static final int PORT = 58889;

	private static final String AGENT_URL = "http://localhost:" + PORT;

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
	 * Sends a message with the A2A Java SDK client, the same way another agent would.
	 */
	@Test
	void sdkClientSendsMessage() throws Exception {
		Task task = sendMessage(createClient(), "Hello A2A");

		assertThat(task.status().state()).isEqualTo(TaskState.TASK_STATE_COMPLETED);
		assertThat(text(task)).isEqualTo("Hello A2A");
	}

	/**
	 * Reads a task with the A2A Java SDK client after it was created by a message.
	 */
	@Test
	void sdkClientGetsTask() throws Exception {
		Client client = createClient();
		Task createdTask = sendMessage(client, "Hello again");

		Task task = client.getTask(new TaskQueryParams(createdTask.id()));

		assertThat(task.id()).isEqualTo(createdTask.id());
		assertThat(task.status().state()).isEqualTo(TaskState.TASK_STATE_COMPLETED);
		assertThat(text(task)).isEqualTo("Hello again");
	}

	private Client createClient() throws Exception {
		return Client.builder(A2A.getAgentCard(AGENT_URL))
			.clientConfig(new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build())
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.build();
	}

	private Task sendMessage(Client client, String text) throws Exception {

		CompletableFuture<Task> result = new CompletableFuture<>();
		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			Task task = null;
			if (event instanceof TaskEvent taskEvent) {
				task = taskEvent.getTask();
			}
			else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
				task = taskUpdateEvent.getTask();
			}
			if (task != null && task.status().state().isFinal()) {
				result.complete(task);
			}
		};

		client.sendMessage(Message.builder().role(Message.Role.ROLE_USER).parts(List.of(new TextPart(text))).build(),
				List.of(consumer), result::completeExceptionally);

		return result.get(30, TimeUnit.SECONDS);
	}

	private static String text(Task task) {
		return task.artifacts()
			.stream()
			.flatMap(artifact -> artifact.parts().stream())
			.filter(TextPart.class::isInstance)
			.map(part -> ((TextPart) part).text())
			.collect(Collectors.joining());
	}

	/**
	 * Sends the JSON-RPC request in the A2A v1.0 format, as the A2A Java SDK 1.4.0 client
	 * does.
	 */
	@Test
	void jsonRpcV10SendMessage() throws Exception {
		HttpResponse<String> response = post("1.0", """
				{
				  "jsonrpc": "2.0",
				  "id": "1",
				  "method": "SendMessage",
				  "params": {
				    "message": {
				      "messageId": "message-1",
				      "role": "ROLE_USER",
				      "parts": [ { "text": "Hello A2A" } ]
				    }
				  }
				}
				""");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.body()).contains("Hello A2A").doesNotContain("\"error\"");
	}

	/**
	 * Sends the JSON-RPC request in the A2A v0.3 format (no {@code A2A-Version} header).
	 * Still handled by the previous implementation, which can't deserialize the SDK 1.x
	 * types (HTTP 400). Enable this test once A2A v0.3 is supported, e.g. with the SDK's
	 * compat-0.3 modules.
	 */
	@Test
	@Disabled("A2A v0.3 is not supported yet")
	void jsonRpcV03SendMessage() throws Exception {
		HttpResponse<String> response = post(null, """
				{
				  "jsonrpc": "2.0",
				  "id": "1",
				  "method": "message/send",
				  "params": {
				    "message": {
				      "kind": "message",
				      "messageId": "message-1",
				      "role": "user",
				      "parts": [ { "kind": "text", "text": "Hello A2A" } ]
				    }
				  }
				}
				""");

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.body()).contains("Hello A2A").doesNotContain("\"error\"");
	}

	private static HttpResponse<String> post(String a2aVersion, String body) throws Exception {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(AGENT_URL + "/"))
			.header("Content-Type", "application/json")
			.header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body));
		if (a2aVersion != null) {
			request.header("A2A-Version", a2aVersion);
		}
		return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
	}

}
