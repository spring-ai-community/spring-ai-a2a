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

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Airbnb Travel Planner - Multi-Agent Example Application.
 *
 * <p>This example demonstrates a composable multi-agent architecture using:
 * <ul>
 *   <li><strong>Remote A2A Agents</strong> - Via A2AToolCallback</li>
 *   <li><strong>MCP Tools</strong> - Standard protocol weather tools</li>
 *   <li><strong>Optional: Local Tools</strong> - Via spring-ai-agent-utils</li>
 * </ul>
 *
 * <p><strong>Architecture:</strong>
 * <pre>
 * Travel Planning Agent (Port 8080)
 *   │
 *   ├─→ A2AToolCallback
 *   │    └─→ Accommodation Agent (Port 10002)
 *   │
 *   ├─→ MCP Weather Tool (get-forecast)
 *   │
 *   └─→ (Optional) spring-ai-agent-utils tools
 * </pre>
 *
 * <p><strong>How It Works:</strong>
 * <ol>
 *   <li>User sends request to http://localhost:8080/a2a</li>
 *   <li>Travel Planning Agent has ChatClient with all ToolCallbacks</li>
 *   <li>LLM autonomously decides which tools to use</li>
 *   <li>A2AToolCallback delegates to remote accommodation agent</li>
 *   <li>MCP tool provides weather forecasts</li>
 *   <li>LLM synthesizes comprehensive travel plan</li>
 * </ol>
 *
 * <p><strong>Configuration:</strong>
 * <ul>
 *   <li><strong>A2AToolConfiguration</strong> - Registers A2AToolCallback</li>
 *   <li><strong>TravelPlanningAgentConfiguration</strong> - Creates agent with all tools</li>
 *   <li><strong>application.yml</strong> - Configures remote agent URLs and MCP</li>
 * </ul>
 *
 * <p><strong>Zero-Config Tool Injection:</strong>
 * All ToolCallback beans are auto-injected via {@code List<ToolCallback>}:
 * <pre>
 * public A2AAgentModel travelPlanningAgent(
 *         ChatModel chatModel,
 *         List&lt;ToolCallback&gt; toolCallbacks) {  // Auto-injected!
 *
 *     return A2AAgentModel.builder()
 *         .chatClient(ChatClient.builder(chatModel)
 *             .defaultToolCallbacks(toolCallbacks)
 *             .build())
 *         .build();
 * }
 * </pre>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see org.springaicommunity.a2a.client.tool.A2AToolCallback
 * @see TravelPlanningAgentConfiguration
 * @see A2AToolConfiguration
 */
@SpringBootApplication
public class AirbnbPlannerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AirbnbPlannerApplication.class, args);
	}

	/**
	 * Display startup information showing the multi-agent architecture.
	 *
	 * @return CommandLineRunner for startup logging
	 */
	@Bean
	public CommandLineRunner startupInfo() {
		return args -> {
			System.out.println("\n" + "=".repeat(80));
			System.out.println("🚀 Airbnb Travel Planner Agent Started!");
			System.out.println("=".repeat(80));

			System.out.println("\n📋 Agent Details:");
			System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
			System.out.println("  │  Travel Planning Agent (Port 8080)                          │");
			System.out.println("  │  • Builder API pattern (A2AAgentModel)                      │");
			System.out.println("  │  • Zero-config tool injection                               │");
			System.out.println("  │  • Spring Boot auto-configuration                           │");
			System.out.println("  │  • URL: http://localhost:8080/a2a                           │");
			System.out.println("  └─────────────────────────────────────────────────────────────┘");

			System.out.println("\n🛠️  Available Tools:");
			System.out.println("  Spring AI automatically provides these tools to the LLM:");
			System.out.println("  • A2AAgent - Delegate to remote A2A agents");
			System.out.println("  • get-forecast - MCP weather tool (stdio protocol)");
			System.out.println("  • (Optional) spring-ai-agent-utils tools if dependency added");

			System.out.println("\n🔌 Integration Patterns:");
			System.out.println("  ┌────────────────────────────────────────────────────────────┐");
			System.out.println("  │  1. Remote A2A Agent (HTTP) - ~100-500ms overhead          │");
			System.out.println("  │     • accommodation (Port 10002) - Hotel recommendations   │");
			System.out.println("  │     → Via A2AToolCallback                             │");
			System.out.println("  │                                                            │");
			System.out.println("  │  2. MCP Tools (Direct Calls) - stdio protocol              │");
			System.out.println("  │     • get-forecast - Weather forecasts                     │");
			System.out.println("  │     → Auto-configured by Spring AI MCP                     │");
			System.out.println("  │                                                            │");
			System.out.println("  │  3. Optional Local Tools - In-process                      │");
			System.out.println("  │     • Add spring-ai-agent-utils dependency                 │");
			System.out.println("  │     → File system, web search, calculator, etc.            │");
			System.out.println("  └────────────────────────────────────────────────────────────┘");

			System.out.println("\n💡 Example Request:");
			System.out.println("  curl -X POST http://localhost:8080/a2a \\");
			System.out.println("    -H 'Content-Type: application/json' \\");
			System.out.println("    -d '{");
			System.out.println("      \"jsonrpc\": \"2.0\",");
			System.out.println("      \"method\": \"agent.execute\",");
			System.out.println("      \"params\": {");
			System.out.println("        \"prompt\": \"Plan a 3-day trip to Paris in July with hotel recommendations\"");
			System.out.println("      },");
			System.out.println("      \"id\": 1");
			System.out.println("    }'");

			System.out.println("\n🔄 Execution Flow:");
			System.out.println("  1. Request arrives at Travel Planning Agent");
			System.out.println("  2. ChatClient sends to LLM with all available tools");
			System.out.println("  3. LLM autonomously decides which tools to use:");
			System.out.println("     • get-forecast(location=\"Paris\", days=3) → MCP weather tool");
			System.out.println("     • A2AAgent(subagent_type=\"accommodation\", ...) → Remote A2A agent");
			System.out.println("  4. A2AToolCallback sends HTTP request to accommodation agent");
			System.out.println("  5. Spring AI manages the tool calling loop automatically");
			System.out.println("  6. Final comprehensive travel plan synthesized from all tool results");

			System.out.println("\n✨ Features:");
			System.out.println("  • Zero-Config Tool Injection - List<ToolCallback> auto-injected");
			System.out.println("  • Builder Pattern - No class inheritance required");
			System.out.println("  • LLM-Driven Orchestration - Autonomous tool selection");
			System.out.println("  • A2AToolCallback - Simple remote agent delegation");
			System.out.println("  • MCP Integration - Standard protocol weather tools");
			System.out.println("  • Composable - Mix remote agents, MCP, and local tools");

			System.out.println("\n📚 Next Steps:");
			System.out.println("  • Start Accommodation Agent: Run on port 10002");
			System.out.println("  • Test with curl: Send travel planning request");
			System.out.println("  • Add spring-ai-agent-utils: Get local tools automatically");
			System.out.println("  • Add More Agents: Register in A2AToolConfiguration");

			System.out.println("\n" + "=".repeat(80) + "\n");
		};
	}

}
