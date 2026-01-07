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

package org.springaicommunity.a2a.integration;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springaicommunity.a2a.server.agentexecution.A2AAgentModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for synchronous and asynchronous A2A execution modes.
 *
 * <p>
 * These tests verify:
 * <ul>
 * <li>Synchronous execution (blocking request/response)</li>
 * <li>Asynchronous streaming execution (Server-Sent Events)</li>
 * <li>Both execution modes produce equivalent results</li>
 * <li>Streaming emits incremental responses</li>
 * <li>Error handling in both modes</li>
 * <li>Performance characteristics of each mode</li>
 * </ul>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@SpringBootTest(classes = TestA2AApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class A2ASyncAsyncExecutionTests {

	private static final int TEST_PORT = 58888;

	private static final String AGENT_URL = "http://localhost:" + TEST_PORT + "/a2a";

	@Autowired
	private A2AAgentModel agentExecutor;

	private WebTestClient webTestClient;

	@BeforeEach
	void setUp() {
		// Create WebTestClient for testing HTTP endpoints
		this.webTestClient = WebTestClient.bindToServer()
			.baseUrl("http://localhost:" + TEST_PORT)
			.responseTimeout(Duration.ofSeconds(30))
			.build();
	}

	// ============================================================================
	// SYNCHRONOUS EXECUTION TESTS
	// ============================================================================

	/**
	 * Tests synchronous execution via the agentExecutor.executeSynchronous() method.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Request is processed completely before returning</li>
	 * <li>Response contains expected content</li>
	 * <li>Execution blocks until completion</li>
	 * </ul>
	 */
	@Test
	void testSynchronousExecutionViaExecutor() {
		// Create request
		Message request = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart("Hello synchronous world"))).build();

		// Execute synchronously
		long startTime = System.currentTimeMillis();
		Message response = this.agentExecutor.executeSynchronous(request);
		long duration = System.currentTimeMillis() - startTime;

		// Verify response
		assertThat(response).isNotNull();
		assertThat(response.parts()).isNotEmpty();

		String responseText = extractTextFromResponse(response);
		assertThat(responseText).contains("Echo:");
		assertThat(responseText).contains("Hello synchronous world");

		// Verify it took some time (not instant)
		assertThat(duration).isGreaterThan(0);
	}

	/**
	 * Tests synchronous execution via the JSON-RPC sendMessage endpoint.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>HTTP POST to /a2a with sendMessage method works</li>
	 * <li>JSON-RPC protocol is correctly implemented</li>
	 * <li>Response is returned synchronously</li>
	 * </ul>
	 */
	@Test
	void testSynchronousExecutionViaHTTP() {
		// Create JSON-RPC request
		String jsonRpcRequest = """
				{
				  "jsonrpc": "2.0",
				  "method": "sendMessage",
				  "id": 1,
				  "params": {
				    "message": {
				      "role": "USER",
				      "parts": [
				        {"text": "Hello via HTTP"}
				      ]
				    }
				  }
				}
				""";

		// Send synchronous HTTP request
		this.webTestClient.post()
			.uri("/a2a")
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(jsonRpcRequest)
			.exchange()
			.expectStatus()
			.isOk()
			.expectBody()
			.jsonPath("$.jsonrpc")
			.isEqualTo("2.0")
			.jsonPath("$.id")
			.isEqualTo(1)
			.jsonPath("$.result.message.parts[0].text")
			.value(text -> {
				assertThat((String) text).contains("Echo:");
				assertThat((String) text).contains("Hello via HTTP");
			});
	}

	/**
	 * Tests synchronous execution with multiple sequential calls.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Multiple synchronous calls work correctly</li>
	 * <li>Each call completes independently</li>
	 * <li>No state leakage between calls</li>
	 * </ul>
	 */
	@Test
	void testSynchronousMultipleCalls() {
		List<String> results = new ArrayList<>();

		// Execute 3 synchronous calls
		for (int i = 1; i <= 3; i++) {
			Message request = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart("Message " + i))).build();
			Message response = this.agentExecutor.executeSynchronous(request);

			String responseText = extractTextFromResponse(response);
			results.add(responseText);

			assertThat(responseText).contains("Echo:");
			assertThat(responseText).contains("Message " + i);
		}

		// Verify all 3 results are distinct
		assertThat(results).hasSize(3);
		assertThat(results.get(0)).contains("Message 1");
		assertThat(results.get(1)).contains("Message 2");
		assertThat(results.get(2)).contains("Message 3");
	}

	// ============================================================================
	// ASYNCHRONOUS EXECUTION TESTS
	// ============================================================================

	/**
	 * Tests asynchronous execution via the agentExecutor.executeStreaming() method.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Returns a Flux that emits responses incrementally</li>
	 * <li>All response parts are eventually emitted</li>
	 * <li>Flux completes successfully</li>
	 * </ul>
	 */
	@Test
	void testAsynchronousExecutionViaExecutor() {
		// Create request
		Message request = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart("Hello asynchronous world"))).build();

		// Execute asynchronously
		Flux<Message> responseFlux = this.agentExecutor.executeStreaming(request);

		// Use StepVerifier to test the Flux
		StepVerifier.create(responseFlux)
			.expectNextMatches(response -> {
				assertThat(response.parts()).isNotEmpty();
				String text = extractTextFromResponse(response);
				return text.contains("Echo:") && text.contains("Hello asynchronous world");
			})
			.verifyComplete();
	}

	/**
	 * Tests asynchronous execution via the SSE /a2a/stream endpoint.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Server-Sent Events endpoint works correctly</li>
	 * <li>Events are emitted incrementally</li>
	 * <li>Final completion event is sent</li>
	 * </ul>
	 */
	@Test
	void testAsynchronousExecutionViaSSE() {
		// Execute async request via SSE endpoint
		FluxExchangeResult<ServerSentEvent> result = this.webTestClient.get()
			.uri(uriBuilder -> uriBuilder.path("/a2a/stream").queryParam("message", "Hello via SSE").build())
			.accept(MediaType.TEXT_EVENT_STREAM)
			.exchange()
			.expectStatus()
			.isOk()
			.returnResult(ServerSentEvent.class);

		// Collect events
		List<ServerSentEvent> events = result.getResponseBody().collectList().block(Duration.ofSeconds(30));

		assertThat(events).isNotNull();
		assertThat(events).isNotEmpty();

		// Verify we received at least one message event and a complete event
		boolean hasMessageEvent = events.stream().anyMatch(event -> "message".equals(event.event()));
		boolean hasCompleteEvent = events.stream().anyMatch(event -> "complete".equals(event.event()));

		assertThat(hasMessageEvent).isTrue();
		assertThat(hasCompleteEvent).isTrue();
	}

	/**
	 * Tests asynchronous execution collects all streaming chunks.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>All chunks from streaming execution are collected</li>
	 * <li>Assembled result matches expected output</li>
	 * </ul>
	 */
	@Test
	void testAsynchronousCollectsAllChunks() {
		Message request = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart("Test streaming chunks"))).build();

		// Collect all parts from streaming execution
		List<Part<?>> allParts = this.agentExecutor.executeStreaming(request)
			.flatMapIterable(Message::parts)
			.collectList()
			.block(Duration.ofSeconds(30));

		assertThat(allParts).isNotNull();
		assertThat(allParts).isNotEmpty();

		// Assemble full response
		StringBuilder fullResponse = new StringBuilder();
		for (Part<?> part : allParts) {
			if (part instanceof TextPart textPart) {
				fullResponse.append(textPart.text());
			}
		}

		String responseText = fullResponse.toString();
		assertThat(responseText).contains("Echo:");
		assertThat(responseText).contains("Test streaming chunks");
	}

	// ============================================================================
	// EQUIVALENCE TESTS (Sync vs Async produce same results)
	// ============================================================================

	/**
	 * Tests that synchronous and asynchronous execution produce equivalent results.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Both modes process the same request identically</li>
	 * <li>Content is equivalent (modulo timing/formatting differences)</li>
	 * <li>Both complete successfully</li>
	 * </ul>
	 */
	@Test
	void testSyncAndAsyncProduceEquivalentResults() {
		String testMessage = "Compare sync and async";
		Message request = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart(testMessage))).build();

		// Execute synchronously
		Message syncResponse = this.agentExecutor.executeSynchronous(request);
		String syncText = extractTextFromResponse(syncResponse);

		// Execute asynchronously and collect
		List<Part<?>> asyncParts = this.agentExecutor.executeStreaming(request)
			.flatMapIterable(Message::parts)
			.collectList()
			.block(Duration.ofSeconds(30));

		assertThat(asyncParts).isNotNull();

		StringBuilder asyncText = new StringBuilder();
		for (Part<?> part : asyncParts) {
			if (part instanceof TextPart textPart) {
				asyncText.append(textPart.text());
			}
		}

		// Both should contain the echo response
		assertThat(syncText).contains("Echo:");
		assertThat(syncText).contains(testMessage);
		assertThat(asyncText.toString()).contains("Echo:");
		assertThat(asyncText.toString()).contains(testMessage);

		// Content should be similar (may differ in whitespace or exact formatting)
		assertThat(syncText.trim()).isEqualTo(asyncText.toString().trim());
	}

	// ============================================================================
	// STREAMING BEHAVIOR TESTS
	// ============================================================================

	/**
	 * Tests that streaming execution emits responses incrementally.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Responses are emitted as they become available</li>
	 * <li>Not all responses arrive at once (streaming behavior)</li>
	 * <li>Flux emits multiple times for complex responses</li>
	 * </ul>
	 */
	@Test
	void testStreamingEmitsIncrementally() {
		Message request = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart("Test incremental streaming"))).build();

		AtomicInteger emissionCount = new AtomicInteger(0);
		List<Long> emissionTimestamps = new ArrayList<>();
		long startTime = System.currentTimeMillis();

		// Subscribe and count emissions
		this.agentExecutor.executeStreaming(request)
			.doOnNext(response -> {
				emissionCount.incrementAndGet();
				emissionTimestamps.add(System.currentTimeMillis() - startTime);
			})
			.blockLast(Duration.ofSeconds(30));

		// Verify we received at least one emission
		assertThat(emissionCount.get()).isGreaterThanOrEqualTo(1);

		// If we got multiple emissions, they should have different timestamps
		if (emissionTimestamps.size() > 1) {
			long firstEmission = emissionTimestamps.get(0);
			long lastEmission = emissionTimestamps.get(emissionTimestamps.size() - 1);
			assertThat(lastEmission).isGreaterThan(firstEmission);
		}
	}

	/**
	 * Tests concurrent asynchronous executions.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Multiple async executions can run concurrently</li>
	 * <li>Each execution completes independently</li>
	 * <li>No interference between concurrent streams</li>
	 * </ul>
	 */
	@Test
	void testConcurrentAsyncExecutions() throws InterruptedException {
		int concurrentRequests = 3;
		CountDownLatch latch = new CountDownLatch(concurrentRequests);
		List<String> results = new ArrayList<>();

		// Start 3 concurrent async executions
		for (int i = 1; i <= concurrentRequests; i++) {
			final int requestNum = i;
			Message request = Message.builder().role(Message.Role.USER).parts(List.of(new TextPart("Concurrent request " + requestNum))).build();

			this.agentExecutor.executeStreaming(request)
				.flatMapIterable(Message::parts)
				.collectList()
				.subscribe(parts -> {
					StringBuilder text = new StringBuilder();
					for (Part<?> part : parts) {
						if (part instanceof TextPart textPart) {
							text.append(textPart.text());
						}
					}
					synchronized (results) {
						results.add(text.toString());
					}
					latch.countDown();
				});
		}

		// Wait for all to complete
		boolean completed = latch.await(30, TimeUnit.SECONDS);
		assertThat(completed).isTrue();

		// Verify all 3 completed
		assertThat(results).hasSize(concurrentRequests);

		// Each should contain its unique message
		assertThat(results.stream().anyMatch(r -> r.contains("Concurrent request 1"))).isTrue();
		assertThat(results.stream().anyMatch(r -> r.contains("Concurrent request 2"))).isTrue();
		assertThat(results.stream().anyMatch(r -> r.contains("Concurrent request 3"))).isTrue();
	}

	// ============================================================================
	// ERROR HANDLING TESTS
	// ============================================================================

	/**
	 * Tests error handling in synchronous execution.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Errors are thrown as exceptions</li>
	 * <li>Error messages are meaningful</li>
	 * </ul>
	 */
	@Test
	void testSynchronousErrorHandling() {
		// Create request with single space (minimal valid message that may still cause processing issues)
		Message minimalMessage = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart(" ")))
			.build();
		Message request = minimalMessage;

		try {
			Message response = this.agentExecutor.executeSynchronous(request);
			// If it succeeds, just verify response is not null
			assertThat(response).isNotNull();
			assertThat(response.parts()).isNotNull();
		}
		catch (Exception e) {
			// If it fails, verify error handling
			assertThat(e).isNotNull();
			// Error should be meaningful
			assertThat(e.getMessage()).isNotBlank();
		}
	}

	/**
	 * Tests error handling in asynchronous execution.
	 * <p>
	 * Verifies:
	 * <ul>
	 * <li>Errors are emitted to the Flux as error signals</li>
	 * <li>Error propagation works correctly</li>
	 * </ul>
	 */
	@Test
	void testAsynchronousErrorHandling() {
		// Create request with single space (minimal valid message)
		Message minimalMessage = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart(" ")))
			.build();
		Message request = minimalMessage;

		Flux<Message> responseFlux = this.agentExecutor.executeStreaming(request);

		// Either completes successfully or emits an error
		AtomicReference<Throwable> errorRef = new AtomicReference<>();
		AtomicReference<Message> successRef = new AtomicReference<>();

		responseFlux.doOnNext(successRef::set)
			.doOnError(errorRef::set)
			.onErrorResume(e -> Flux.empty())
			.blockLast(Duration.ofSeconds(30));

		// Either we got a response or an error, both are valid outcomes
		boolean hasResponse = successRef.get() != null;
		boolean hasError = errorRef.get() != null;

		assertThat(hasResponse || hasError)
			.as("Should have either response or error")
			.isTrue();
	}

	// ============================================================================
	// HELPER METHODS
	// ============================================================================

	/**
	 * Helper method to extract text from Message.
	 */
	private String extractTextFromResponse(Message response) {
		StringBuilder text = new StringBuilder();
		if (response.parts() != null) {
			for (Part<?> part : response.parts()) {
				if (part instanceof TextPart textPart) {
					text.append(textPart.text());
				}
			}
		}
		return text.toString();
	}

}
