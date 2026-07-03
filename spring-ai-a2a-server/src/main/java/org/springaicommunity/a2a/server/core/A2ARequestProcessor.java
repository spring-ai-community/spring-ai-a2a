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

package org.springaicommunity.a2a.server.core;

import java.util.Map;
import java.util.Set;

import io.a2a.server.ServerCallContext;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.EventKind;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskQueryParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Web-stack agnostic A2A request processor.
 *
 * <p>
 * Wraps the A2A SDK's blocking {@link RequestHandler} in Reactor types
 * ({@link Mono}/Flux) so the same processing logic can be exposed through both Spring
 * WebMvc.fn (servlet) and WebFlux.fn (reactive) router functions. Blocking SDK calls are
 * offloaded to the {@link Schedulers#boundedElastic() bounded-elastic} scheduler, which
 * keeps event-loop threads free when running on a reactive runtime (Netty) and is
 * harmless on a servlet runtime.
 *
 * <p>
 * This class contains no web-layer types — routing, content negotiation and
 * request/response mapping live in the per-stack router configurations.
 *
 * @author Mekki Amiri
 * @since 0.4.0
 */
public class A2ARequestProcessor {

	private static final Logger logger = LoggerFactory.getLogger(A2ARequestProcessor.class);

	private final RequestHandler requestHandler;

	public A2ARequestProcessor(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	/**
	 * Handles a JSON-RPC {@code message/send} request.
	 *
	 * <p>
	 * Per the JSON-RPC contract, {@link JSONRPCError}s raised by the protocol layer are
	 * returned as a JSON-RPC <em>error response</em> (HTTP 200 with an {@code error}
	 * member) rather than surfacing as an HTTP 5xx.
	 * @param request the sendMessage JSON-RPC request
	 * @return the JSON-RPC response, resolving on the bounded-elastic scheduler
	 */
	public Mono<SendMessageResponse> sendMessage(SendMessageRequest request) {
		return Mono.fromCallable(() -> {
			logger.debug("Received sendMessage request - id: {}", request.getId());
			EventKind result = this.requestHandler.onMessageSend(request.getParams(), newCallContext());
			logger.debug("Message processed successfully - id: {}", request.getId());
			return new SendMessageResponse(request.getId(), result);
		}).onErrorResume(JSONRPCError.class, error -> Mono.fromCallable(() -> {
			logger.error("Error processing message - id: {}", request.getId(), error);
			return new SendMessageResponse(request.getId(), error);
		})).onErrorResume(throwable -> !(throwable instanceof JSONRPCError), throwable -> Mono.fromCallable(() -> {
			logger.error("Unexpected error processing message - id: {}", request.getId(), throwable);
			return new SendMessageResponse(request.getId(),
					new JSONRPCError(-32603, "Internal error: " + throwable.getMessage(), null));
		})).subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * Returns task status and results.
	 * @param taskId the task identifier
	 * @return the task, resolving on the bounded-elastic scheduler
	 */
	public Mono<Task> getTask(String taskId) {
		return Mono.fromCallable(() -> {
			logger.info("Getting task: {}", taskId);
			Task task = this.requestHandler.onGetTask(new TaskQueryParams(taskId), newCallContext());
			logger.debug("Task retrieved: {} - state: {}", taskId, task.getStatus().state());
			return task;
		}).subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * Cancels a running task.
	 * @param taskId the task identifier
	 * @return the cancelled task, resolving on the bounded-elastic scheduler
	 */
	public Mono<Task> cancelTask(String taskId) {
		return Mono.fromCallable(() -> {
			logger.info("Cancelling task: {}", taskId);
			Task task = this.requestHandler.onCancelTask(new TaskIdParams(taskId), newCallContext());
			logger.debug("Task cancelled: {}", taskId);
			return task;
		}).subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * Creates the per-request server call context.
	 *
	 * <p>
	 * TODO: Add support for auth context, state, and extensions.
	 */
	private static ServerCallContext newCallContext() {
		return new ServerCallContext(null, Map.of(), Set.of());
	}

}
