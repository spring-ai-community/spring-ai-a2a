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

import io.a2a.server.agentexecution.RequestContext;
import org.springframework.ai.chat.client.ChatClient;

/**
 * Executes ChatClient operations with A2A RequestContext for A2A agents.
 *
 * <p>This interface is used internally by {@link DefaultA2AChatClientAgentExecutor}
 * for executing ChatClient operations in response to A2A protocol requests.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@FunctionalInterface
public interface ChatClientExecutorHandler {

	/**
	 * Execute and return response.
	 *
	 * @param chatClient the Spring AI ChatClient
	 * @param requestContext the A2A RequestContext containing message, task, and context IDs
	 * @return the response text
	 */
	String execute(ChatClient chatClient, RequestContext requestContext);

}
