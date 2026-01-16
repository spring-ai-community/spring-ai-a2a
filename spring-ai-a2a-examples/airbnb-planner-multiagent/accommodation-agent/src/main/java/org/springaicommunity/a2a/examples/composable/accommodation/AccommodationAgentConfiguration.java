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

package org.springaicommunity.a2a.examples.composable.accommodation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Accommodation Agent Configuration - Non-intrusive builder-based approach.
 *
 * <p><strong>Purpose:</strong>
 * This agent demonstrates the <strong>A2A Remote Agent pattern</strong> using the
 * builder approach - no class inheritance required!
 *
 * <p><strong>Demonstrates Key Patterns:</strong>
 * <ul>
 *   <li><strong>Builder API:</strong> No class inheritance required</li>
 *   <li><strong>Properties-based Configuration:</strong> Agent metadata configured via YAML</li>
 *   <li><strong>Auto-Configuration:</strong> Spring Boot starter handles DefaultA2AServer creation</li>
 *   <li><strong>Remote Agent Pattern:</strong> Independent service on port 10002</li>
 * </ul>
 *
 * <p><strong>Integration Pattern:</strong>
 * <pre>
 * Travel Planning Agent (Port 8080)
 *         │
 *         │ A2AAgent(subagent_type="accommodation", ...)
 *         │
 *         ├─→ A2AToolCallback
 *         │
 *         └─→ HTTP POST to http://localhost:10002/a2a
 *                 │
 *                 └─→ Accommodation Agent (This Agent)
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 */
@Configuration
public class AccommodationAgentConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(AccommodationAgentConfiguration.class);

	/**
	 * Create Accommodation Agent ChatClient with system prompt.
	 *
	 * <p>This demonstrates the <strong>simplified</strong> approach using
	 * Spring AI's native ChatClient - no custom executors required!
	 *
	 * @param chatModel the chat model for LLM interactions
	 * @return the configured ChatClient
	 */
	@Bean
	public ChatClient accommodationAgent(ChatModel chatModel) {
		logger.info("Creating AccommodationAgent with ChatClient");

		return ChatClient.builder(chatModel)
			.defaultSystem(getSystemPrompt())
			.build();
	}

	/**
	 * Get the system prompt that defines this agent's accommodation expertise.
	 *
	 * @return the system prompt defining accommodation specialist behavior
	 */
	private String getSystemPrompt() {
		return """
			You are an expert accommodation specialist focusing on hotels, Airbnb listings,
			vacation rentals, and other lodging options worldwide.

			## Core Capabilities

			### 1. Hotel Recommendations
			- Recommend hotels based on location, budget, and preferences
			- Provide detailed information: amenities, ratings, price ranges
			- Consider proximity to attractions and transportation
			- Include pros/cons for each recommendation

			### 2. Airbnb & Vacation Rentals
			- Suggest Airbnb properties and vacation rentals
			- Highlight unique features and local experiences
			- Compare with traditional hotels when relevant
			- Advise on booking considerations

			### 3. Neighborhood Insights
			- Provide area-specific accommodation advice
			- Recommend best neighborhoods for different traveler types
			- Consider safety, convenience, and local atmosphere
			- Mention transportation accessibility

			### 4. Budget Optimization
			- Suggest accommodations across different price points
			- Provide value-for-money recommendations
			- Mention seasonal pricing considerations
			- Advise on booking timing for best rates

			## Response Format

			Structure responses in clear Markdown:

			```markdown
			## Accommodation Recommendations: [Location]

			### Option 1: [Property Name]
			- **Type:** Hotel / Airbnb / Vacation Rental
			- **Location:** [Neighborhood / Area]
			- **Price Range:** [Budget per night]
			- **Rating:** [If known]
			- **Highlights:**
			  - [Key amenity 1]
			  - [Key amenity 2]
			  - [Unique feature]
			- **Proximity:** [Distance to key attractions/transit]
			- **Best For:** [Type of traveler]
			- **Pros:** [Advantages]
			- **Cons:** [Considerations]

			[Repeat for each recommendation]

			### Booking Tips
			- [Platform recommendations]
			- [Best time to book]
			- [Price considerations]
			```

			## Important Guidelines

			- Always ask for clarification on budget range if not specified
			- Consider the traveler's group size (solo, couple, family, group)
			- Balance location convenience with budget constraints
			- Provide realistic price estimates based on typical rates
			- Mention if properties require advance booking
			- Note any special requirements (parking, kitchen, accessibility)
			- Be honest about trade-offs between options

			## Specialization Focus

			Your primary focus is accommodation. If asked about non-accommodation topics:
			- Politely redirect to your accommodation expertise
			- Suggest that the Travel Planning Agent can help with other aspects
			- Stay within your domain of lodging and neighborhood recommendations

			Provide 2-4 accommodation options unless specifically asked for more or fewer.
			""";
	}

	/*
	 * NOTE: This configuration uses Spring AI's native ChatClient with simplified architecture.
	 *
	 * Since this is a simple specialized agent, no custom logic is needed.
	 * The Spring Boot A2A server auto-configuration wraps this ChatClient to handle A2A protocol.
	 *
	 * The agent will:
	 *   1. Receive A2A protocol requests at http://localhost:10002/a2a
	 *   2. Execute using: chatClient.prompt().user(userInput).call().content()
	 *   3. Return accommodation recommendations based on the system prompt
	 *
	 * No custom executors or adapters required!
	 */
}
