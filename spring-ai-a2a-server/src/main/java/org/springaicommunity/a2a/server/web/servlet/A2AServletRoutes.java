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

package org.springaicommunity.a2a.server.web.servlet;

import io.a2a.spec.AgentCard;
import io.a2a.spec.SendMessageRequest;
import org.springaicommunity.a2a.server.core.A2ARequestProcessor;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.contentType;

/**
 * WebMvc.fn (servlet) functional routes for the A2A server.
 *
 * <p>
 * Thin adapter over {@link A2ARequestProcessor}: translates servlet-stack
 * {@link ServerRequest}s into processor calls and adapts the resulting reactive types via
 * {@link ServerResponse#async(Object)}. All protocol logic lives in the shared processor.
 *
 * <p>
 * Registered by the auto-configuration when the application runs on a servlet web stack
 * (e.g. {@code spring-boot-starter-web} / Tomcat).
 *
 * @author Mekki Amiri
 * @since 0.4.0
 */
public final class A2AServletRoutes {

	private A2AServletRoutes() {
	}

	/**
	 * Builds the A2A servlet router function.
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

	private static ServerResponse sendMessage(A2ARequestProcessor processor, ServerRequest request) throws Exception {
		SendMessageRequest body = request.body(SendMessageRequest.class);
		return ServerResponse.async(processor.sendMessage(body)
			.map(response -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(response)));
	}

	private static ServerResponse agentCard(AgentCard agentCard) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(agentCard);
	}

	private static ServerResponse getTask(A2ARequestProcessor processor, ServerRequest request) {
		return ServerResponse.async(processor.getTask(request.pathVariable("taskId"))
			.map(task -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(task)));
	}

	private static ServerResponse cancelTask(A2ARequestProcessor processor, ServerRequest request) {
		return ServerResponse.async(processor.cancelTask(request.pathVariable("taskId"))
			.map(task -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).body(task)));
	}

}
