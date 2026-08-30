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

import io.a2a.server.ServerCallContext;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendStreamingMessageRequest;
import io.a2a.spec.SendStreamingMessageResponse;
import io.a2a.spec.StreamingEventKind;
import org.junit.jupiter.api.Test;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.http.codec.ServerSentEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageControllerTest {

	private final RequestHandler requestHandler = mock(RequestHandler.class);

	private final MessageController controller = new MessageController(this.requestHandler);

	@Test
	void sendMessageStreamWrapsEventsInSseJsonRpcResponses() throws JSONRPCError {
		MessageSendParams params = new MessageSendParams(mock(Message.class), null, null);
		SendStreamingMessageRequest request = new SendStreamingMessageRequest("request-1", params);
		StreamingEventKind event = mock(Message.class);
		when(this.requestHandler.onMessageSendStream(same(params), any(ServerCallContext.class)))
			.thenReturn(JdkFlowAdapter.publisherToFlowPublisher(Flux.just(event)));

		Flux<ServerSentEvent<SendStreamingMessageResponse>> response = this.controller.sendMessageStream(request);

		StepVerifier.create(response).assertNext(sse -> {
			assertThat(sse.data()).isNotNull();
			assertThat(sse.data().getId()).isEqualTo("request-1");
			assertThat(sse.data().getResult()).isSameAs(event);
		}).verifyComplete();
		verify(this.requestHandler).onMessageSendStream(same(params), any(ServerCallContext.class));
	}

	@Test
	void sendMessageStreamWrapsUnexpectedHandlerErrors() throws JSONRPCError {
		MessageSendParams params = new MessageSendParams(mock(Message.class), null, null);
		SendStreamingMessageRequest request = new SendStreamingMessageRequest("request-2", params);
		when(this.requestHandler.onMessageSendStream(same(params), any(ServerCallContext.class)))
			.thenThrow(new IllegalStateException("boom"));

		assertThatThrownBy(() -> this.controller.sendMessageStream(request)).isInstanceOf(JSONRPCError.class)
			.hasMessageContaining("Internal error: boom");
	}

}
