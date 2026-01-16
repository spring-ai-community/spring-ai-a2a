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

package org.springaicommunity.a2a.util.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agent.tools.task.TaskOutputTool;
import org.springaicommunity.agent.tools.task.repository.DefaultTaskRepository;
import org.springaicommunity.agent.tools.task.repository.TaskRepository;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Task Management support in Spring AI A2A.
 *
 * <p>This configuration provides automatic setup for background task execution
 * and retrieval using the {@link TaskRepository} and {@link TaskOutputTool} from
 * spring-ai-agent-utils.
 *
 * <p><strong>What This Provides:</strong>
 * <ul>
 *   <li>{@link TaskRepository} - For managing background tasks created by {@link org.springaicommunity.a2a.core.tool.A2AToolCallback}</li>
 *   <li>{@link TaskOutputTool} - ToolCallback for retrieving task results via LLM tool calls</li>
 * </ul>
 *
 * <p><strong>Default Configuration:</strong>
 * <pre>
 * // DefaultTaskRepository is auto-configured
 * TaskRepository taskRepository = ...; // Uses ConcurrentHashMap internally
 *
 * // TaskOutputTool is auto-configured and registered as a ToolCallback
 * ToolCallback taskOutputTool = TaskOutputTool.builder()
 *     .taskRepository(taskRepository)
 *     .build();
 * </pre>
 *
 * <p><strong>Custom Configuration:</strong>
 * <p>You can override these beans to provide custom implementations:
 * <pre>
 * {@literal @}Configuration
 * public class CustomTaskConfiguration {
 *
 *     // Custom TaskRepository (e.g., Redis-backed for distributed tasks)
 *     {@literal @}Bean
 *     public TaskRepository taskRepository() {
 *         return new RedisTaskRepository(...);
 *     }
 *
 *     // Custom TaskOutputTool with custom template
 *     {@literal @}Bean
 *     public ToolCallback taskOutputTool(TaskRepository taskRepository) {
 *         return TaskOutputTool.builder()
 *             .taskRepository(taskRepository)
 *             .taskDescriptionTemplate("Custom template for task output")
 *             .build();
 *     }
 * }
 * </pre>
 *
 * <p><strong>Integration with A2AToolCallback:</strong>
 * <p>The {@link org.springaicommunity.a2a.core.tool.A2AToolCallback} automatically uses
 * the configured TaskRepository for background task management. When an A2A agent
 * tool is called with {@code run_in_background: true}, tasks are stored in the
 * TaskRepository and can be retrieved using the TaskOutputTool.
 *
 * <p><strong>Usage Example:</strong>
 * <pre>
 * // Agent executor with A2A tools and TaskOutputTool
 * {@literal @}Component
 * public class MyAgent extends DefaultSpringAIAgentExecutor {
 *
 *     public MyAgent(ChatModel chatModel, List&lt;ToolCallback&gt; toolCallbacks) {
 *         // toolCallbacks includes:
 *         // - A2AToolCallback (for delegating to remote agents)
 *         // - TaskOutputTool (for retrieving background task results)
 *         super(chatModel, toolCallbacks);
 *     }
 *
 *     {@literal @}Override
 *     public String getSystemPrompt() {
 *         return """
 *             You can delegate tasks to remote agents using the A2A tool.
 *             For long-running operations, use run_in_background: true.
 *             To check results, use the TaskOutput tool with the task_id.
 *             """;
 *     }
 * }
 * </pre>
 *
 * @see TaskRepository
 * @see TaskOutputTool
 * @see org.springaicommunity.a2a.core.tool.A2AToolCallback
 * @author Spring AI Community
 */
@Configuration
public class TaskConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(TaskConfiguration.class);

	/**
	 * Provides a default {@link TaskRepository} implementation for managing background tasks.
	 *
	 * <p>This bean uses {@link DefaultTaskRepository}, which stores tasks in-memory
	 * using a {@link java.util.concurrent.ConcurrentHashMap}. For distributed applications
	 * or persistent storage, provide your own TaskRepository bean.
	 *
	 * <p><strong>Bean Override:</strong>
	 * <pre>
	 * {@literal @}Bean
	 * public TaskRepository taskRepository() {
	 *     return new RedisTaskRepository(...); // Custom implementation
	 * }
	 * </pre>
	 *
	 * @return a new DefaultTaskRepository instance
	 */
	@Bean
	@ConditionalOnMissingBean
	public TaskRepository taskRepository() {
		logger.info("Auto-configuring DefaultTaskRepository for background task management");
		return new DefaultTaskRepository();
	}

	/**
	 * Provides the {@link TaskOutputTool} as a Spring AI {@link ToolCallback}.
	 *
	 * <p>This tool allows the LLM to retrieve results from background tasks created
	 * by {@link org.springaicommunity.a2a.core.tool.A2AToolCallback}. When an A2A agent
	 * tool is invoked with {@code run_in_background: true}, it returns a task_id.
	 * The LLM can then use this tool to check the task status and retrieve results.
	 *
	 * <p><strong>Tool Function Signature:</strong>
	 * <pre>
	 * TaskOutput(task_id: string) -> string
	 * </pre>
	 *
	 * <p><strong>Example LLM Usage:</strong>
	 * <pre>
	 * // Step 1: Start background task
	 * A2AAgent(subagent_type: "research", prompt: "Research quantum computing", run_in_background: true)
	 * // Returns: "Task ID: abc-123"
	 *
	 * // Step 2: Check task output
	 * TaskOutput(task_id: "abc-123")
	 * // Returns task result or status
	 * </pre>
	 *
	 * <p><strong>Bean Override:</strong>
	 * <pre>
	 * {@literal @}Bean
	 * public ToolCallback taskOutputTool(TaskRepository taskRepository) {
	 *     return TaskOutputTool.builder()
	 *         .taskRepository(taskRepository)
	 *         .taskDescriptionTemplate("Custom description template")
	 *         .build();
	 * }
	 * </pre>
	 *
	 * @param taskRepository the TaskRepository to use for task retrieval
	 * @return a ToolCallback for retrieving background task results
	 */
	@Bean
	@ConditionalOnMissingBean(name = "taskOutputTool")
	public ToolCallback taskOutputTool(TaskRepository taskRepository) {
		logger.info("Auto-configuring TaskOutputTool for background task result retrieval");
		return TaskOutputTool.builder().taskRepository(taskRepository).build();
	}

}
