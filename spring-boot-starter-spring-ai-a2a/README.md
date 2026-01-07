# Spring Boot Starter for Spring AI A2A

This Spring Boot Starter provides auto-configuration for the Agent-to-Agent (A2A) Protocol in Spring AI applications.

## Features

- Automatic configuration of A2A Agent Server
- Property-based agent card configuration
- Auto-discovery of `AgentExecutor` beans
- Exposes A2A protocol endpoints automatically

## Usage

### 1. Add the Starter Dependency

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-boot-starter-spring-ai-a2a</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure Agent Properties

Add the following configuration to your `application.yml`:

```yaml
spring:
  ai:
    a2a:
      server:
        enabled: true
        base-path: /a2a  # Optional, defaults to /a2a
      agent:
        name: My Agent
        description: A sample agent that does X, Y, and Z
        version: 1.0.0  # Optional, defaults to 1.0.0
        protocol-version: 0.1.0  # Optional, defaults to 0.1.0
        capabilities:
          streaming: true  # Optional, defaults to true
          push-notifications: false  # Optional, defaults to false
          state-transition-history: false  # Optional, defaults to false
        default-input-modes:  # Optional, defaults to ["text"]
          - text
        default-output-modes:  # Optional, defaults to ["text"]
          - text
```

Or using `application.properties`:

```properties
spring.ai.a2a.server.enabled=true
spring.ai.a2a.server.base-path=/a2a
spring.ai.a2a.agent.name=My Agent
spring.ai.a2a.agent.description=A sample agent that does X, Y, and Z
spring.ai.a2a.agent.version=1.0.0
spring.ai.a2a.agent.protocol-version=0.1.0
spring.ai.a2a.agent.capabilities.streaming=true
spring.ai.a2a.agent.capabilities.push-notifications=false
spring.ai.a2a.agent.capabilities.state-transition-history=false
```

### 3. Implement a SpringAIAgentExecutor

Create a class that extends `DefaultSpringAIAgentExecutor` (which implements `SpringAIAgentExecutor`):

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import org.springaicommunity.a2a.core.A2ARequest;
import org.springaicommunity.a2a.core.A2AResponse;
import org.springaicommunity.a2a.server.agentexecution.DefaultSpringAIAgentExecutor;

@Component
public class MyAgentExecutor extends DefaultSpringAIAgentExecutor {

    private final ChatClient chatClient;

    public MyAgentExecutor(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public A2AResponse executeSynchronous(A2ARequest request) {
        // Extract user message
        String userMessage = extractTextFromMessage(request.getMessage());

        // Call the LLM
        String response = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();

        // Return A2A response
        return createTextResponse(response);
    }
}
```

### 4. Run Your Application

That's it! The starter will automatically:

1. Create an `AgentCard` bean based on your configuration
2. Create an `A2AAgentServer` bean using your `SpringAIAgentExecutor`
3. Expose the following A2A endpoints:
   - `GET /.well-known/agent-card.json` - Agent discovery (RFC 8615)
   - `GET /a2a` - Agent card
   - `POST /a2a` - JSON-RPC request handling
   - `GET /a2a/stream` - Streaming endpoint
   - `GET /a2a/tasks/{taskId}/subscribe` - Task subscription

## Example Application

Here's a complete minimal example:

```java
@SpringBootApplication
public class MyAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyAgentApplication.class, args);
    }

    @Bean
    public SpringAIAgentExecutor agentExecutor(ChatClient chatClient) {
        return new MyAgentExecutor(chatClient);
    }
}
```

With `application.yml`:

```yaml
spring:
  ai:
    a2a:
      agent:
        name: Weather Agent
        description: Provides weather information

server:
  port: 8080
```

## Configuration Options

| Property | Description | Default |
|----------|-------------|---------|
| `spring.ai.a2a.server.enabled` | Enable/disable A2A server | `true` |
| `spring.ai.a2a.server.base-path` | Base path for A2A endpoints | `/a2a` |
| `spring.ai.a2a.agent.name` | Agent name (required) | - |
| `spring.ai.a2a.agent.description` | Agent description (required) | - |
| `spring.ai.a2a.agent.version` | Agent version | `1.0.0` |
| `spring.ai.a2a.agent.protocol-version` | A2A protocol version | `0.1.0` |
| `spring.ai.a2a.agent.capabilities.streaming` | Enable streaming support | `true` |
| `spring.ai.a2a.agent.capabilities.push-notifications` | Enable push notifications | `false` |
| `spring.ai.a2a.agent.capabilities.state-transition-history` | Enable state transition history | `false` |
| `spring.ai.a2a.agent.default-input-modes` | Default input modes | `["text"]` |
| `spring.ai.a2a.agent.default-output-modes` | Default output modes | `["text"]` |

## Advanced Configuration

If you need more control over the agent configuration, you can provide your own `AgentCard` bean:

```java
@Bean
public AgentCard agentCard(@Value("${server.port:8080}") int serverPort) {
    return AgentCard.builder()
        .name("My Custom Agent")
        .description("Custom agent with specific configuration")
        .version("2.0.0")
        .protocolVersion("0.1.0")
        .capabilities(AgentCapabilities.builder()
            .streaming(true)
            .pushNotifications(false)
            .stateTransitionHistory(false)
            .build())
        .defaultInputModes(List.of("text"))
        .defaultOutputModes(List.of("text"))
        .supportedInterfaces(List.of(
            new AgentInterface("JSONRPC", "http://localhost:" + serverPort + "/a2a")
        ))
        .skills(List.of(
            AgentSkill.builder()
                .id("custom-skill")
                .name("Custom Skill")
                .description("A custom skill description")
                .tags(List.of("custom", "skill"))
                .build()
        ))
        .build();
}
```

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
