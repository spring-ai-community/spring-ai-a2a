# Spring AI A2A

Spring Boot integration for building AI agent servers using the [A2A Protocol](https://a2a.anthropic.com/).

## Overview

Spring AI A2A provides **server-side** support for exposing Spring AI agents via the A2A protocol. Simply add the dependency, implement an AgentExecutor, and your agent is automatically exposed at `/a2a` endpoint.

### Key Features

- **A2A Protocol Server**: Expose agents via A2A protocol HTTP/JSON-RPC endpoints
- **Spring Boot Auto-Configuration**: Zero-configuration server setup
- **Spring AI Integration**: Built on Spring AI's ChatClient
- **A2A SDK Based**: Uses the official A2A Java SDK (0.3.3.Final)
- **DefaultChatClientAgentExecutor**: Framework-provided executor for simple agents
- **Tool Support**: Full support for Spring AI's `@Tool` annotations

## Quick Start

### 1. Add Dependencies

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-server</artifactId>
    <version>0.3.3.Final</version>
</dependency>

<!-- Spring AI OpenAI (or your preferred model) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

### 2. Create Your Agent

#### Simple Agent with Tools

```java
@SpringBootApplication
public class WeatherAgentApplication {

    private static final String SYSTEM_INSTRUCTION = """
        You are a weather forecasting assistant.
        Use the provided tools to retrieve weather information.
        """;

    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder()
            .name("Weather Agent")
            .description("Provides weather forecasts and climate data")
            .url("http://localhost:8080/a2a")
            .version("1.0.0")
            .protocolVersion("0.3.0")
            .capabilities(new AgentCapabilities.Builder().streaming(false).build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .build();
    }

    @Bean
    public AgentExecutor agentExecutor(
            ChatClient.Builder chatClientBuilder,
            WeatherTools weatherTools) {

        ChatClient chatClient = chatClientBuilder.clone()
            .defaultSystem(SYSTEM_INSTRUCTION)
            .defaultTools(weatherTools)
            .build();

        return new DefaultChatClientAgentExecutor(chatClient);
    }
}
```

#### Define Tools

```java
@Component
public class WeatherTools {

    @Tool(description = "Get current weather for a location")
    public String getCurrentWeather(
            @ToolParam(description = "The city and state, e.g. San Francisco, CA")
            String location) {
        // Your implementation here
        return "Current weather in " + location + ": Sunny, 72°F";
    }

    @Tool(description = "Get weather forecast")
    public String getWeatherForecast(
            @ToolParam(description = "The city and state") String location,
            @ToolParam(description = "Number of days (1-7)") int days) {
        // Your implementation here
        return "Forecast for " + location + ": ...";
    }
}
```

### 3. Configure Application

```yaml
server:
  port: 8080

spring:
  ai:
    a2a:
      server:
        enabled: true
        base-path: /a2a
    openai:
      api-key: ${OPENAI_API_KEY}
```

### 4. Run

```bash
export OPENAI_API_KEY=your-key-here
mvn spring-boot:run
```

Your agent is now available at:
- **A2A Endpoint**: `http://localhost:8080/a2a`
- **Agent Card**: `http://localhost:8080/a2a/card`

## Calling Your Agent

### Using A2A Java SDK

```java
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.JdkA2AHttpClient;
import io.a2a.client.Client;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;

// 1. Discover agent metadata
A2ACardResolver resolver = new A2ACardResolver(
    new JdkA2AHttpClient(),
    "http://localhost:8080",
    "/a2a/card",
    null
);
AgentCard agentCard = resolver.getAgentCard();

// 2. Create A2A client
Client client = Client.builder(agentCard)
    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
    .build();

// 3. Send message
Message message = new Message.Builder()
    .role(Message.Role.USER)
    .parts(List.of(new TextPart("What's the weather in Paris?")))
    .build();

client.sendMessage(message);
```

### Using curl

```bash
# Get agent metadata
curl http://localhost:8080/a2a/card | jq

# Send a message
curl -X POST http://localhost:8080/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "sendMessage",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"type": "text", "text": "What is the weather in London?"}]
      }
    },
    "id": "1"
  }'
```

## Architecture

### Server-Side: Exposing Agents

Spring AI A2A makes it simple to expose your agents via A2A protocol:

```
┌─────────────────────────────────────┐
│     Your Spring Boot Application   │
│                                     │
│  ┌──────────────────────────────┐  │
│  │   AgentExecutor              │  │
│  │   (your implementation)      │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│  ┌──────────────▼───────────────┐  │
│  │  DefaultA2AServer            │  │
│  │  (auto-configured)           │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│                 ▼                   │
│         HTTP POST /a2a              │
└─────────────────────────────────────┘
```

**Key Points**:
- You provide `AgentExecutor` bean
- Framework auto-configures `DefaultA2AServer`
- A2A endpoints exposed automatically

### Client-Side: Calling Remote Agents

Use direct A2A SDK calls to communicate with remote agents:

```java
@Service
public class RemoteAgentConnections {

    @Tool(description = "Sends a task to a remote agent")
    public String sendMessage(
            @ToolParam(description = "The agent name") String agentName,
            @ToolParam(description = "The task") String task) {

        // 1. Get agent card
        AgentCard agentCard = discoverAgent(agentName);

        // 2. Create A2A client
        Client client = Client.builder(agentCard)
            .clientConfig(clientConfig)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
            .addConsumers(List.of(consumer))
            .build();

        // 3. Send message and wait for response
        client.sendMessage(message);
        return responseFuture.get(60, TimeUnit.SECONDS);
    }
}
```

## Examples

The `spring-ai-a2a-examples/` directory contains complete working examples:

### Simple Agents

- **weather-agent** - Weather forecasting with mock tools
- **accommodation-agent** - Hotel recommendations with mock search

Both use `DefaultChatClientAgentExecutor` for simplicity.

### Multi-Agent System

- **airbnb-planner-multiagent** - Travel planning orchestrator that delegates to weather and accommodation agents

This example demonstrates:
- LLM-driven agent orchestration
- Direct A2A SDK client calls
- Automatic agent discovery
- Tool-based agent delegation

See [airbnb-planner-multiagent/README.md](spring-ai-a2a-examples/airbnb-planner-multiagent/README.md) for detailed documentation.

## Project Structure

```
spring-ai-a2a/
├── spring-ai-a2a-server/              # Server implementation
│   ├── executor/                      # AgentExecutor interfaces and defaults
│   └── autoconfigure/                 # Spring Boot auto-configuration
├── spring-ai-a2a-utils/               # Optional utilities
├── spring-ai-a2a-examples/            # Example applications
│   └── airbnb-planner-multiagent/     # Multi-agent example
│       ├── weather-agent/
│       ├── accommodation-agent/
│       └── travel-planner/
└── spring-ai-a2a-integration-tests/   # Integration tests
```

## Configuration

### A2A Server Configuration

The server uses configuration from the A2A Java SDK. Default values:

```properties
# Agent execution timeout
a2a.blocking.agent.timeout.seconds=30

# Event persistence timeout
a2a.blocking.consumption.timeout.seconds=5

# Thread pool configuration
a2a.executor.core-pool-size=5
a2a.executor.max-pool-size=50
a2a.executor.keep-alive-seconds=60
```

Override by creating `src/main/resources/META-INF/a2a-defaults.properties`:

```properties
# Increase timeout for slow agents
a2a.blocking.agent.timeout.seconds=60

# Increase thread pool for high concurrency
a2a.executor.max-pool-size=100
```

### Spring Boot Configuration

```yaml
spring:
  ai:
    a2a:
      server:
        enabled: true          # Enable A2A server
        base-path: /a2a        # Base path for endpoints
      agent:
        name: "My Agent"
        description: "Agent description"
        version: "1.0.0"
        protocol-version: "0.3.0"
```

## Implementation Patterns

### Pattern 1: Simple Agent with DefaultChatClientAgentExecutor

Best for agents that use ChatClient with tools:

```java
@Bean
public AgentExecutor agentExecutor(
        ChatClient.Builder chatClientBuilder,
        MyTools tools) {

    ChatClient chatClient = chatClientBuilder.clone()
        .defaultSystem("You are a helpful assistant...")
        .defaultTools(tools)
        .build();

    return new DefaultChatClientAgentExecutor(chatClient);
}
```

**Use when**:
- Simple agents with tools
- Standard ChatClient patterns
- No custom execution logic needed

### Pattern 2: Custom AgentExecutor

For agents needing custom execution logic:

```java
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.TextPart;

@Component
public class MyCustomAgent implements AgentExecutor {

    private final ChatClient chatClient;

    public MyCustomAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(context, eventQueue);

        try {
            // Start the task
            if (context.getTask() == null) {
                updater.submit();
            }
            updater.startWork();

            // Send progress update
            updater.addMessage(
                List.of(new TextPart("Processing your request...")),
                "assistant"
            );

            // Extract message text
            String userMessage = extractTextFromMessage(context.getMessage());

            // Call ChatClient
            String response = this.chatClient.prompt()
                .user(userMessage)
                .call()
                .content();

            // Add result as artifact
            updater.addArtifact(List.of(new TextPart(response)));

            // Mark complete
            updater.complete();
        }
        catch (Exception e) {
            logger.error("Task execution failed", e);
            throw new JSONRPCError(-32603, "Agent execution failed: " + e.getMessage(), null);
        }
    }

    @Override
    public void cancel(RequestContext context, EventQueue eventQueue) {
        new TaskUpdater(context, eventQueue).cancel();
    }

    private String extractTextFromMessage(Message message) {
        if (message == null || message.getParts() == null) {
            return "";
        }
        return message.getParts().stream()
            .filter(part -> part instanceof TextPart)
            .map(part -> ((TextPart) part).getText())
            .collect(Collectors.joining("\n"));
    }
}
```

**Use when**:
- Custom task lifecycle management
- Progress updates during execution
- Complex error handling
- Custom artifact generation

### Pattern 3: Agent with Remote Agent Delegation

For orchestrators that delegate to other A2A agents:

```java
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;

@Service
public class RemoteAgentTools {

    private final Map<String, AgentCard> agentCards;

    public RemoteAgentTools(Map<String, AgentCard> agentCards) {
        this.agentCards = agentCards;
    }

    @Tool(description = "Sends a task to a remote agent")
    public String sendMessage(
            @ToolParam(description = "Agent name") String agentName,
            @ToolParam(description = "Task description") String task) {

        AgentCard agentCard = agentCards.get(agentName);

        // Create message
        Message message = new Message.Builder()
            .role(Message.Role.USER)
            .parts(List.of(new TextPart(task)))
            .build();

        // Setup response future
        CompletableFuture<String> responseFuture = new CompletableFuture<>();
        AtomicReference<String> responseText = new AtomicReference<>("");

        BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
            if (event instanceof TaskEvent taskEvent) {
                Task completedTask = taskEvent.getTask();

                // Extract text from artifacts
                if (completedTask.getArtifacts() != null) {
                    StringBuilder sb = new StringBuilder();
                    for (Artifact artifact : completedTask.getArtifacts()) {
                        if (artifact.parts() != null) {
                            for (Part<?> part : artifact.parts()) {
                                if (part instanceof TextPart textPart) {
                                    sb.append(textPart.getText());
                                }
                            }
                        }
                    }
                    responseText.set(sb.toString());
                }
                responseFuture.complete(responseText.get());
            }
        };

        // Create A2A client
        ClientConfig clientConfig = new ClientConfig.Builder()
            .setAcceptedOutputModes(List.of("text"))
            .build();

        Client client = Client.builder(agentCard)
            .clientConfig(clientConfig)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
            .addConsumers(List.of(consumer))
            .build();

        // Send message and wait for response
        client.sendMessage(message);

        try {
            return responseFuture.get(60, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}

@Bean
public AgentExecutor orchestrator(
        ChatClient.Builder chatClientBuilder,
        RemoteAgentTools tools) {

    ChatClient chatClient = chatClientBuilder.clone()
        .defaultSystem("You are an orchestrator that delegates to specialized agents...")
        .defaultTools(tools)
        .build();

    return new DefaultChatClientAgentExecutor(chatClient);
}
```

**Use when**:
- Building multi-agent systems
- Delegating to specialized remote A2A agents
- Composing agent capabilities
- LLM-driven agent orchestration

See the [airbnb-planner-multiagent example](spring-ai-a2a-examples/airbnb-planner-multiagent/) for a complete implementation.

## Building

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run an example
cd spring-ai-a2a-examples/airbnb-planner-multiagent/weather-agent
mvn spring-boot:run
```

## Testing

```bash
# Run all tests
mvn test

# Run integration tests only
mvn test -pl spring-ai-a2a-integration-tests
```

## Requirements

- Java 17+
- Maven 3.8+
- Spring Boot 4.0+
- Spring AI 2.0.0-M1+
- A2A Java SDK 0.3.3.Final

## Troubleshooting

### Agent Not Responding

**Issue**: Requests hang or timeout

**Solution**:
- Check agent executor is registered as Spring bean
- Verify OpenAI API key is set
- Increase timeout in `a2a-defaults.properties`

### 404 on /a2a Endpoint

**Issue**: Endpoint not found

**Solution**:
- Verify `spring.ai.a2a.server.enabled=true`
- Check `spring.ai.a2a.server.base-path` configuration
- Ensure spring-ai-a2a-server dependency is present

### Tools Not Working

**Issue**: Agent doesn't call tools

**Solution**:
- Verify tools are Spring beans (`@Component` or `@Bean`)
- Check tool descriptions are clear
- Ensure tools are registered with ChatClient via `.defaultTools()`

## License

Apache License 2.0

## Resources

- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)

## Contributing

Contributions welcome! Please open an issue or pull request.

## Support

For questions and issues:
- GitHub Issues: [spring-ai-a2a/issues](https://github.com/spring-ai-community/spring-ai-a2a/issues)
- Spring AI Community: [spring-ai-community](https://github.com/spring-ai-community)
