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
 * distributed under the License at
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

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Accommodation Agent - Remote A2A Agent Example.
 *
 * <p>This application demonstrates the <strong>Remote A2A Agent pattern</strong>,
 * running as an independent service that can be called by other agents via the A2A protocol.
 *
 * <p><strong>Architecture Pattern:</strong>
 * <pre>
 * Travel Planning Agent (Port 8080)
 *         │
 *         │ A2AToolCallback
 *         ▼
 *   HTTP POST /a2a
 *         │
 *         ▼
 * Accommodation Agent (Port 10002) ← THIS SERVICE
 *         │
 *         └─→ Specialized accommodation expertise
 * </pre>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li><strong>Independent Service:</strong> Runs on separate port (10002)</li>
 *   <li><strong>A2A Server:</strong> Exposes /a2a endpoint via Spring Boot auto-configuration</li>
 *   <li><strong>Domain Specialization:</strong> Focused on accommodation recommendations</li>
 *   <li><strong>Builder Pattern:</strong> Uses A2AAgentModel.builder() - no inheritance</li>
 * </ul>
 *
 * <p><strong>Integration with Travel Planning Agent:</strong>
 * The Travel Planning Agent calls this agent using A2AToolCallback:
 * <pre>
 * // In Travel Planning Agent's A2AToolConfiguration:
 * Map&lt;String, String&gt; agentUrls = Map.of(
 *     "accommodation", "http://localhost:10002/a2a"
 * );
 * return new A2AToolCallback(agentUrls, Duration.ofMinutes(2));
 *
 * // LLM calls it as:
 * A2AAgent(
 *   description="Find Paris hotels",
 *   prompt="Recommend 3 hotels near Eiffel Tower, budget €100-200/night",
 *   subagent_type="accommodation"
 * )
 * </pre>
 *
 * <p><strong>Execution Flow:</strong>
 * <ol>
 *   <li>Travel Planning Agent receives user request</li>
 *   <li>LLM decides to use A2AAgent tool for accommodation</li>
 *   <li>A2AToolCallback sends HTTP POST to this service</li>
 *   <li>This agent processes request with accommodation expertise</li>
 *   <li>Returns hotel recommendations via A2A protocol</li>
 *   <li>Travel Planning Agent incorporates into final response</li>
 * </ol>
 *
 * @author Ilayaperumal Gopinathan
 * @since 0.1.0
 * @see org.springaicommunity.a2a.client.tool.A2AToolCallback
 * @see AccommodationAgentConfiguration
 */
@SpringBootApplication
public class AccommodationAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccommodationAgentApplication.class, args);
	}

	/**
	 * Display startup information showing the remote A2A agent pattern.
	 *
	 * @return CommandLineRunner for startup logging
	 */
	@Bean
	public CommandLineRunner startupInfo() {
		return args -> {
			System.out.println("\n" + "=".repeat(80));
			System.out.println("🏨 Accommodation Agent Started!");
			System.out.println("=".repeat(80));

			System.out.println("\n📋 Agent Details:");
			System.out.println("  ┌─────────────────────────────────────────────────────────────┐");
			System.out.println("  │  Accommodation Agent (Port 10002)                           │");
			System.out.println("  │  • Remote A2A Agent pattern                                 │");
			System.out.println("  │  • Builder API (A2AAgentModel)                              │");
			System.out.println("  │  • Domain specialization: Hotels & Lodging                  │");
			System.out.println("  │  • URL: http://localhost:10002/a2a                          │");
			System.out.println("  └─────────────────────────────────────────────────────────────┘");

			System.out.println("\n🔌 Integration Pattern:");
			System.out.println("  This agent is called by Travel Planning Agent via A2AToolCallback:");
			System.out.println("  1. Travel Planning Agent receives user request (Port 8080)");
			System.out.println("  2. LLM decides to use A2AAgent tool for accommodation");
			System.out.println("  3. A2AToolCallback sends HTTP POST to this service");
			System.out.println("  4. This agent processes with accommodation expertise");
			System.out.println("  5. Returns recommendations via A2A protocol");
			System.out.println("  6. Travel Planning Agent synthesizes final response");

			System.out.println("\n⚙️  Configuration (in travel-planner-agent):");
			System.out.println("  // A2AToolConfiguration.java");
			System.out.println("  Map<String, String> agentUrls = Map.of(");
			System.out.println("      \"accommodation\", \"http://localhost:10002/a2a\"");
			System.out.println("  );");
			System.out.println("  return new A2AToolCallback(agentUrls, Duration.ofMinutes(2));");

			System.out.println("\n💡 Example Usage (from Travel Planning Agent):");
			System.out.println("  A2AAgent(");
			System.out.println("    description=\"Find Paris hotels\",");
			System.out.println("    prompt=\"Recommend 3 hotels in Paris near Eiffel Tower, €100-200/night\",");
			System.out.println("    subagent_type=\"accommodation\"");
			System.out.println("  )");

			System.out.println("\n🧪 Direct Testing (A2A Protocol):");
			System.out.println("  curl -X POST http://localhost:10002/a2a \\");
			System.out.println("    -H 'Content-Type: application/json' \\");
			System.out.println("    -d '{");
			System.out.println("      \"jsonrpc\": \"2.0\",");
			System.out.println("      \"method\": \"agent.execute\",");
			System.out.println("      \"params\": {");
			System.out.println("        \"prompt\": \"Recommend 3 hotels in Paris near Eiffel Tower\"");
			System.out.println("      },");
			System.out.println("      \"id\": 1");
			System.out.println("    }'");

			System.out.println("\n✨ Pattern Benefits:");
			System.out.println("  • Independent Deployment: Scale and deploy separately");
			System.out.println("  • Technology Flexibility: Can use different LLM/stack");
			System.out.println("  • Domain Expertise: Focused specialist prompting");
			System.out.println("  • Reusability: Can be called by multiple orchestrator agents");
			System.out.println("  • Fault Isolation: Failures don't crash main agent");

			System.out.println("\n🚀 Ready! Waiting for requests from Travel Planning Agent...");
			System.out.println("=" + "=".repeat(79) + "\n");
		};
	}

}
