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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.protobuf.MessageOrBuilder;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.compat03.common.A2AHeaders_v0_3;
import org.a2aproject.sdk.compat03.conversion.A2AProtocol_v0_3;
import org.a2aproject.sdk.compat03.json.JsonUtil_v0_3;
import org.a2aproject.sdk.compat03.spec.CancelTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.DeleteTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetAuthenticatedExtendedCardRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.GetTaskRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.InternalError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidParamsError_v0_3;
import org.a2aproject.sdk.compat03.spec.InvalidRequestError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONParseError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCError_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCErrorResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCMessage_v0_3;
import org.a2aproject.sdk.compat03.spec.JSONRPCResponse_v0_3;
import org.a2aproject.sdk.compat03.spec.ListTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.MethodNotFoundError_v0_3;
import org.a2aproject.sdk.compat03.spec.NonStreamingJSONRPCRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SendStreamingMessageRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.SetTaskPushNotificationConfigRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.TaskResubscriptionRequest_v0_3;
import org.a2aproject.sdk.compat03.spec.UnsupportedOperationError_v0_3;
import org.a2aproject.sdk.compat03.transport.jsonrpc.handler.JSONRPCHandler_v0_3;
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
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.InternalError;
import org.a2aproject.sdk.spec.InvalidParamsError;
import org.a2aproject.sdk.spec.InvalidRequestError;
import org.a2aproject.sdk.spec.JSONParseError;
import org.a2aproject.sdk.spec.MethodNotFoundError;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.spec.VersionNotSupportedError;
import org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * All requests are received by {@link #handleRequest}, which selects the protocol version
 * by the {@code A2A-Version} header, as defined in section 3.6 of the A2A specification:
 * a missing or empty header means v0.3, patch versions are ignored (e.g. {@code 1.0.0} is
 * handled as {@code 1.0}), and unsupported versions are answered with a
 * {@link VersionNotSupportedError}.
 *
 * <p>
 * A2A v1.0 requests are handled by {@link #handleV10Request}. Their body is parsed and
 * the response is serialized with the JSON-RPC support of the A2A Java SDK, so the wire
 * format always matches the A2A clients. Spring's {@code HttpMessageConverter}s can't be
 * used for this, because the SDK's spec classes don't carry Jackson type information
 * (e.g. for {@code Part}) and A2A v1.0 uses the ProtoJSON format. The requests are
 * dispatched to the SDK's {@link JSONRPCHandler}, following the SDK's reference
 * implementation. Streaming methods ({@code SendStreamingMessage},
 * {@code SubscribeToTask}) aren't supported yet and are answered with an
 * {@link UnsupportedOperationError}.
 *
 * <p>
 * A2A v0.3 requests are handled by {@link #handleV03Request} in the same way, using the
 * SDK's compat-0.3 modules. The {@link JSONRPCHandler_v0_3} converts the v0.3 requests to
 * v1.0, delegates them to the same {@code RequestHandler} and converts the results back
 * to v0.3. Streaming methods ({@code message/stream}, {@code tasks/resubscribe}) aren't
 * supported yet either.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Thorben Janssen
 * @since 0.1.0
 */
@RestController
public class MessageController {

	private static final Logger logger = LoggerFactory.getLogger(MessageController.class);

	private static final String V0_3 = "0.3";

	private static final String V1_0 = "1.0";

	private static final List<String> SUPPORTED_VERSIONS = List.of(V0_3, V1_0);

	/**
	 * Matches a protocol version and captures its {@code Major.Minor} elements. A patch
	 * version is allowed but ignored.
	 */
	private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+)(\\.\\d+)?");

	private final JSONRPCHandler jsonRpcHandler;

	private final JSONRPCHandler_v0_3 jsonRpcHandlerV03;

	public MessageController(JSONRPCHandler jsonRpcHandler, JSONRPCHandler_v0_3 jsonRpcHandlerV03) {
		this.jsonRpcHandler = jsonRpcHandler;
		this.jsonRpcHandlerV03 = jsonRpcHandlerV03;
	}

	/**
	 * Handles all JSON-RPC requests and dispatches them by the protocol version that the
	 * client requested with the {@code A2A-Version} header.
	 */
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public String handleRequest(@RequestBody String body, @RequestHeader HttpHeaders headers) {
		String requestedVersion = headers.getFirst(A2AHeaders.A2A_VERSION);
		String version = protocolVersion(requestedVersion);
		if (V0_3.equals(version)) {
			return handleV03Request(body, headers);
		}
		if (V1_0.equals(version)) {
			return handleV10Request(body, headers);
		}
		logger.debug("Unsupported A2A protocol version: {}", requestedVersion);
		return JSONRPCUtils.toJsonRPCErrorResponse(parseRequestId(body),
				new VersionNotSupportedError(null, "Protocol version '" + requestedVersion
						+ "' is not supported. Supported versions: " + SUPPORTED_VERSIONS, null));
	}

	/**
	 * Returns the {@code Major.Minor} elements of the requested protocol version, "0.3"
	 * if no version was requested, or {@code null} if the version is invalid.
	 */
	private static String protocolVersion(String requestedVersion) {
		if (requestedVersion == null || requestedVersion.isBlank()) {
			return V0_3;
		}
		Matcher matcher = VERSION_PATTERN.matcher(requestedVersion.trim());
		return matcher.matches() ? matcher.group(1) : null;
	}

	/**
	 * Returns the id of the JSON-RPC request with its original type, or {@code null} if
	 * the request doesn't contain a valid id.
	 */
	private static Object parseRequestId(String body) {
		try {
			JsonElement id = JsonParser.parseString(body).getAsJsonObject().get("id");
			if (id != null && id.isJsonPrimitive()) {
				JsonPrimitive idPrimitive = id.getAsJsonPrimitive();
				if (idPrimitive.isString()) {
					return idPrimitive.getAsString();
				}
				if (idPrimitive.isNumber()) {
					return idPrimitive.getAsNumber();
				}
			}
		}
		catch (RuntimeException ex) {
			// No valid JSON-RPC request, so the error response has no id
		}
		return null;
	}

	/**
	 * Handles all A2A v1.0 JSON-RPC requests. Following the JSON-RPC specification,
	 * errors are returned as JSON-RPC error responses with HTTP status 200.
	 */
	private String handleV10Request(String body, HttpHeaders headers) {
		ServerCallContext context = createCallContext(headers, A2AHeaders.A2A_EXTENSIONS, V1_0);
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

	/**
	 * Handles all A2A v0.3 JSON-RPC requests, following the SDK's v0.3 reference
	 * implementation. Errors are returned as JSON-RPC error responses with HTTP status
	 * 200.
	 */
	private String handleV03Request(String body, HttpHeaders headers) {
		ServerCallContext context = createCallContext(headers, A2AHeaders_v0_3.X_A2A_EXTENSIONS,
				A2AProtocol_v0_3.PROTOCOL_VERSION);
		// Declared outside the try block, so that error responses keep the request id
		Object requestId = null;
		try {
			JsonObject node;
			try {
				node = JsonParser.parseString(body).getAsJsonObject();
			}
			catch (RuntimeException ex) {
				throw new JSONParseError_v0_3(ex.getMessage());
			}

			JsonElement idElement = node.get("id");
			if (idElement != null && !idElement.isJsonNull()) {
				if (!idElement.isJsonPrimitive()) {
					throw new InvalidRequestError_v0_3(
							"Invalid JSON-RPC request: 'id' must be a string, number, or null");
				}
				JsonPrimitive id = idElement.getAsJsonPrimitive();
				requestId = id.isNumber() ? id.getAsLong() : id.getAsString();
			}

			JsonElement jsonrpcElement = node.get("jsonrpc");
			if (jsonrpcElement == null || !jsonrpcElement.isJsonPrimitive()
					|| !JSONRPCMessage_v0_3.JSONRPC_VERSION.equals(jsonrpcElement.getAsString())) {
				throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: missing or invalid 'jsonrpc' field");
			}

			JsonElement methodElement = node.get("method");
			if (methodElement == null || !methodElement.isJsonPrimitive()) {
				throw new InvalidRequestError_v0_3("Invalid JSON-RPC request: missing or invalid 'method' field");
			}
			String method = methodElement.getAsString();
			context.getState().put(METHOD_NAME_KEY, method);
			logger.debug("Received {} request - id: {}", method, requestId);

			if (SendStreamingMessageRequest_v0_3.METHOD.equals(method)
					|| TaskResubscriptionRequest_v0_3.METHOD.equals(method)) {
				throw new UnsupportedOperationError_v0_3();
			}
			return toJsonV03(processRequest(parseV03Request(body, method), context));
		}
		catch (JSONRPCError_v0_3 ex) {
			return toJsonV03(new JSONRPCErrorResponse_v0_3(requestId, ex));
		}
		catch (RuntimeException ex) {
			logger.error("Unexpected error processing A2A v0.3 request", ex);
			return toJsonV03(new JSONRPCErrorResponse_v0_3(requestId, new InternalError_v0_3("Internal error")));
		}
	}

	private static NonStreamingJSONRPCRequest_v0_3<?> parseV03Request(String body, String method) {
		Class<? extends NonStreamingJSONRPCRequest_v0_3<?>> requestType = switch (method) {
			case SendMessageRequest_v0_3.METHOD -> SendMessageRequest_v0_3.class;
			case GetTaskRequest_v0_3.METHOD -> GetTaskRequest_v0_3.class;
			case CancelTaskRequest_v0_3.METHOD -> CancelTaskRequest_v0_3.class;
			case SetTaskPushNotificationConfigRequest_v0_3.METHOD -> SetTaskPushNotificationConfigRequest_v0_3.class;
			case GetTaskPushNotificationConfigRequest_v0_3.METHOD -> GetTaskPushNotificationConfigRequest_v0_3.class;
			case ListTaskPushNotificationConfigRequest_v0_3.METHOD -> ListTaskPushNotificationConfigRequest_v0_3.class;
			case DeleteTaskPushNotificationConfigRequest_v0_3.METHOD ->
				DeleteTaskPushNotificationConfigRequest_v0_3.class;
			case GetAuthenticatedExtendedCardRequest_v0_3.METHOD -> GetAuthenticatedExtendedCardRequest_v0_3.class;
			default -> throw new MethodNotFoundError_v0_3();
		};
		try {
			return JsonUtil_v0_3.fromJson(body, requestType);
		}
		catch (JSONRPCError_v0_3 ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new InvalidParamsError_v0_3(ex.getMessage());
		}
	}

	private JSONRPCResponse_v0_3<?> processRequest(NonStreamingJSONRPCRequest_v0_3<?> request,
			ServerCallContext context) {
		if (request instanceof SendMessageRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.onMessageSend(req, context);
		}
		if (request instanceof GetTaskRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.onGetTask(req, context);
		}
		if (request instanceof CancelTaskRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.onCancelTask(req, context);
		}
		if (request instanceof SetTaskPushNotificationConfigRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.setPushNotificationConfig(req, context);
		}
		if (request instanceof GetTaskPushNotificationConfigRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.getPushNotificationConfig(req, context);
		}
		if (request instanceof ListTaskPushNotificationConfigRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.listPushNotificationConfig(req, context);
		}
		if (request instanceof DeleteTaskPushNotificationConfigRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.deletePushNotificationConfig(req, context);
		}
		if (request instanceof GetAuthenticatedExtendedCardRequest_v0_3 req) {
			return this.jsonRpcHandlerV03.onGetAuthenticatedExtendedCardRequest(req, context);
		}
		return new JSONRPCErrorResponse_v0_3(request.getId(), new UnsupportedOperationError_v0_3());
	}

	private static String toJsonV03(JSONRPCResponse_v0_3<?> response) {
		try {
			return JsonUtil_v0_3.toJson(response);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Can't serialize A2A v0.3 response", ex);
		}
	}

	private static ServerCallContext createCallContext(HttpHeaders headers, String extensionsHeader,
			String protocolVersion) {
		Map<String, Object> state = new HashMap<>();
		state.put(HEADERS_KEY, headers.toSingleValueMap());
		state.put(TRANSPORT_KEY, TransportProtocol.JSONRPC);
		List<String> requestedExtensions = headers.getOrEmpty(extensionsHeader);
		// TODO: Add support for authenticated users
		return new ServerCallContext(UnauthenticatedUser.INSTANCE, state,
				A2AExtensions.getRequestedExtensions(requestedExtensions), protocolVersion);
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
