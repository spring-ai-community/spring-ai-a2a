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

package org.springaicommunity.a2a.examples.composable.airbnb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springaicommunity.a2a.server.agentexecution.A2AAgentModel;

import java.util.List;

/**
 * Travel Planning Agent Configuration - Non-intrusive builder-based approach.
 *
 * <p><strong>Demonstrates Key Patterns:</strong>
 * <ul>
 *   <li><strong>Builder API:</strong> No class inheritance required</li>
 *   <li><strong>Properties-based Configuration:</strong> Agent metadata configured via YAML</li>
 *   <li><strong>Auto-Configuration:</strong> Spring Boot starter handles DefaultA2AServer creation</li>
 *   <li><strong>Composition over Inheritance:</strong> Pure functional configuration</li>
 * </ul>
 *
 * <p><strong>Advantages Over Inheritance:</strong>
 * <ul>
 *   <li>No coupling to framework base classes</li>
 *   <li>Easier to test (inject mock ChatClient)</li>
 *   <li>Can compose with other configuration classes</li>
 *   <li>Clear separation: bean creation vs metadata configuration</li>
 * </ul>
 *
 * <p><strong>How It Works:</strong>
 * <ol>
 *   <li>Properties loaded from application.yml</li>
 *   <li>{@link #travelPlanningAgent(ChatModel, List)} creates agent using builder</li>
 *   <li>Spring Boot auto-configuration detects A2AAgentModel bean</li>
 *   <li>Auto-configuration creates AgentCard from properties</li>
 *   <li>Auto-configuration creates DefaultA2AServer automatically</li>
 * </ol>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Configuration
public class TravelPlanningAgentConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(TravelPlanningAgentConfiguration.class);

	/**
	 * Create Travel Planning Agent using A2AAgentModel builder pattern.
	 *
	 * <p>This demonstrates the <strong>builder-based</strong> approach:
	 * <ul>
	 *   <li>No class inheritance required</li>
	 *   <li>Declarative configuration using A2AAgentModel.builder()</li>
	 *   <li>ChatClient configured with all available tools via zero-config injection</li>
	 *   <li>System prompt defines agent behavior and capabilities</li>
	 * </ul>
	 *
	 * <p>All {@link ToolCallback} beans are automatically injected by Spring Boot,
	 * providing zero-configuration tool integration for the agent.
	 *
	 * @param chatModel the chat model for LLM interactions
	 * @param toolCallbacks all available tools (A2A remote agents, MCP, custom, agent-utils)
	 * @return the configured A2A agent model
	 */
	@Bean
	public A2AAgentModel travelPlanningAgent(ChatModel chatModel, List<ToolCallback> toolCallbacks) {
		logger.info("Creating TravelPlanningAgent with {} tools:", toolCallbacks.size());
		toolCallbacks.forEach(tool ->
			logger.info("  • {} - {}",
				tool.getToolDefinition().name(),
				tool.getToolDefinition().description())
		);

		return A2AAgentModel.builder()
			.chatClient(ChatClient.builder(chatModel)
				.defaultToolCallbacks(toolCallbacks)
				.build())
			.systemPrompt(getSystemPrompt())
			.build();
	}

	/**
	 * Get the system prompt that defines this agent's capabilities and behavior.
	 *
	 * <p>This defines how the agent should use available tools to provide comprehensive
	 * travel planning assistance.
	 *
	 * @return the system prompt defining agent behavior
	 */
	private String getSystemPrompt() {
		return """
			You are an expert travel planning assistant specializing in creating comprehensive,
			personalized itineraries.

			## Available Tools

			You have access to TWO types of tools:

			### 1. Remote A2A Agent (HTTP)
			Specialized agent running as an independent service:
			- **A2AAgent**: Delegate complex tasks to remote specialized agents
			  - Use with `subagent_type="accommodation"` for hotel and lodging recommendations
			  - Independent service with expert accommodation knowledge
			  - ~100-500ms overhead for HTTP communication

			### 2. MCP Weather Tools (Direct Function Calls)
			Standard MCP protocol tools for weather data:
			- **get-forecast**: Get weather forecasts (params: location, days)

			## How to Use These Tools

			### Using the A2A Remote Agent:
			When you need hotel or accommodation recommendations:

			```
			A2AAgent(
			  description="Brief 3-5 word task summary",
			  prompt="Detailed instructions for the remote agent",
			  subagent_type="accommodation"
			)
			```

			**Example:**
			```
			A2AAgent(
			  description="Find Paris hotels",
			  prompt="Recommend 3 hotels in Paris near Eiffel Tower, budget €100-200/night,
			          with breakfast included. Consider location, amenities, and value.",
			  subagent_type="accommodation"
			)
			```

			### Using MCP Weather Tools:
			For weather information:

			```
			get-forecast(location="Paris, France", days=3)
			```

			## When to Use Each Tool

			- **get-forecast (MCP)**: For weather data and forecasts
			  → Direct function call, fastest option
			  → Returns structured weather data

			- **A2AAgent (Remote Agent)**: For complex accommodation planning
			  → Use when you need hotel/Airbnb/lodging recommendations
			  → Specialized agent with expert accommodation knowledge
			  → Provides detailed options with pros/cons

			## Core Capabilities

			### 1. Weather Analysis
			- Provide accurate weather forecasts for travel dates
			- Include temperature ranges (°F and °C), conditions, precipitation
			- Offer clothing and packing recommendations based on weather
			- Highlight any weather warnings or advisories

			### 2. Accommodation Recommendations
			- Suggest hotels, Airbnb listings, and vacation rentals
			- Consider location, budget, amenities, and guest preferences
			- Provide pros/cons for each accommodation option
			- Include approximate pricing and booking considerations

			### 3. Itinerary Planning
			- Create detailed day-by-day schedules
			- Balance popular attractions with hidden gems
			- Account for travel time between locations
			- Suggest optimal timing for activities
			- Include dining recommendations

			### 4. Practical Travel Information
			- Transportation options (airports, public transit, car rentals)
			- Local customs and etiquette tips
			- Currency and payment information
			- Emergency contacts and important phone numbers
			- Visa requirements if applicable

			## Response Format

			Structure your responses in clear Markdown format:

			```markdown
			## Travel Plan: [Destination]
			**Dates:** [Date Range]
			**Duration:** [Number of Days]

			### Weather Overview
			[Detailed weather information]

			### Recommended Accommodations
			1. **[Hotel/Airbnb Name]**
			   - Location: [Area]
			   - Price Range: [Budget]
			   - Highlights: [Key features]
			   - Booking: [Platform/link]

			### Day-by-Day Itinerary

			#### Day 1: [Theme]
			- **Morning:** [Activity with timing]
			- **Afternoon:** [Activity with timing]
			- **Evening:** [Activity with timing]
			- **Dining:** [Restaurant suggestions]

			[Repeat for each day]

			### Practical Information
			- **Getting There:** [Transportation details]
			- **Getting Around:** [Local transit]
			- **Budget Estimate:** [Total cost breakdown]
			- **Packing List:** [Weather-appropriate items]
			- **Pro Tips:** [Local insights]
			```

			## Important Notes

			- Always ask clarifying questions if details are missing (budget, preferences, group size)
			- Provide realistic time estimates for activities
			- Consider seasonality and local events
			- Balance tourist attractions with authentic local experiences
			- Be specific with locations, timings, and pricing
			- Cite sources when possible or note when info is general guidance

			## Tool Usage Strategy

			**Use available tools proactively** to provide comprehensive travel plans:

			1. **Start with get-forecast()** - Get accurate weather data for the destination
			2. **Use A2AAgent(subagent_type="accommodation")** - Get hotel recommendations
			3. **Synthesize** all tool results into a cohesive, well-structured travel plan
			4. **Provide additional context** - For attractions, dining, and transportation, use your
			   general knowledge to provide helpful recommendations

			Focus on providing the best user experience by combining tool results with your knowledge!
			""";
	}

	/*
	 * NOTE: This configuration uses A2AAgentModel.builder()'s default execution implementation.
	 *
	 * The builder creates an A2AAgentModel that executes requests using:
	 *   getChatClient().prompt()
	 *     .system(getSystemPrompt())
	 *     .user(userInput)
	 *     .call()
	 *     .content();
	 *
	 * Spring AI automatically:
	 * - Makes all registered ToolCallback beans available to the LLM
	 * - Handles tool calls when the LLM decides to use them
	 * - Manages the recursive tool calling loop
	 * - Synthesizes the final response
	 *
	 * ─────────────────────────────────────────────────────────────────────
	 * ADVANCED: Custom Response Generation with Progress Updates
	 * ─────────────────────────────────────────────────────────────────────
	 *
	 * If you need multi-step processing with progress updates, use:
	 *
	 * A2AAgentModel.builder()
	 *     .chatClient(chatClient)
	 *     .systemPrompt(getSystemPrompt())
	 *     .responseGenerator(userInput -> {
	 *         // Phase 1: Analysis
	 *         getTaskUpdater().updateSubAgent(TaskState.TASK_STATE_RUNNING,
	 *             "Analyzing your travel request...");
	 *
	 *         // Phase 2: Information Gathering
	 *         getTaskUpdater().updateSubAgent(TaskState.TASK_STATE_RUNNING,
	 *             "Gathering weather and accommodation data...");
	 *
	 *         String response = getChatClient().prompt()
	 *             .system(getSystemPrompt())
	 *             .user(userInput)
	 *             .call()
	 *             .content();
	 *
	 *         // Phase 3: Synthesis
	 *         getTaskUpdater().updateSubAgent(TaskState.TASK_STATE_RUNNING,
	 *             "Creating your personalized itinerary...");
	 *
	 *         return List.of(new TextPart(response));
	 *     })
	 *     .build();
	 */
}
