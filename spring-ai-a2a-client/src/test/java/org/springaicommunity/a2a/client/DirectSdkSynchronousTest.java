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

package org.springaicommunity.a2a.client;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests demonstrating direct A2A SDK usage for synchronous execution.
 *
 * <p>These tests show how to use the A2A Java SDK directly without any wrapper
 * abstractions for simple synchronous request/response patterns.
 *
 * <p><strong>Note:</strong> These are example tests that require a running A2A agent.
 * Mark with {@code @Disabled} or configure with test containers for CI.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Disabled("Requires a running A2A agent on localhost:8080")
class DirectSdkSynchronousTest {

	private static final String TEST_AGENT_URL = "http://localhost:8080/a2a";
	private static final Duration TIMEOUT = Duration.ofSeconds(30);

	/**
	 * Test basic synchronous message sending using direct A2A SDK.
	 *
	 * <p>This test demonstrates:
	 * <ul>
	 *   <li>Building AgentCard and ClientConfig manually
	 *   <li>Setting up CountDownLatch for synchronous blocking
	 *   <li>Creating and registering BiConsumer event handler
	 *   <li>Sending a message and waiting for response
	 * </ul>
	 */
	@Test
	void testSynchronousMessageWithDirectSdk() throws Exception {
		// 1. Build AgentCard
		AgentCard agentCard = AgentCard.builder()
			.name("Test Agent")
			.description("Test A2A agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(io.a2a.spec.AgentCapabilities.builder().build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(
				new io.a2a.spec.AgentInterface("JSONRPC", TEST_AGENT_URL)
			))
			.build();

		// 2. Build ClientConfig
		io.a2a.client.config.ClientConfig clientConfig =
			new io.a2a.client.config.ClientConfig.Builder()
				.setStreaming(false)
				.setAcceptedOutputModes(List.of("text"))
				.build();

		// 3. Prepare request
		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Hello, agent!")))
			.build();

		// 4. Setup synchronous response handling
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			try {
				if (event instanceof MessageEvent messageEvent) {
					responseRef.set(messageEvent.getMessage());
					latch.countDown();
				}
			}
			catch (Exception e) {
				errorRef.set(e);
				latch.countDown();
			}
		};

		// 5. Build client with consumer registered
		Client client = Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(List.of(consumer))
			.build();

		// 6. Send message
		client.sendMessage(request);

		// 7. Wait for response
		boolean completed = latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

		// 8. Assert results
		assertTrue(completed, "Should complete within timeout");
		assertNull(errorRef.get(), "Should not have errors");
		assertNotNull(responseRef.get(), "Should receive response");

		Message response = responseRef.get();
		assertNotNull(response.parts(), "Response should have parts");
		assertFalse(response.parts().isEmpty(), "Response parts should not be empty");
	}

	/**
	 * Test synchronous execution with manual client configuration.
	 *
	 * <p>This test demonstrates building a client completely from scratch
	 * without using A2AClientUtils, showing full control over configuration.
	 */
	@Test
	void testSynchronousMessageWithManualConfiguration() throws Exception {
		// 1. Build AgentCard manually
		AgentCard agentCard = AgentCard.builder()
			.name("Test Agent")
			.description("Test A2A agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(io.a2a.spec.AgentCapabilities.builder().build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(
				new io.a2a.spec.AgentInterface("JSONRPC", TEST_AGENT_URL)
			))
			.build();

		// 2. Build ClientConfig manually
		io.a2a.client.config.ClientConfig clientConfig =
			new io.a2a.client.config.ClientConfig.Builder()
				.setStreaming(false)
				.setAcceptedOutputModes(List.of("text"))
				.build();

		// 3. Create message
		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Test message")))
			.build();

		// 4. Setup response handling
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();

		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			if (event instanceof MessageEvent messageEvent) {
				responseRef.set(messageEvent.getMessage());
				latch.countDown();
			}
		};

		// 5. Build client with all configuration
		Client client = Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(List.of(consumer))
			.build();

		// 6. Send and wait
		client.sendMessage(request);
		boolean completed = latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

		// 7. Verify
		assertTrue(completed, "Should complete within timeout");
		assertNotNull(responseRef.get(), "Should receive response");
	}

	/**
	 * Test timeout handling in synchronous execution.
	 *
	 * <p>Demonstrates how timeouts work when the agent doesn't respond
	 * within the configured timeout period.
	 */
	@Test
	void testSynchronousTimeout() throws Exception {
		// Use very short timeout to test timeout behavior
		Duration shortTimeout = Duration.ofMillis(100);

		AgentCard agentCard = AgentCard.builder()
			.name("Test Agent")
			.description("Test A2A agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(io.a2a.spec.AgentCapabilities.builder().build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(
				new io.a2a.spec.AgentInterface("JSONRPC", TEST_AGENT_URL)
			))
			.build();

		io.a2a.client.config.ClientConfig clientConfig =
			new io.a2a.client.config.ClientConfig.Builder()
				.setStreaming(false)
				.setAcceptedOutputModes(List.of("text"))
				.build();

		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Slow request")))
			.build();

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();

		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			if (event instanceof MessageEvent messageEvent) {
				responseRef.set(messageEvent.getMessage());
				latch.countDown();
			}
		};

		Client client = Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(List.of(consumer))
			.build();

		client.sendMessage(request);

		// Wait with the short timeout
		boolean completed = latch.await(shortTimeout.toMillis(), TimeUnit.MILLISECONDS);

		// In most cases, this will timeout
		// The actual assertion depends on whether the agent responds quickly enough
		// This test primarily demonstrates the timeout pattern
	}

	/**
	 * Test error handling in synchronous execution.
	 *
	 * <p>Shows how to capture and handle errors that occur during
	 * message processing using the error reference pattern.
	 */
	@Test
	void testSynchronousErrorHandling() throws Exception {
		AgentCard agentCard = AgentCard.builder()
			.name("Test Agent")
			.description("Test A2A agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(io.a2a.spec.AgentCapabilities.builder().build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(
				new io.a2a.spec.AgentInterface("JSONRPC", TEST_AGENT_URL)
			))
			.build();

		io.a2a.client.config.ClientConfig clientConfig =
			new io.a2a.client.config.ClientConfig.Builder()
				.setStreaming(false)
				.setAcceptedOutputModes(List.of("text"))
				.build();

		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("Test message")))
			.build();

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();
		AtomicReference<Throwable> errorRef = new AtomicReference<>();

		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			try {
				if (event instanceof MessageEvent messageEvent) {
					responseRef.set(messageEvent.getMessage());
					latch.countDown();
				}
			}
			catch (Exception e) {
				// Capture any error that occurs during event handling
				errorRef.set(e);
				latch.countDown();
			}
		};

		Client client = Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(List.of(consumer))
			.build();

		client.sendMessage(request);
		boolean completed = latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

		assertTrue(completed, "Should complete (either success or error)");

		// Check if error occurred
		if (errorRef.get() != null) {
			// Handle error case
			Throwable error = errorRef.get();
			assertNotNull(error, "Error should be captured");
			// In real code, you might log or handle the error
		}
		else {
			// Handle success case
			assertNotNull(responseRef.get(), "Should have response if no error");
		}
	}

	/**
	 * Test extracting text content from response message.
	 *
	 * <p>Demonstrates the pattern for extracting text from the Message
	 * response's parts collection.
	 */
	@Test
	void testExtractingTextFromResponse() throws Exception {
		AgentCard agentCard = AgentCard.builder()
			.name("Test Agent")
			.description("Test A2A agent")
			.version("1.0.0")
			.protocolVersion("0.1.0")
			.capabilities(io.a2a.spec.AgentCapabilities.builder().build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of())
			.supportedInterfaces(List.of(
				new io.a2a.spec.AgentInterface("JSONRPC", TEST_AGENT_URL)
			))
			.build();

		io.a2a.client.config.ClientConfig clientConfig =
			new io.a2a.client.config.ClientConfig.Builder()
				.setStreaming(false)
				.setAcceptedOutputModes(List.of("text"))
				.build();

		Message request = Message.builder()
			.role(Message.Role.USER)
			.parts(List.of(new TextPart("What is 2+2?")))
			.build();

		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Message> responseRef = new AtomicReference<>();

		BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
			if (event instanceof MessageEvent messageEvent) {
				responseRef.set(messageEvent.getMessage());
				latch.countDown();
			}
		};

		Client client = Client.builder(agentCard)
			.clientConfig(clientConfig)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumers(List.of(consumer))
			.build();

		client.sendMessage(request);
		latch.await(TIMEOUT.toSeconds(), TimeUnit.SECONDS);

		Message response = responseRef.get();
		assertNotNull(response, "Should have response");

		// Extract text from parts
		String text = extractTextFromMessage(response);
		assertNotNull(text, "Should have text content");
		assertFalse(text.isEmpty(), "Text should not be empty");
	}

	/**
	 * Helper method to extract text content from a Message.
	 */
	private String extractTextFromMessage(Message message) {
		if (message == null || message.parts() == null || message.parts().isEmpty()) {
			return "";
		}

		StringBuilder result = new StringBuilder();
		for (Object part : message.parts()) {
			if (part instanceof TextPart textPart) {
				result.append(textPart.text());
			}
		}
		return result.toString();
	}

}
