package org.springaicommunity.a2a.examples.composable.weather;

import java.util.List;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;
import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * An agent that can help questions about weather
 *
 * https://github.com/a2aproject/a2a-samples/tree/main/samples/python/agents/airbnb_planner_multiagent/weather_agent
 *
 * @author Christian Tzolov
 */
@SpringBootApplication
public class WeatherAgentApplication {

	private static final String WEATHER_SYSTEM_INSTRUCTION = """
			You are a specialized weather forecast assistant.
			Your primary function is to utilize the provided tools to retrieve and relay weather information in response to user queries.
			You must rely exclusively on these tools for data and refrain from inventing information.
			Ensure that all responses include the detailed output from the tools used and are formatted in Markdown
			""";

	public static void main(String[] args) {
		SpringApplication.run(WeatherAgentApplication.class, args);
	}

	@Bean
	public AgentCard agentCard(@Value("${server.port:8080}") int port,
			@Value("${server.servlet.context-path:/}") String contextPath) {

		return AgentCard.builder()
			.name("Weather Agent")
			.description("Helps with weather")
			.supportedInterfaces(List.of(new AgentInterface("JSONRPC", "http://localhost:" + port + contextPath + "/")))
			.version("1.0.0")
			.capabilities(AgentCapabilities.builder().streaming(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(AgentSkill.builder()
				.id("weather_search")
				.name("Search weather")
				.description("Helps with weather in city, or states")
				.tags(List.of("weather"))
				.examples(List.of("weather in LA, CA"))
				.build()))
			.build();
	}

	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {

		ChatClient chatClient = chatClientBuilder.clone()
			.defaultSystem(WEATHER_SYSTEM_INSTRUCTION)
			.defaultTools(weatherTools)
			.build();

		return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
			String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
			return chat.prompt().user(userMessage).call().content();
		});
	}

}
