package org.springaicommunity.a2a.examples.composable.airbnb;

import java.util.List;
import java.util.stream.Stream;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.a2a.server.executor.DefaultA2AChatClientAgentExecutor;

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

		return new AgentCard.Builder().name("Airbnb Agent")
			.description("Helps with searching accommodation")
			.url("http://localhost:" + port + contextPath + "/")
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).pushNotifications(true).build())
			.defaultInputModes(List.of("text", "text/plain"))
			.defaultOutputModes(List.of("text", "text/plain"))
			.skills(List.of(new AgentSkill.Builder().id("airbnb_search")
				.name("Search airbnb accommodation")
				.description("Helps with accommodation search using airbnb")
				.tags(List.of("airbnb accommodation"))
				.examples(List.of("Please find a room in LA, CA, April 15, 2025, checkout date is april 18, 2 adults"))
				.build()))
			.protocolVersion("0.3.0")
			.build();
	}

	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder,
			ToolCallbackProvider toolCallbackProvider) {

		ChatClient chatClient = chatClientBuilder.clone()
			.defaultSystem(SYSTEM_INSTRUCTION)
			.defaultToolCallbacks(toolCallbackProvider)
			.build();

		return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, requestContext) -> {
			String userMessage = DefaultA2AChatClientAgentExecutor.extractTextFromMessage(requestContext.getMessage());
			return chat.prompt().user(userMessage).call().content();
		});
	}

}
