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

package org.springaicommunity.a2a.server.agentexecution;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.spec.Part;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link A2AAgentModelBuilder}.
 *
 * <p>This builder provides a fluent API for creating {@link DefaultA2AAgentModel} instances
 * with varying levels of customization.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see A2AAgentModelBuilder
 * @see DefaultA2AAgentModel
 */
public final class DefaultA2AAgentModelBuilder implements A2AAgentModelBuilder {

	private ChatClient chatClient;

	private String systemPrompt;

	// Optional customization functions
	private Function<String, List<Part<?>>> responseGenerator;

	private BiFunction<List<Part<?>>, RequestContext, List<Part<?>>> beforeCompleteHook;

	private Consumer<RequestContext> afterCompleteHook;

	private BiFunction<Exception, RequestContext, Void> onErrorHook;

	/**
	 * Package-private constructor - use {@link A2AAgentModel#builder()}.
	 */
	DefaultA2AAgentModelBuilder() {
	}

	@Override
	public A2AAgentModelBuilder chatClient(ChatClient chatClient) {
		Assert.notNull(chatClient, "chatClient cannot be null");
		this.chatClient = chatClient;
		return this;
	}

	@Override
	public A2AAgentModelBuilder chatModel(ChatModel chatModel, List<ToolCallback> tools) {
		Assert.notNull(chatModel, "chatModel cannot be null");
		// Build ChatClient with tools
		ChatClient.Builder builder = ChatClient.builder(chatModel);
		if (tools != null && !tools.isEmpty()) {
			builder.defaultToolCallbacks(tools);
		}
		this.chatClient = builder.build();
		return this;
	}

	@Override
	public A2AAgentModelBuilder systemPrompt(String systemPrompt) {
		Assert.hasText(systemPrompt, "systemPrompt cannot be null or empty");
		this.systemPrompt = systemPrompt;
		return this;
	}

	@Override
	public A2AAgentModelBuilder responseGenerator(Function<String, List<Part<?>>> responseGenerator) {
		this.responseGenerator = responseGenerator;
		return this;
	}

	@Override
	public A2AAgentModelBuilder beforeComplete(BiFunction<List<Part<?>>, RequestContext, List<Part<?>>> beforeComplete) {
		this.beforeCompleteHook = beforeComplete;
		return this;
	}

	@Override
	public A2AAgentModelBuilder afterComplete(Consumer<RequestContext> afterComplete) {
		this.afterCompleteHook = afterComplete;
		return this;
	}

	@Override
	public A2AAgentModelBuilder onError(BiFunction<Exception, RequestContext, Void> onError) {
		this.onErrorHook = onError;
		return this;
	}

	@Override
	public A2AAgentModel build() {
		// Validate required fields
		Assert.notNull(this.chatClient, "chatClient is required - call chatClient() or chatModel() first");
		Assert.hasText(this.systemPrompt, "systemPrompt is required - call systemPrompt() first");

		// Create and return the agent
		return new DefaultA2AAgentModel(this.chatClient, this.systemPrompt, this.responseGenerator,
				this.beforeCompleteHook, this.afterCompleteHook, this.onErrorHook);
	}

}
