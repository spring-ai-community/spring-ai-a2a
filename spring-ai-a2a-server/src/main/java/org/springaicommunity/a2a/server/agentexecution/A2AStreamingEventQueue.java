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

import java.util.ArrayList;
import java.util.List;

import io.a2a.server.events.EventQueue;
import io.a2a.spec.Artifact;
import io.a2a.spec.Event;
import io.a2a.spec.Message;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatusUpdateEvent;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Sinks;

import org.springaicommunity.a2a.core.MessageUtils;

/**
 * EventQueue implementation that converts A2A events to Reactive Flux emissions.
 *
 * <p>This utility class bridges the A2A SDK's event-based execution model with
 * Spring's Reactive Streams (Flux) model. It's used by streaming agent execution
 * to emit incremental responses as they become available.
 *
 * <p>Extracted from DefaultSpringAIAgentExecutor for reusability across different
 * agent implementations.
 *
 * <p><strong>Usage:</strong>
 * <pre>
 * Flux&lt;Message&gt; stream = Flux.create(sink -> {
 *     A2AStreamingEventQueue eventQueue = new A2AStreamingEventQueue(sink);
 *     RequestContext context = buildContext(request);
 *     agentExecutor.execute(context, eventQueue);
 * });
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
public class A2AStreamingEventQueue extends EventQueue {

	private final Sinks.Many<Message> sink;

	private final List<io.a2a.spec.Part<?>> currentArtifactParts = new ArrayList<>();

	/**
	 * Create a streaming event queue that emits to the provided FluxSink.
	 * @param fluxSink the FluxSink to emit messages to
	 */
	public A2AStreamingEventQueue(FluxSink<Message> fluxSink) {
		// Wrap FluxSink in a Sinks.Many for better control
		this.sink = Sinks.many().multicast().onBackpressureBuffer();
		// Connect the sink to the provided FluxSink
		this.sink.asFlux().subscribe(fluxSink::next, fluxSink::error, fluxSink::complete);
	}

	@Override
	public void enqueueEvent(Event event) {
		processEvent(event);
	}

	/**
	 * Process an A2A event and emit corresponding messages to the Flux.
	 * @param event the A2A event to process
	 */
	private void processEvent(Event event) {
		if (event instanceof TaskArtifactUpdateEvent artifactEvent) {
			handleArtifactUpdate(artifactEvent);
		}
		else if (event instanceof TaskStatusUpdateEvent statusEvent) {
			handleStatusUpdate(statusEvent);
		}
	}

	/**
	 * Handle task artifact updates by emitting the artifact parts as a message.
	 * @param artifactEvent the artifact update event
	 */
	private void handleArtifactUpdate(TaskArtifactUpdateEvent artifactEvent) {
		Artifact artifact = artifactEvent.artifact();
		if (artifact != null && artifact.parts() != null) {
			synchronized (this.currentArtifactParts) {
				this.currentArtifactParts.addAll(artifact.parts());
			}
			// Emit the artifact parts as a message
			Message message = MessageUtils.assistantMessage(new ArrayList<>(artifact.parts()));
			this.sink.tryEmitNext(message);
		}
	}

	/**
	 * Handle task status updates by completing or erroring the stream.
	 * @param statusEvent the status update event
	 */
	private void handleStatusUpdate(TaskStatusUpdateEvent statusEvent) {
		TaskState state = statusEvent.status().state();
		if (state == TaskState.COMPLETED) {
			this.sink.tryEmitComplete();
		}
		else if (state == TaskState.FAILED || state == TaskState.CANCELED) {
			this.sink.tryEmitError(new RuntimeException("Task ended with state: " + state));
		}
	}

	@Override
	public EventQueue tap() {
		throw new UnsupportedOperationException("Streaming event queue does not support tapping");
	}

	@Override
	public void awaitQueuePollerStart() throws InterruptedException {
		// No-op for streaming execution
	}

	@Override
	public void signalQueuePollerStarted() {
		// No-op for streaming execution
	}

	@Override
	public void close() {
		this.sink.tryEmitComplete();
	}

	@Override
	public void close(boolean immediate) {
		this.sink.tryEmitComplete();
	}

	@Override
	public void close(boolean immediate, boolean notifyParent) {
		this.sink.tryEmitComplete();
	}

}
