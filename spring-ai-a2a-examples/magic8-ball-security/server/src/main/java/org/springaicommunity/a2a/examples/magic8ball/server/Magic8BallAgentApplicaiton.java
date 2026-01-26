package org.springaicommunity.a2a.examples.magic8ball.server;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.ClientCredentialsOAuthFlow;
import io.a2a.spec.OAuth2SecurityScheme;
import io.a2a.spec.OAuthFlows;
import io.a2a.spec.TransportProtocol;
import org.springaicommunity.a2a.server.executor.DefaultA2AChatClientAgentExecutor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * An agent that can help answer yes/no questions using a Magic 8 Ball
 *
 * https://github.com/a2aproject/a2a-samples/blob/main/samples/java/agents/magic_8_ball_security/server
 *
 * @author Christian Tzolov
 */
@SpringBootApplication
public class Magic8BallAgentApplicaiton {

	private static final String SYSTEM_INSTRUCTIONS = """
			You shake a Magic 8 Ball to answer questions.
			The only thing you do is shake the Magic 8 Ball to answer
			the user's question and then discuss the response.
			When you are asked to answer a question, you must call the
			shakeMagic8Ball tool with the user's question.
			You should never rely on the previous history for Magic 8 Ball
			responses. Call the shakeMagic8Ball tool for each question.
			You should never shake the Magic 8 Ball on your own.
			You must always call the tool.
			When you are asked a question, you should always make the following
			function call:
			1. You should first call the shakeMagic8Ball tool to get the response.
			Wait for the function response.
			2. After you get the function response, relay the response to the user.
			You should not rely on the previous history for Magic 8 Ball responses.
			""";

	public static void main(String[] args) {
		SpringApplication.run(Magic8BallAgentApplicaiton.class, args);
	}

	@Bean
	public AgentCard agentCard(@Value("${server.port:8080}") int httpPort,
			@Value("${keycloak.devservices.port:8080}") int keycloakPort) {

		ClientCredentialsOAuthFlow clientCredentialsOAuthFlow = new ClientCredentialsOAuthFlow(null,
				Map.of("openid", "openid", "profile", "profile"),
				"http://localhost:" + keycloakPort + "/realms/quarkus/protocol/openid-connect/token");

		OAuth2SecurityScheme securityScheme = new OAuth2SecurityScheme.Builder()
			.flows(new OAuthFlows.Builder().clientCredentials(clientCredentialsOAuthFlow).build())
			.build();

		return new AgentCard.Builder().name("Magic 8 Ball Agent")
			.description(
					"A mystical fortune-telling agent that answers your yes/no questions by asking the all-knowing Magic 8 Ball oracle.")
			.preferredTransport(TransportProtocol.JSONRPC.asString())
			.url("http://localhost:" + httpPort)
			.version("1.0.0")
			.documentationUrl("http://example.com/docs")
			.capabilities(new AgentCapabilities.Builder().streaming(true)
				.pushNotifications(false)
				.stateTransitionHistory(false)
				.build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.security(List.of(Map.of(OAuth2SecurityScheme.OAUTH2, List.of("profile"))))
			.securitySchemes(Map.of(OAuth2SecurityScheme.OAUTH2, securityScheme))
			.skills(List.of(new AgentSkill.Builder().id("magic_8_ball")
				.name("Magic 8 Ball Fortune Teller")
				.description("Uses a Magic 8 Ball to answer" + " yes/no questions")
				.tags(List.of("fortune", "magic-8-ball", "oracle"))
				.examples(
						List.of("Should I deploy this code on Friday?", "Will my tests pass?", "Is this a good idea?"))
				.build()))
			.protocolVersion("0.3.0")
			.additionalInterfaces(
					List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), "http://localhost:" + httpPort),
							new AgentInterface(TransportProtocol.HTTP_JSON.asString(), "http://localhost:" + httpPort),
							new AgentInterface(TransportProtocol.GRPC.asString(), "localhost:" + httpPort)))
			.build();
	}

	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, Magic8BallTools magic8BallTools) {

		ChatClient chatClient = chatClientBuilder.clone()
			.defaultSystem(SYSTEM_INSTRUCTIONS)
			.defaultTools(magic8BallTools)
			.build();

		return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, requestContext) -> {
			String question = DefaultA2AChatClientAgentExecutor.extractTextFromMessage(requestContext.getMessage());

			// Generate a unique memory ID for this request for fresh chat memory
			final String memoryId = UUID.randomUUID().toString();
			System.out.println("=== EXECUTOR === Using memory ID: " + memoryId + " for question: " + question);

			return chat.prompt()
				.user(question)
				.toolContext(Map.of(ChatMemory.CONVERSATION_ID, memoryId))
				.call()
				.content();
		});
	}

}
