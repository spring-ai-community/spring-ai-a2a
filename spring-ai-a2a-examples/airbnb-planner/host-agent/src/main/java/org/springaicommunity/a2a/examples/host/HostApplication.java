package org.springaicommunity.a2a.examples.host;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class HostApplication {

	private static final Logger logger = LoggerFactory.getLogger(HostApplication.class);

	private static final String ROUTING_SYSTEM_PROMPT = """
			**Role:** You are an expert Routing Delegator. Your primary function is to accurately delegate user inquiries regarding weather or accommodations to the appropriate specialized remote agents.

			**Core Directives:**

			* **Task Delegation:** Utilize the `sendMessage` function to assign actionable tasks to remote agents.
			* **Contextual Awareness for Remote Agents:** If a remote agent repeatedly requests user confirmation, assume it lacks access to the full conversation history. In such cases, enrich the task description with all necessary contextual information relevant to that specific agent.
			* **Autonomous Agent Engagement:** Never seek user permission before engaging with remote agents. If multiple agents are required to fulfill a request, connect with them directly without requesting user preference or confirmation.
			* **Transparent Communication:** Always present the complete and detailed response from the remote agent to the user.
			* **User Confirmation Relay:** If a remote agent asks for confirmation, and the user has not already provided it, relay this confirmation request to the user.
			* **Focused Information Sharing:** Provide remote agents with only relevant contextual information. Avoid extraneous details.
			* **No Redundant Confirmations:** Do not ask remote agents for confirmation of information or actions.
			* **Tool Reliance:** Strictly rely on available tools to address user requests. Do not generate responses based on assumptions. If information is insufficient, request clarification from the user.
			* **Prioritize Recent Interaction:** Focus primarily on the most recent parts of the conversation when processing requests.

			**Agent Roster:**

			Available Agents:
			%s
			""";

	public static void main(String[] args) {
		SpringApplication.run(HostApplication.class, args);
	}

	@Bean
	public ChatClient routingChatClient(ChatClient.Builder chatClientBuilder,
			RemoteAgentConnections remoteAgentConnections) {

		String systemPrompt = String.format(ROUTING_SYSTEM_PROMPT, remoteAgentConnections.getAgentDescriptions());

		logger.info("Initializing routing ChatClient with agents: {}", remoteAgentConnections.getAgentNames());

		return chatClientBuilder.defaultSystem(systemPrompt).defaultTools(remoteAgentConnections).build();
	}

}
