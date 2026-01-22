# Spring AI ChatClient Utils

Protocol-agnostic ChatClient execution interface for Spring AI applications.

## Overview

This module provides the `ChatClientExecutor` interface, decoupling Spring AI business logic from protocol-specific layers (like A2A, MCP, or custom protocols).

## Key Interface

### ChatClientExecutor

```java
@FunctionalInterface
public interface ChatClientExecutor {
    String execute(ChatClient chatClient, String userMessage, Map<String, Object> context);

    default Flux<String> executeStream(ChatClient chatClient, String userMessage, Map<String, Object> context) {
        return Flux.just(execute(chatClient, userMessage, context));
    }
}
```

**Parameters**:
- `chatClient` - Spring AI ChatClient instance
- `userMessage` - Extracted user message text (protocol-agnostic)
- `context` - Execution context map (protocol-agnostic)

## Usage Pattern

### 1. Define Custom Execution Logic

```java
@Bean
public ChatClientExecutor chatClientExecutor() {
    return (chatClient, userMessage, context) -> {
        // Your custom logic here
        return chatClient.prompt()
            .user(userMessage)
            .toolContext(context)
            .call()
            .content();
    };
}
```

### 2. Protocol Layer Extracts Message and Delegates

```java
// A2A protocol layer
public class DefaultA2AChatClientAgentExecutor implements AgentExecutor {

    private final ChatClient chatClient;
    private final ChatClientExecutor executor;

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) {
        // Extract message from A2A protocol
        String userMessage = A2AContext.getUserMessage(context);
        Map<String, Object> executionContext = buildContext(context);

        // Delegate to protocol-agnostic executor
        String response = executor.execute(chatClient, userMessage, executionContext);

        // Return via A2A protocol
        updater.addArtifact(List.of(new TextPart(response)));
    }
}
```

## Benefits

1. **Protocol Independence** - Business logic doesn't depend on A2A, MCP, or any specific protocol
2. **Testability** - Easy to test without protocol overhead
3. **Reusability** - Same executor works with different protocol adapters
4. **Flexibility** - Customize execution without touching protocol layer

## Integration with A2A

When using `spring-ai-a2a-server`, the auto-configuration:

1. Detects your `ChatClientExecutor` bean (or provides default)
2. Creates `DefaultA2AChatClientAgentExecutor` using your executor
3. Registers it as A2A `AgentExecutor`

You only need to provide:
- `AgentCard` bean (agent metadata)
- `ChatClient` bean (with tools and system prompt)
- `ChatClientExecutor` bean (optional - for custom execution logic)

## Example: Weather Agent

```java
@SpringBootApplication
public class WeatherAgentApplication {

    @Bean
    public AgentCard agentCard() {
        return new AgentCard(
            "Weather Agent",
            "Helps with weather forecasts and climate data",
            "http://localhost:10001/a2a",
            /* ... */
        );
    }

    @Bean
    public ChatClient weatherChatClient(
            ChatClient.Builder chatClientBuilder,
            WeatherTools weatherTools) {

        return chatClientBuilder.clone()
            .defaultSystem("You are a weather forecast assistant.")
            .defaultTools(weatherTools)
            .build();
    }

    @Bean
    public ChatClientExecutor chatClientExecutor() {
        return (chatClient, userMessage, context) ->
            chatClient.prompt()
                .user(userMessage)
                .toolContext(context)
                .call()
                .content();
    }
}
```

## Streaming Support

For streaming responses, implement the `executeStream()` method:

```java
@Bean
public ChatClientExecutor chatClientExecutor() {
    return new ChatClientExecutor() {
        @Override
        public String execute(ChatClient chatClient, String userMessage, Map<String, Object> context) {
            return chatClient.prompt()
                .user(userMessage)
                .toolContext(context)
                .call()
                .content();
        }

        @Override
        public Flux<String> executeStream(ChatClient chatClient, String userMessage, Map<String, Object> context) {
            return chatClient.prompt()
                .user(userMessage)
                .toolContext(context)
                .stream()
                .content();
        }
    };
}
```

## License

Apache License 2.0
