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

import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
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
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Message;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskQueryParams;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Round-trip tests for A2A v1.0 requests (header {@code A2A-Version: 1.0}) with the A2A
 * Java SDK client and with raw JSON-RPC requests.
 *
 * @author Thorben Janssen
 * @since 0.4.0
 */
class A2AProtocolV10IntegrationTests extends AbstractA2AServerIntegrationTests {

	private static final String V1_0 = "1.0";

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

	/**
	 * Loads the agent card, sends a message and reads the task with the A2A Java SDK
	 * client, the same way an A2A v1.0 agent would. This is the A2A v1.0 counterpart of
	 * {@link A2AProtocolV03IntegrationTests#sdkClientLoadsAgentCardAndSendsMessage()}.
	 */
	@Test
	void sdkClientLoadsAgentCardAndSendsMessage() throws Exception {
		AgentCard agentCard = A2A.getAgentCard(AGENT_URL);
		assertThat(agentCard.supportedInterfaces()).extracting(AgentInterface::protocolBinding, AgentInterface::url)
			.containsExactly(tuple("JSONRPC", AGENT_URL));

		Client client = createClient(agentCard);
		Task createdTask = sendMessage(client, "Hello A2A v1.0");

		assertThat(createdTask.status().state()).isEqualTo(TaskState.TASK_STATE_COMPLETED);
		assertThat(text(createdTask)).isEqualTo("Hello A2A v1.0");

		Task task = client.getTask(new TaskQueryParams(createdTask.id()));

		assertThat(task.id()).isEqualTo(createdTask.id());
		assertThat(task.status().state()).isEqualTo(TaskState.TASK_STATE_COMPLETED);
		assertThat(text(task)).isEqualTo("Hello A2A v1.0");
	}

	/**
	 * Sends the JSON-RPC request in the A2A v1.0 format, as the A2A Java SDK 1.4.0 client
	 * does.
	 */
	@Test
	void jsonRpcSendMessage() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(V1_0, """
				{
				  "jsonrpc": "2.0",
				  "id": "%s",
				  "method": "SendMessage",
				  "params": {
				    "message": {
				      "messageId": "message-1",
				      "role": "ROLE_USER",
				      "parts": [ { "text": "Hello A2A" } ]
				    }
				  }
				}
				""".formatted(id));

		assertJsonRpcResult(response, id);
		assertThat(response.body()).contains("Hello A2A");
	}

	/**
	 * Patch versions are ignored, so {@code A2A-Version: 1.0.0} is handled as A2A v1.0.
	 */
	@Test
	void jsonRpcSendMessageWithPatchVersion() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post("1.0.0", """
				{
				  "jsonrpc": "2.0",
				  "id": "%s",
				  "method": "SendMessage",
				  "params": {
				    "message": {
				      "messageId": "message-1",
				      "role": "ROLE_USER",
				      "parts": [ { "text": "Hello A2A" } ]
				    }
				  }
				}
				""".formatted(id));

		assertJsonRpcResult(response, id);
		assertThat(response.body()).contains("Hello A2A").contains("TASK_STATE_COMPLETED");
	}

	/**
	 * Unsupported or invalid protocol versions are answered with a
	 * {@code VersionNotSupportedError}.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "2.0", "1.1", "0.2", "1", "abc" })
	void jsonRpcUnsupportedVersion(String version) throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(version, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "GetTask", "params": { "id": "task-1" } }
				""".formatted(id));

		assertJsonRpcError(response, id, -32009);
		assertThat(response.body()).contains("is not supported. Supported versions: [0.3, 1.0]")
			.contains("VERSION_NOT_SUPPORTED");
	}

	/**
	 * The {@code VersionNotSupportedError} keeps a numeric request id unchanged.
	 */
	@Test
	void jsonRpcUnsupportedVersionKeepsNumericId() throws Exception {
		HttpResponse<String> response = post("2.0", """
				{ "jsonrpc": "2.0", "id": 3000000000, "method": "GetTask", "params": { "id": "task-1" } }
				""");

		assertJsonRpcError(response, -32009);
		assertThat(response.body()).contains("\"id\":3000000000");
	}

	/**
	 * Lists the tasks, including a task that was created before.
	 */
	@Test
	void jsonRpcListTasks() throws Exception {
		Task createdTask = sendMessage(createClient(), "Hello list");
		String id = UUID.randomUUID().toString();

		HttpResponse<String> response = post(V1_0, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "ListTasks", "params": {} }
				""".formatted(id));

		assertJsonRpcResult(response, id);
		assertThat(response.body()).contains(createdTask.id());
	}

	/**
	 * Reading an unknown task is answered with a {@code TaskNotFoundError}.
	 */
	@Test
	void jsonRpcGetUnknownTask() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(V1_0, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "GetTask", "params": { "id": "%s" } }
				""".formatted(id, UUID.randomUUID()));

		assertJsonRpcError(response, id, -32001);
	}

	/**
	 * Canceling a completed task is answered with a {@code TaskNotCancelableError}.
	 */
	@Test
	void jsonRpcCancelCompletedTask() throws Exception {
		Task createdTask = sendMessage(createClient(), "Hello cancel");
		String id = UUID.randomUUID().toString();

		HttpResponse<String> response = post(V1_0, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "CancelTask", "params": { "id": "%s" } }
				""".formatted(id, createdTask.id()));

		assertJsonRpcError(response, id, -32002);
	}

	/**
	 * The agent card doesn't declare push notifications, so push notification configs are
	 * answered with a {@code PushNotificationNotSupportedError}.
	 */
	@Test
	void jsonRpcPushNotificationsAreNotSupported() throws Exception {
		Task createdTask = sendMessage(createClient(), "Hello push");
		String id = UUID.randomUUID().toString();

		HttpResponse<String> response = post(V1_0, """
				{
				  "jsonrpc": "2.0",
				  "id": "%s",
				  "method": "CreateTaskPushNotificationConfig",
				  "params": { "taskId": "%s", "url": "http://localhost/callback" }
				}
				""".formatted(id, createdTask.id()));

		assertJsonRpcError(response, id, -32003);
	}

	/**
	 * Unknown A2A v1.0 methods are answered with a JSON-RPC error response.
	 */
	@Test
	void jsonRpcUnknownMethod() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(V1_0, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "UnknownMethod", "params": {} }
				""".formatted(id));

		assertJsonRpcError(response, id, -32601);
	}

	/**
	 * Streaming isn't supported yet, so A2A v1.0 streaming requests are answered with a
	 * JSON-RPC error response.
	 */
	@Test
	void jsonRpcStreamingIsNotSupported() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(V1_0, """
				{
				  "jsonrpc": "2.0",
				  "id": "%s",
				  "method": "SendStreamingMessage",
				  "params": {
				    "message": {
				      "messageId": "message-1",
				      "role": "ROLE_USER",
				      "parts": [ { "text": "Hello A2A" } ]
				    }
				  }
				}
				""".formatted(id));

		assertJsonRpcError(response, id, -32004);
	}

	/**
	 * Invalid parameters are answered with an {@code InvalidParamsError}.
	 */
	@Test
	void jsonRpcInvalidParams() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(V1_0, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "SendMessage", "params": { "message": "invalid" } }
				""".formatted(id));

		assertJsonRpcError(response, id, -32602);
	}

	/**
	 * A request with an invalid id is answered with an {@code InvalidRequestError}.
	 */
	@Test
	void jsonRpcInvalidRequest() throws Exception {
		HttpResponse<String> response = post(V1_0, """
				{ "jsonrpc": "2.0", "id": { "invalid": true }, "method": "GetTask", "params": {} }
				""");

		assertJsonRpcError(response, -32600);
	}

	/**
	 * Invalid JSON is answered with a JSON-RPC parse error.
	 */
	@Test
	void jsonRpcInvalidJson() throws Exception {
		HttpResponse<String> response = post(V1_0, "{ \"jsonrpc\": ");

		assertJsonRpcError(response, -32700);
	}

	private Client createClient() throws Exception {
		return createClient(A2A.getAgentCard(AGENT_URL));
	}

	private Client createClient(AgentCard agentCard) throws Exception {
		return Client.builder(agentCard)
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

}
