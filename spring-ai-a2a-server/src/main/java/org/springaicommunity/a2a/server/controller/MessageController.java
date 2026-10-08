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

package org.springaicommunity.a2a.server.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Set;

import com.google.gson.JsonSyntaxException;
import com.google.protobuf.MessageOrBuilder;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.grpc.utils.JSONRPCUtils;
import org.a2aproject.sdk.grpc.utils.ProtoUtils;
import org.a2aproject.sdk.jsonrpc.common.json.IdJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.InvalidParamsJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.json.JsonProcessingException;
import org.a2aproject.sdk.jsonrpc.common.json.MethodNotFoundJsonMappingException;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AErrorResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2ARequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.A2AResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CancelTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.GetTaskResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTaskPushNotificationConfigsResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksRequest;
import org.a2aproject.sdk.jsonrpc.common.wrappers.ListTasksResponse;
import org.a2aproject.sdk.jsonrpc.common.wrappers.NonStreamingJSONRPCRequest;
import org.a2aproject.sdk.server.ServerCallContext;
import org.a2aproject.sdk.server.auth.UnauthenticatedUser;
import org.a2aproject.sdk.server.extensions.A2AExtensions;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.EventKind;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.model.SendMessageRequest;
import org.springaicommunity.a2a.server.model.SendMessageResponse;
import org.springaicommunity.a2a.server.model.SendMessageRequest;
import org.springaicommunity.a2a.server.model.SendMessageResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.a2aproject.sdk.server.ServerCallContext.TRANSPORT_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.HEADERS_KEY;
import static org.a2aproject.sdk.transport.jsonrpc.context.JSONRPCContextKeys.METHOD_NAME_KEY;

/**
 * JSON-RPC endpoint of the A2A server.
 *
 * <p>
 * Requests with the header {@code A2A-Version: 1.0} are handled by
 * {@link #handleV10Request}. Their body is parsed and the response is serialized with the
 * JSON-RPC support of the A2A Java SDK, so the wire format always matches the A2A
 * clients. Spring's {@code HttpMessageConverter}s can't be used for this, because the
 * SDK's spec classes don't carry Jackson type information (e.g. for {@code Part}) and A2A
 * v1.0 uses the ProtoJSON format. The requests are dispatched to the SDK's
 * {@link JSONRPCHandler}, following the SDK's reference implementation. Streaming methods
 * ({@code SendStreamingMessage}, {@code SubscribeToTask}) aren't supported yet and are
 * answered with an {@link UnsupportedOperationError}.
 *
 * <p>
 * All other requests (no {@code A2A-Version} header, i.e. A2A v0.3) are handled by
 * {@link #sendMessage}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @since 0.1.0
 */
@RestController
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

	private static final String V1_0 = "1.0";

	private final RequestHandler requestHandler;

	private final JSONRPCHandler jsonRpcHandler;

	public MessageController(RequestHandler requestHandler, JSONRPCHandler jsonRpcHandler) {
		this.requestHandler = requestHandler;
		this.jsonRpcHandler = jsonRpcHandler;
	}

	/**
	 * Handles sendMessage JSON-RPC requests. Passes the {@code A2A-Version} request
	 * header to the {@link ServerCallContext} so that the {@link RequestHandler} can
	 * apply the correct protocol version rules (v0.3 vs v1.0).
	 */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public SendMessageResponse sendMessage(@RequestBody SendMessageRequest request,
			@RequestHeader(value = "A2A-Version", required = false) String a2aVersion) throws A2AError {

		logger.debug("Received sendMessage request - id: {}", request.id());

		try {
			String version = (a2aVersion == null || a2aVersion.isBlank()) ? "0.3" : a2aVersion;

			// TODO: Add support for auth context, state, and extensions
			ServerCallContext context = new ServerCallContext(null, // auth context
					Map.of(), // state
					Set.of(), // extensions
					version // requested protocol version
			);

			EventKind result = this.requestHandler.onMessageSend(request.params(), context);

			logger.debug("Message processed successfully - id: {}", request.id());
			return new SendMessageResponse(request.jsonrpc(), request.id(), result);
		}
		catch (A2AError e) {
			logger.error("Error processing message - id: {}", request.id(), e);
			throw e;
		}
		catch (Exception e) {
			logger.error("Unexpected error processing message - id: {}", request.id(), e);
			throw new A2AError(-32603, "Internal error: " + e.getMessage(), null);
		}
	}

	/**
	 * Handles all A2A v1.0 JSON-RPC requests. Following the JSON-RPC specification,
	 * errors are returned as JSON-RPC error responses with HTTP status 200.
	 */
	@PostMapping(headers = A2AHeaders.A2A_VERSION + "=" + V1_0, consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public String handleV10Request(@RequestBody String body, @RequestHeader HttpHeaders headers) {
		ServerCallContext context = createCallContext(headers);
		// Declared outside the try block, so that errors after parsing keep the request
		// id
		A2ARequest<?> request = null;
		try {
			request = JSONRPCUtils.parseRequestBody(body, null);
			context.getState().put(METHOD_NAME_KEY, request.getMethod());
			logger.debug("Received {} request - id: {}", request.getMethod(), request.getId());

			if (request instanceof NonStreamingJSONRPCRequest<?> nonStreamingRequest) {
				return serializeResponse(processRequest(nonStreamingRequest, context));
			}
			return serializeResponse(new A2AErrorResponse(request.getId(), new UnsupportedOperationError()));
		}
		catch (A2AError ex) {
			return serializeResponse(new A2AErrorResponse(requestId(request), ex));
		}
		catch (InvalidParamsJsonMappingException ex) {
			return serializeResponse(
					new A2AErrorResponse(ex.getId(), new InvalidParamsError(null, ex.getMessage(), null)));
		}
		catch (MethodNotFoundJsonMappingException ex) {
			return serializeResponse(
					new A2AErrorResponse(ex.getId(), new MethodNotFoundError(null, ex.getMessage(), null)));
		}
		catch (IdJsonMappingException ex) {
			return serializeResponse(
					new A2AErrorResponse(ex.getId(), new InvalidRequestError(null, ex.getMessage(), null)));
		}
		catch (JsonMappingException ex) {
			return serializeResponse(new A2AErrorResponse(new InvalidRequestError(null, ex.getMessage(), null)));
		}
		catch (JsonSyntaxException | JsonProcessingException ex) {
			return serializeResponse(new A2AErrorResponse(new JSONParseError(ex.getMessage())));
		}
		catch (RuntimeException ex) {
			logger.error("Unexpected error processing A2A request", ex);
			return serializeResponse(new A2AErrorResponse(requestId(request), new InternalError("Internal error")));
		}
	}

	private static Object requestId(A2ARequest<?> request) {
		return (request != null) ? request.getId() : null;
	}

	private A2AResponse<?> processRequest(NonStreamingJSONRPCRequest<?> request, ServerCallContext context) {
		if (request instanceof org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageRequest req) {
			return this.jsonRpcHandler.onMessageSend(req, context);
		}
		if (request instanceof GetTaskRequest req) {
			return this.jsonRpcHandler.onGetTask(req, context);
		}
		if (request instanceof CancelTaskRequest req) {
			return this.jsonRpcHandler.onCancelTask(req, context);
		}
		if (request instanceof ListTasksRequest req) {
			return this.jsonRpcHandler.onListTasks(req, context);
		}
		if (request instanceof CreateTaskPushNotificationConfigRequest req) {
			return this.jsonRpcHandler.setPushNotificationConfig(req, context);
		}
		if (request instanceof GetTaskPushNotificationConfigRequest req) {
			return this.jsonRpcHandler.getPushNotificationConfig(req, context);
		}
		if (request instanceof ListTaskPushNotificationConfigsRequest req) {
			return this.jsonRpcHandler.listPushNotificationConfigs(req, context);
		}
		if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
			return this.jsonRpcHandler.deletePushNotificationConfig(req, context);
		}
		if (request instanceof GetExtendedAgentCardRequest req) {
			return this.jsonRpcHandler.onGetExtendedCardRequest(req, context);
		}
		return new A2AErrorResponse(request.getId(), new UnsupportedOperationError());
	}

	private static ServerCallContext createCallContext(HttpHeaders headers) {
		Map<String, Object> state = new HashMap<>();
		state.put(HEADERS_KEY, headers.toSingleValueMap());
		state.put(TRANSPORT_KEY, TransportProtocol.JSONRPC);
		// TODO: Add support for authenticated users
		return new ServerCallContext(UnauthenticatedUser.INSTANCE, state,
				A2AExtensions.getRequestedExtensions(headers.getOrEmpty(A2AHeaders.A2A_EXTENSIONS)),
				headers.getFirst(A2AHeaders.A2A_VERSION));
	}

	private static String serializeResponse(A2AResponse<?> response) {
		if (response.getError() != null) {
			return JSONRPCUtils.toJsonRPCErrorResponse(response.getId(), response.getError());
		}
		return JSONRPCUtils.toJsonRPCResultResponse(response.getId(), toProto(response));
	}

	private static MessageOrBuilder toProto(A2AResponse<?> response) {
		if (response instanceof org.a2aproject.sdk.jsonrpc.common.wrappers.SendMessageResponse r) {
			return ProtoUtils.ToProto.taskOrMessage(r.getResult());
		}
		if (response instanceof GetTaskResponse r) {
			return ProtoUtils.ToProto.task(r.getResult());
		}
		if (response instanceof CancelTaskResponse r) {
			return ProtoUtils.ToProto.task(r.getResult());
		}
		if (response instanceof ListTasksResponse r) {
			return ProtoUtils.ToProto.listTasksResult(r.getResult());
		}
		if (response instanceof CreateTaskPushNotificationConfigResponse r) {
			return ProtoUtils.ToProto.createTaskPushNotificationConfigResponse(r.getResult());
		}
		if (response instanceof GetTaskPushNotificationConfigResponse r) {
			return ProtoUtils.ToProto.getTaskPushNotificationConfigResponse(r.getResult());
		}
		if (response instanceof ListTaskPushNotificationConfigsResponse r) {
			return ProtoUtils.ToProto.listTaskPushNotificationConfigsResponse(r.getResult());
		}
		if (response instanceof DeleteTaskPushNotificationConfigResponse) {
			return com.google.protobuf.Empty.getDefaultInstance();
		}
		if (response instanceof GetExtendedAgentCardResponse r) {
			return ProtoUtils.ToProto.getExtendedCardResponse(r.getResult());
		}
		throw new IllegalArgumentException("Unknown response type: " + response.getClass().getName());
	}

}
