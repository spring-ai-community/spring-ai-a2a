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

package org.springaicommunity.a2a.server.repository;

import io.a2a.spec.Task;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for Task management.
 *
 * <p>This component provides thread-safe storage and retrieval of tasks
 * created by the A2A Task API endpoints.
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Component
public class TaskRepository {

	private final ConcurrentHashMap<String, Task> tasks = new ConcurrentHashMap<>();

	/**
	 * Save or update a task in storage.
	 * @param task the task to save
	 * @throws IllegalArgumentException if task or task ID is null
	 */
	public void save(Task task) {
		if (task == null || task.id() == null) {
			throw new IllegalArgumentException("Task and task ID must not be null");
		}
		tasks.put(task.id(), task);
	}

	/**
	 * Retrieve a task by its ID.
	 * @param taskId the task ID
	 * @return Optional containing the task if found, empty otherwise
	 */
	public Optional<Task> get(String taskId) {
		return Optional.ofNullable(tasks.get(taskId));
	}

	/**
	 * List all tasks in storage.
	 * @return immutable list of all tasks
	 */
	public List<Task> listAll() {
		return List.copyOf(tasks.values());
	}

	/**
	 * Delete a task from storage.
	 * @param taskId the task ID
	 * @return true if task was deleted, false if not found
	 */
	public boolean delete(String taskId) {
		return tasks.remove(taskId) != null;
	}

	/**
	 * Check if a task exists.
	 * @param taskId the task ID
	 * @return true if task exists, false otherwise
	 */
	public boolean exists(String taskId) {
		return tasks.containsKey(taskId);
	}

}
