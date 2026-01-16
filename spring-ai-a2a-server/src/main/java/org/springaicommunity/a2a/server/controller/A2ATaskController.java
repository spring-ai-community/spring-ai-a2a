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

import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
import org.springaicommunity.a2a.core.MessageUtils;
import org.springaicommunity.a2a.server.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for handling asynchronous task-based A2A communication.
 *
 * <p>This controller provides the Task API endpoints for managing long-running
 * operations:
 * <ul>
 *   <li>{@code POST /a2a} with {@code submitTask} method - Create and start a task</li>
 *   <li>{@code GET /a2a/tasks/{taskId}} - Retrieve task status and results</li>
 *   <li>{@code POST /a2a/tasks/{taskId}/cancel} - Cancel a running task</li>
 *   <li>{@code GET /a2a/tasks} - List all tasks</li>
 * </ul>
 *
 * <p>Tasks are executed asynchronously in the background using Spring's {@code @Async}
 * support. The user's {@link ChatClient} bean is injected and used to process the task.
 *
 * <p><strong>Usage:</strong>
 * <ol>
 *   <li>Application configures a {@link ChatClient} bean with system prompt</li>
 *   <li>Application enables async with {@code @EnableAsync}</li>
 *   <li>This controller handles all task lifecycle management automatically</li>
 * </ol>
 *
 * <p><strong>Example Configuration:</strong>
 * <pre>
 * &#64;SpringBootApplication
 * &#64;EnableAsync
 * public class MyAgentApplication {
 *     &#64;Bean
 *     public ChatClient chatClient(ChatModel chatModel) {
 *         return ChatClient.builder(chatModel)
 *             .defaultSystem("You are a helpful assistant...")
 *             .build();
 *     }
 * }
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@RestController
public class A2ATaskController {

	private static final Logger logger = LoggerFactory.getLogger(A2ATaskController.class);

	private final ChatClient chatClient;

	private final TaskRepository taskRepository;

	public A2ATaskController(ChatClient chatClient, TaskRepository taskRepository) {
		this.chatClient = chatClient;
		this.taskRepository = taskRepository;
	}

	/**
	 * Handle JSON-RPC requests for task-based communication.
	 *
	 * <p>Supports the {@code submitTask} method for asynchronous task execution.
	 * Creates a task, starts background execution, and returns the task ID immediately.
	 *
	 * @param requestBody the JSON-RPC request
	 * @return JSON-RPC response with task ID
	 */
	@PostMapping(path = "${spring.ai.a2a.server.base-path:/a2a}/tasks",
			consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> submitTask(@RequestBody Map<String, Object> requestBody) {
		try {
			String method = (String) requestBody.get("method");
			Object id = requestBody.get("id");

			// Normalize method name
			String normalizedMethod = method != null ? method.toLowerCase() : "";

			if (!"submittask".equals(normalizedMethod)) {
				Map<String, Object> errorResponse = createErrorResponse(id, -32601,
						"Method not found: " + method);
				return ResponseEntity.ok()
					.contentType(MediaType.APPLICATION_JSON)
					.body(errorResponse);
			}

			Map<String, Object> params = (Map<String, Object>) requestBody.get("params");
			Map<String, Object> response = handleSubmitTask(params, id);

			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(response);
		}
		catch (Exception e) {
			logger.error("Error handling task submission", e);
			Map<String, Object> errorResponse = createErrorResponse(requestBody.get("id"), -32603,
					"Internal error: " + e.getMessage());
			return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(errorResponse);
		}
	}

	/**
	 * Get task status and results.
	 *
	 * @param taskId the task ID
	 * @return the task object with current status
	 */
	@GetMapping(path = "${spring.ai.a2a.server.base-path:/a2a}/tasks/{taskId}",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Task> getTask(@PathVariable String taskId) {
		return taskRepository.get(taskId)
			.map(ResponseEntity::ok)
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * Cancel a running task.
	 *
	 * @param taskId the task ID
	 * @return the updated task with CANCELED status
	 */
	@PostMapping(path = "${spring.ai.a2a.server.base-path:/a2a}/tasks/{taskId}/cancel",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Task> cancelTask(@PathVariable String taskId) {
		return taskRepository.get(taskId)
			.map(task -> {
				TaskState currentState = task.status().state();

				// Only cancel if task is in SUBMITTED or WORKING state
				if (currentState == TaskState.SUBMITTED || currentState == TaskState.WORKING) {
					Task canceledTask = Task.builder()
						.id(task.id())
						.contextId(task.contextId())
						.status(new TaskStatus(TaskState.CANCELED))
						.artifacts(task.artifacts())
						.build();
					taskRepository.save(canceledTask);
					return ResponseEntity.ok(canceledTask);
				}
				else {
					// Task already in terminal state
					return ResponseEntity.ok(task);
				}
			})
			.orElse(ResponseEntity.notFound().build());
	}

	/**
	 * List all tasks.
	 *
	 * @return list of all tasks
	 */
	@GetMapping(path = "${spring.ai.a2a.server.base-path:/a2a}/tasks",
			produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Task> listTasks() {
		return taskRepository.listAll();
	}

	/**
	 * Handle submitTask JSON-RPC method.
	 */
	private Map<String, Object> handleSubmitTask(Map<String, Object> params, Object id) {
		try {
			// Extract or generate contextId
			String contextId = (String) params.get("contextId");
			if (contextId == null || contextId.isEmpty()) {
				contextId = UUID.randomUUID().toString();
			}

			// Parse the message
			Map<String, Object> messageMap = (Map<String, Object>) params.get("message");
			Message userMessage = parseMessage(messageMap);
			String userText = MessageUtils.extractText(userMessage);

			// Create a new task in SUBMITTED state
			String taskId = UUID.randomUUID().toString();
			Task task = Task.builder()
				.id(taskId)
				.contextId(contextId)
				.status(new TaskStatus(TaskState.SUBMITTED))
				.artifacts(new ArrayList<>())
				.build();

			taskRepository.save(task);
			logger.info("Created task {} in SUBMITTED state", taskId);

			// Execute task asynchronously
			executeTaskAsync(taskId, contextId, userText);

			// Return immediate response with taskId
			Map<String, Object> result = new HashMap<>();
			result.put("taskId", taskId);

			return createSuccessResponse(id, result);
		}
		catch (Exception e) {
			logger.error("Error submitting task", e);
			return createErrorResponse(id, -32603, "Error submitting task: " + e.getMessage());
		}
	}

	/**
	 * Execute task asynchronously in background thread.
	 */
	@Async
	protected void executeTaskAsync(String taskId, String contextId, String userText) {
		try {
			// Update state to WORKING
			Task workingTask = Task.builder()
				.id(taskId)
				.contextId(contextId)
				.status(new TaskStatus(TaskState.WORKING))
				.artifacts(new ArrayList<>())
				.build();
			taskRepository.save(workingTask);
			logger.info("Task {} transitioned to WORKING", taskId);

			// Check if task was canceled
			Task currentTask = taskRepository.get(taskId).orElse(null);
			if (currentTask != null && currentTask.status().state() == TaskState.CANCELED) {
				logger.info("Task {} was canceled before execution", taskId);
				return;
			}

			// Call ChatClient to process the request
			String responseText = chatClient.prompt()
				.user(userText)
				.call()
				.content();

			// Check again if task was canceled during execution
			currentTask = taskRepository.get(taskId).orElse(null);
			if (currentTask != null && currentTask.status().state() == TaskState.CANCELED) {
				logger.info("Task {} was canceled during execution", taskId);
				return;
			}

			// Create artifact with the response
			Artifact artifact = Artifact.builder()
				.artifactId(UUID.randomUUID().toString())
				.name("response")
				.description("Agent response")
				.parts(List.of(new TextPart(responseText)))
				.build();

			// Update task to COMPLETED with artifact
			Task completedTask = Task.builder()
				.id(taskId)
				.contextId(contextId)
				.status(new TaskStatus(TaskState.COMPLETED))
				.artifacts(List.of(artifact))
				.build();

			taskRepository.save(completedTask);
			logger.info("Task {} completed successfully", taskId);
		}
		catch (Exception e) {
			logger.error("Error executing task " + taskId, e);

			// Update task to FAILED
			try {
				Task failedTask = Task.builder()
					.id(taskId)
					.contextId(contextId)
					.status(new TaskStatus(TaskState.FAILED))
					.artifacts(new ArrayList<>())
					.build();
				taskRepository.save(failedTask);
			}
			catch (Exception storageError) {
				logger.error("Failed to save FAILED task state", storageError);
			}
		}
	}

	/**
	 * Parse a message from JSON-RPC params map.
	 */
	private Message parseMessage(Map<String, Object> messageMap) {
		String roleStr = (String) messageMap.get("role");

		// Handle various role name formats
		String normalizedRole = roleStr.toUpperCase();
		if (normalizedRole.startsWith("ROLE_")) {
			normalizedRole = normalizedRole.substring(5);
		}

		Message.Role role = Message.Role.valueOf(normalizedRole);

		List<Map<String, Object>> partsList = (List<Map<String, Object>>) messageMap.get("parts");
		List<Part<?>> parts = new ArrayList<>();

		for (Map<String, Object> partMap : partsList) {
			String type = (String) partMap.get("type");
			if ("text".equals(type) || type == null) {
				String text = (String) partMap.get("text");
				if (text != null) {
					parts.add(new TextPart(text));
				}
			}
		}

		return Message.builder().role(role).parts(parts).build();
	}

	/**
	 * Create a JSON-RPC success response.
	 */
	private Map<String, Object> createSuccessResponse(Object id, Map<String, Object> result) {
		Map<String, Object> response = new HashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		response.put("result", result);
		return response;
	}

	/**
	 * Create a JSON-RPC error response.
	 */
	private Map<String, Object> createErrorResponse(Object id, int code, String message) {
		Map<String, Object> error = new HashMap<>();
		error.put("code", code);
		error.put("message", message);

		Map<String, Object> response = new HashMap<>();
		response.put("jsonrpc", "2.0");
		response.put("id", id);
		response.put("error", error);
		return response;
	}

}
