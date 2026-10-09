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
import java.util.UUID;
import java.util.stream.Collectors;

import org.a2aproject.sdk.compat03.client.http.A2ACardResolver_v0_3;
import org.a2aproject.sdk.compat03.client.transport.jsonrpc.JSONRPCTransport_v0_3;
import org.a2aproject.sdk.compat03.spec.AgentCard_v0_3;
import org.a2aproject.sdk.compat03.spec.EventKind_v0_3;
import org.a2aproject.sdk.compat03.spec.MessageSendParams_v0_3;
import org.a2aproject.sdk.compat03.spec.Message_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskQueryParams_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskState_v0_3;
import org.a2aproject.sdk.compat03.spec.Task_v0_3;
import org.a2aproject.sdk.compat03.spec.TextPart_v0_3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip tests for A2A v0.3 requests (no {@code A2A-Version} header) with the A2A
 * v0.3 client of the A2A Java SDK's compat-0.3 modules and with raw JSON-RPC requests.
 *
 * @author Thorben Janssen
 * @since 0.4.0
 */
class A2AProtocolV03IntegrationTests extends AbstractA2AServerIntegrationTests {

	/**
	 * Loads the agent card, sends a message and reads the task with the A2A v0.3 client
	 * of the A2A Java SDK's compat-0.3 modules, the same way an A2A v0.3 agent would. The
	 * agent card only defines {@code supportedInterfaces}, so this also verifies that the
	 * server adds the {@code url} that A2A v0.3 clients require.
	 */
	@Test
	void sdkClientLoadsAgentCardAndSendsMessage() throws Exception {
		AgentCard_v0_3 agentCard = new A2ACardResolver_v0_3(AGENT_URL).getAgentCard();
		assertThat(agentCard.url()).isEqualTo(AGENT_URL);

		JSONRPCTransport_v0_3 transport = new JSONRPCTransport_v0_3(agentCard);
		Task_v0_3 createdTask = sendMessage(transport, "Hello A2A v0.3");

		assertThat(createdTask.status().state()).isEqualTo(TaskState_v0_3.COMPLETED);
		assertThat(text(createdTask)).isEqualTo("Hello A2A v0.3");

		Task_v0_3 task = transport.getTask(new TaskQueryParams_v0_3(createdTask.id()), null);

		assertThat(task.id()).isEqualTo(createdTask.id());
		assertThat(task.status().state()).isEqualTo(TaskState_v0_3.COMPLETED);
		assertThat(text(task)).isEqualTo("Hello A2A v0.3");
	}

	/**
	 * Both agent card endpoints contain the {@code url} that A2A v0.3 clients require.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "/.well-known/agent-card.json", "/card" })
	void agentCardContainsUrl(String path) throws Exception {
		HttpResponse<String> response = get(path);

		assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
		assertThat(response.body()).contains("\"url\":\"" + AGENT_URL + "\"");
	}

	/**
	 * Sends the JSON-RPC request in the A2A v0.3 format (no {@code A2A-Version} header),
	 * as A2A v0.3 clients do.
	 */
	@Test
	void jsonRpcSendMessage() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(null, sendMessageRequest(id));

		assertJsonRpcResult(response, id);
		assertThat(response.body()).contains("Hello A2A");
	}

	/**
	 * Requests with the header {@code A2A-Version: 0.3}, with or without patch version,
	 * are handled as A2A v0.3 requests as well.
	 */
	@ParameterizedTest
	@ValueSource(strings = { "0.3", "0.3.0", " " })
	void jsonRpcSendMessageWithVersionHeader(String version) throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(version, sendMessageRequest(id));

		assertJsonRpcResult(response, id);
		assertThat(response.body()).contains("Hello A2A").contains("\"kind\":\"task\"");
	}

	/**
	 * Reading an unknown task is answered with a {@code TaskNotFoundError}.
	 */
	@Test
	void jsonRpcGetUnknownTask() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(null, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "tasks/get", "params": { "id": "%s" } }
				""".formatted(id, UUID.randomUUID()));

		assertJsonRpcError(response, id, -32001);
	}

	/**
	 * Canceling a completed task is answered with a {@code TaskNotCancelableError}.
	 */
	@Test
	void jsonRpcCancelCompletedTask() throws Exception {
		Task_v0_3 createdTask = sendMessage(new JSONRPCTransport_v0_3(AGENT_URL), "Hello cancel");
		String id = UUID.randomUUID().toString();

		HttpResponse<String> response = post(null, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "tasks/cancel", "params": { "id": "%s" } }
				""".formatted(id, createdTask.id()));

		assertJsonRpcError(response, id, -32002);
	}

	/**
	 * The agent card doesn't declare push notifications, so push notification configs are
	 * answered with a {@code PushNotificationNotSupportedError}.
	 */
	@Test
	void jsonRpcPushNotificationsAreNotSupported() throws Exception {
		Task_v0_3 createdTask = sendMessage(new JSONRPCTransport_v0_3(AGENT_URL), "Hello push");
		String id = UUID.randomUUID().toString();

		HttpResponse<String> response = post(null, """
				{
				  "jsonrpc": "2.0",
				  "id": "%s",
				  "method": "tasks/pushNotificationConfig/set",
				  "params": {
				    "taskId": "%s",
				    "pushNotificationConfig": { "url": "http://localhost/callback" }
				  }
				}
				""".formatted(id, createdTask.id()));

		assertJsonRpcError(response, id, -32003);
	}

	/**
	 * Unknown A2A v0.3 methods are answered with a JSON-RPC error response.
	 */
	@Test
	void jsonRpcUnknownMethod() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(null, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "tasks/unknown", "params": {} }
				""".formatted(id));

		assertJsonRpcError(response, id, -32601);
	}

	/**
	 * Streaming isn't supported yet, so A2A v0.3 streaming requests are answered with a
	 * JSON-RPC error response.
	 */
	@Test
	void jsonRpcStreamingIsNotSupported() throws Exception {
		String id = UUID.randomUUID().toString();
		HttpResponse<String> response = post(null, """
				{
				  "jsonrpc": "2.0",
				  "id": "%s",
				  "method": "message/stream",
				  "params": {
				    "message": {
				      "kind": "message",
				      "messageId": "message-1",
				      "role": "user",
				      "parts": [ { "kind": "text", "text": "Hello A2A" } ]
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
		HttpResponse<String> response = post(null, """
				{ "jsonrpc": "2.0", "id": "%s", "method": "message/send", "params": {} }
				""".formatted(id));

		assertJsonRpcError(response, id, -32602);
	}

	/**
	 * Requests without {@code jsonrpc} or {@code method} or with an invalid {@code id}
	 * are answered with an {@code InvalidRequestError}.
	 */
	@ParameterizedTest
	@ValueSource(strings = { """
			{ "id": "1", "method": "tasks/get", "params": {} }
			""", """
			{ "jsonrpc": "2.0", "id": "1", "params": {} }
			""", """
			{ "jsonrpc": "2.0", "id": { "invalid": true }, "method": "tasks/get", "params": {} }
			""" })
	void jsonRpcInvalidRequest(String body) throws Exception {
		HttpResponse<String> response = post(null, body);

		assertJsonRpcError(response, -32600);
	}

	/**
	 * Invalid JSON is answered with a JSON-RPC parse error.
	 */
	@Test
	void jsonRpcInvalidJson() throws Exception {
		HttpResponse<String> response = post(null, "{ \"jsonrpc\": ");

		assertJsonRpcError(response, -32700);
	}

	private static String sendMessageRequest(String id) {
		return """
				{
				  "jsonrpc": "2.0",
				  "id": "%s",
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
				""".formatted(id);
	}

	private static Task_v0_3 sendMessage(JSONRPCTransport_v0_3 transport, String text) throws Exception {
		Message_v0_3 message = new Message_v0_3.Builder().role(Message_v0_3.Role.USER)
			.parts(new TextPart_v0_3(text))
			.build();

		EventKind_v0_3 result = transport.sendMessage(new MessageSendParams_v0_3.Builder().message(message).build(),
				null);

		assertThat(result).isInstanceOf(Task_v0_3.class);
		return (Task_v0_3) result;
	}

	private static String text(Task_v0_3 task) {
		return task.artifacts()
			.stream()
			.flatMap(artifact -> artifact.parts().stream())
			.filter(TextPart_v0_3.class::isInstance)
			.map(part -> ((TextPart_v0_3) part).text())
			.collect(Collectors.joining());
	}

}
