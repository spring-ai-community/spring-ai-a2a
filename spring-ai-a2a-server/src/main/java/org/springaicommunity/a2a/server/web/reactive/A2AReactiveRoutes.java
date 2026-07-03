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

package org.springaicommunity.a2a.server.web.reactive;

import io.a2a.spec.AgentCard;
import io.a2a.spec.SendMessageRequest;
import org.springaicommunity.a2a.server.core.A2ARequestProcessor;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;

/**
 * WebFlux.fn (reactive) functional routes for the A2A server.
 *
 * <p>
 * Thin adapter over {@link A2ARequestProcessor}: translates reactive-stack
 * {@link ServerRequest}s into processor calls. All protocol logic lives in the shared
 * processor, which already offloads the blocking SDK calls to a bounded-elastic scheduler
 * so event-loop threads are never blocked.
 *
 * <p>
 * Registered by the auto-configuration when the application runs on a reactive web stack
 * (e.g. {@code spring-boot-starter-webflux} / Netty).
 *
 * @author Mekki Amiri
 * @since 0.4.0
 */
public final class A2AReactiveRoutes {

	private A2AReactiveRoutes() {
	}

	/**
	 * Builds the A2A reactive router function.
	 * @param processor the shared A2A request processor
	 * @param agentCard the agent card advertised by this server
	 * @return the router function exposing the A2A HTTP endpoints
	 */
	public static RouterFunction<ServerResponse> routes(A2ARequestProcessor processor, AgentCard agentCard) {
		return RouterFunctions.route()
			.POST("/", contentType(MediaType.APPLICATION_JSON), request -> sendMessage(processor, request))
			.GET("/.well-known/agent-card.json", request -> agentCard(agentCard))
			.GET("/card", request -> agentCard(agentCard))
			.GET("/tasks/{taskId}", request -> getTask(processor, request))
			.POST("/tasks/{taskId}/cancel", request -> cancelTask(processor, request))
			.build();
	}

	private static Mono<ServerResponse> sendMessage(A2ARequestProcessor processor, ServerRequest request) {
		return request.bodyToMono(SendMessageRequest.class)
			.flatMap(processor::sendMessage)
			.flatMap(response -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(response));
	}

	private static Mono<ServerResponse> agentCard(AgentCard agentCard) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(agentCard);
	}

	private static Mono<ServerResponse> getTask(A2ARequestProcessor processor, ServerRequest request) {
		return processor.getTask(request.pathVariable("taskId"))
			.flatMap(task -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(task));
	}

	private static Mono<ServerResponse> cancelTask(A2ARequestProcessor processor, ServerRequest request) {
		return processor.cancelTask(request.pathVariable("taskId"))
			.flatMap(task -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(task));
	}

}
