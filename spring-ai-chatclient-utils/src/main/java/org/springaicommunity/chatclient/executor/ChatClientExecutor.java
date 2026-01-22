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

package org.springaicommunity.chatclient.executor;

import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * Executes ChatClient operations with user message and context.
 * Protocol-agnostic interface for Spring AI business logic.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@FunctionalInterface
public interface ChatClientExecutor {

	/**
	 * Execute and return response.
	 *
	 * @param chatClient the Spring AI ChatClient
	 * @param userMessage the user's message text (extracted from protocol layer)
	 * @param context execution context (protocol-agnostic)
	 * @return the response text
	 */
	String execute(ChatClient chatClient, String userMessage, Map<String, Object> context);

	/**
	 * Execute with streaming (default: wraps synchronous execution).
	 *
	 * @param chatClient the Spring AI ChatClient
	 * @param userMessage the user's message text
	 * @param context execution context
	 * @return flux of response chunks
	 */
	default Flux<String> executeStream(ChatClient chatClient, String userMessage, Map<String, Object> context) {
		return Flux.just(execute(chatClient, userMessage, context));
	}

}
