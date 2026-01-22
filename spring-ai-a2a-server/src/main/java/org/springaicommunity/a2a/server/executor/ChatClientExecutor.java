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

package org.springaicommunity.a2a.server.executor;

import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Executes ChatClient operations with context variables.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@FunctionalInterface
public interface ChatClientExecutor extends BiFunction<ChatClient, Map<String, Object>, String> {

	/**
	 * Execute and return response.
	 */
	String execute(ChatClient chatClient, Map<String, Object> context);

	/**
	 * Execute with streaming (default: wraps synchronous execution).
	 */
	default Flux<String> executeStream(ChatClient chatClient, Map<String, Object> context) {
		return Flux.just(execute(chatClient, context));
	}

	@Override
	default String apply(ChatClient chatClient, Map<String, Object> context) {
		return execute(chatClient, context);
	}

}
