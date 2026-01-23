package org.springaicommunity.a2a.examples.composable.weather;

import java.util.List;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;
import org.springaicommunity.a2a.server.executor.DefaultA2AChatClientAgentExecutor;

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

		return new AgentCard.Builder().name("Weather Agent")
			.description("Helps with weather")
			.url("http://localhost:" + port + contextPath + "/")
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(new AgentSkill.Builder().id("weather_search")
				.name("Search weather")
				.description("Helps with weather in city, or states")
				.tags(List.of("weather"))
				.examples(List.of("weather in LA, CA"))
				.build()))
			.protocolVersion("0.3.0")
			.build();
	}

	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, WeatherTools weatherTools) {

		ChatClient chatClient = chatClientBuilder.clone()
			.defaultSystem(WEATHER_SYSTEM_INSTRUCTION)
			.defaultTools(weatherTools)
			.build();

		return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, requestContext) -> {
			String userMessage = DefaultA2AChatClientAgentExecutor.extractTextFromMessage(requestContext.getMessage());
			return chat.prompt().user(userMessage).call().content();
		});
	}

}
