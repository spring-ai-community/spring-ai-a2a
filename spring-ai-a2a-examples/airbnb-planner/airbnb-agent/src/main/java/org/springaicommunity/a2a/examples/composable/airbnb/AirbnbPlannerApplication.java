package org.springaicommunity.a2a.examples.composable.airbnb;

import java.util.List;
import java.util.stream.Stream;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * A2A agent for Airbnb accommodation search.
 *
 * @author Christian Tzolov
 * @since 0.1.0
 */
@SpringBootApplication
public class AirbnbPlannerApplication {

	private static final Logger logger = LoggerFactory.getLogger(AirbnbPlannerApplication.class);

	private static final String SYSTEM_INSTRUCTION = """
			You are a specialized assistant for Airbnb accommodations.
			Your primary function is to utilize the provided tools to search for Airbnb listings and answer related questions.
			You must rely exclusively on these tools for information; do not invent listings or prices.
			Ensure that your Markdown-formatted response includes all relevant tool output, with particular emphasis on providing direct links to listings
			""";

	public static void main(String[] args) {
		SpringApplication.run(AirbnbPlannerApplication.class, args);
	}

	@Bean
	CommandLineRunner logTools(ToolCallbackProvider toolCallbackProvider) {
		return args -> {
			logger.info("Available MCP tools: {}",
					Stream.of(toolCallbackProvider.getToolCallbacks())
						.map(tc -> tc.getToolDefinition().name())
						.toList());
		};
	}

	@Bean
	public AgentCard agentCard(@Value("${server.port:8080}") int port,
			@Value("${server.servlet.context-path:/}") String contextPath) {

		return AgentCard.builder()
			.name("Airbnb Agent")
			.description("Helps with searching accommodation")
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", "http://localhost:" + port + contextPath + "/")))
			.version("1.0.0")
			.capabilities(AgentCapabilities.builder().streaming(false).pushNotifications(true).build())
			.defaultInputModes(List.of("text", "text/plain"))
			.defaultOutputModes(List.of("text", "text/plain"))
			.skills(List.of(AgentSkill.builder()
				.id("airbnb_search")
				.name("Search airbnb accommodation")
				.description("Helps with accommodation search using airbnb")
				.tags(List.of("airbnb accommodation"))
				.examples(List.of("Please find a room in LA, CA, April 15, 2025, checkout date is april 18, 2 adults"))
				.build()))
			.build();
	}

	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder,
			ToolCallbackProvider toolCallbackProvider) {

		ChatClient chatClient = chatClientBuilder.clone()
			.defaultSystem(SYSTEM_INSTRUCTION)
			.defaultToolCallbacks(toolCallbackProvider)
			.build();

		return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
			String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
			return chat.prompt().user(userMessage).call().content();
		});
	}

}
