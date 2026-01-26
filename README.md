# Spring AI A2A

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity/spring-ai-a2a.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.springaicommunity/spring-ai-a2a)
[![Java Version](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

Spring Boot integration for building AI agent servers using the [A2A Protocol](https://a2a.anthropic.com/).

## Overview

Spring AI A2A provides **server-side** support for exposing Spring AI agents via the A2A protocol. Add the dependency, provide `ChatClient`, `AgentCard`, and `AgentExecutor` beans, and your agent is automatically exposed.

**Key Features**: A2A Protocol Server • Spring AI ChatClient Integration • Full `@Tool` Support • Auto-Configuration

## Quick Start

### 1. Add Dependencies

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-server-autoconfigure</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

### 2. Create Your Agent

```java
@SpringBootApplication
public class WeatherAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(WeatherAgentApplication.class, args);
    }

    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port) {
        return new AgentCard.Builder()
            .name("Weather Agent")
            .description("Provides weather forecasts")
            .url("http://localhost:" + port + "/a2a/")
            .version("1.0.0")
            .capabilities(new AgentCapabilities.Builder().streaming(false).build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of(new AgentSkill.Builder()
                .id("weather_search").name("Weather Search")
                .description("Provides current weather and forecasts")
                .tags(List.of("weather")).build()))
            .protocolVersion("0.3.0")
            .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient.Builder chatClientBuilder, WeatherTools tools) {
        ChatClient chatClient = chatClientBuilder.clone()
            .defaultSystem("You are a weather assistant. Use tools to get weather data.")
            .defaultTools(tools)
            .build();

        return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, ctx) -> {
            String userMessage = DefaultA2AChatClientAgentExecutor.extractTextFromMessage(ctx.getMessage());
            return chat.prompt().user(userMessage).call().content();
        });
    }
}
```

### 3. Define Tools

```java
@Service
public class WeatherTools {

    @Tool(description = "Get current weather for a location")
    public String getCurrentWeather(
            @ToolParam(description = "City and state, e.g. San Francisco, CA") String location) {
        return "Current weather in " + location + ": Sunny, 72°F";
    }
}
```

### 4. Configure & Run

```yaml
server:
  port: 8080
  servlet:
    context-path: /a2a

spring:
  ai:
    a2a.server.enabled: true
    openai.api-key: ${OPENAI_API_KEY}
```

```bash
export OPENAI_API_KEY=your-key && mvn spring-boot:run
```

Your agent is available at `http://localhost:8080/a2a` (card at `/a2a/card`).

## Calling Your Agent

**Using curl:**
```bash
curl -X POST http://localhost:8080/a2a -H 'Content-Type: application/json' -d '{
  "jsonrpc": "2.0", "method": "sendMessage", "id": "1",
  "params": {"message": {"role": "user", "parts": [{"type": "text", "text": "Weather in London?"}]}}
}'
```

**Using A2A SDK:**
```java
A2ACardResolver resolver = new A2ACardResolver(new JdkA2AHttpClient(), "http://localhost:8080", "/a2a/card", null);
Client client = Client.builder(resolver.getAgentCard())
    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig()).build();
client.sendMessage(new Message.Builder().role(Message.Role.USER)
    .parts(List.of(new TextPart("Weather in Paris?"))).build());
```

## Architecture

```
Your Spring Boot App
├── ChatClient, AgentCard, AgentExecutor (your beans)
├── DefaultA2AChatClientAgentExecutor (bridges A2A ↔ ChatClient)
└── A2A Controllers (auto-configured)
    ├── POST /      → MessageController (sendMessage)
    ├── GET  /card  → AgentCardController (discovery)
    └── GET  /tasks/{id} → TaskController (status)
```

**Request Flow:**
1. `MessageController` receives JSON-RPC request
2. A2A SDK `RequestHandler` creates task
3. `DefaultA2AChatClientAgentExecutor` invokes your `ChatClientExecutorHandler`
4. `ChatClient` calls LLM and executes tools
5. Response wrapped as task artifact → returned to client

**You provide:** `AgentExecutor`, `AgentCard`, `ChatClient` beans
**Framework handles:** Endpoints, task lifecycle, JSON-RPC, error handling

## Implementation Patterns

### Pattern 1: Simple Agent (Recommended)

Use `DefaultA2AChatClientAgentExecutor` with a lambda:

```java
@Bean
public AgentExecutor agentExecutor(ChatClient.Builder builder, MyTools tools) {
    ChatClient chatClient = builder.clone().defaultSystem("...").defaultTools(tools).build();
    return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, ctx) -> {
        String msg = DefaultA2AChatClientAgentExecutor.extractTextFromMessage(ctx.getMessage());
        return chat.prompt().user(msg).call().content();
    });
}
```

### Pattern 2: Custom AgentExecutor

For custom task lifecycle or progress updates:

```java
@Component
public class MyAgent implements AgentExecutor {
    @Override
    public void execute(RequestContext ctx, EventQueue queue) throws JSONRPCError {
        TaskUpdater updater = new TaskUpdater(ctx, queue);
        if (ctx.getTask() == null) updater.submit();
        updater.startWork();
        updater.addMessage(List.of(new TextPart("Processing...")), "assistant");
        String response = chatClient.prompt().user(extractText(ctx.getMessage())).call().content();
        updater.addArtifact(List.of(new TextPart(response)), null, null, null);
        updater.complete();
    }
}
```

### Pattern 3: Multi-Agent Orchestration

Create tools that call remote A2A agents:

```java
@Service
public class RemoteAgentTools {
    private final Map<String, AgentCard> agents;

    @Tool(description = "Delegate task to remote agent")
    public String sendMessage(@ToolParam(description = "Agent name") String name,
                              @ToolParam(description = "Task") String task) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Client client = Client.builder(agents.get(name))
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
            .addConsumers(List.of((event, card) -> {
                if (event instanceof TaskEvent te && te.getTask().getArtifacts() != null)
                    future.complete(extractText(te.getTask().getArtifacts()));
            })).build();
        client.sendMessage(new Message.Builder().role(Message.Role.USER)
            .parts(List.of(new TextPart(task))).build());
        return future.get(60, TimeUnit.SECONDS);
    }
}
```

See [airbnb-planner example](spring-ai-a2a-examples/airbnb-planner/) for complete multi-agent implementation.

## Project Structure

```
spring-ai-a2a/
├── spring-ai-a2a-server/              # Core: executor/, controller/
├── spring-ai-a2a-server-autoconfigure/ # Auto-configuration
└── spring-ai-a2a-examples/airbnb-planner/
    ├── weather-agent/   (Port 10001)
    ├── airbnb-agent/    (Port 10002)
    └── host-agent/      (Port 10000) - orchestrator
```

## Configuration

**Spring Boot** (`application.properties` or `application.yml`):
```yaml
spring:
  ai:
    a2a:
      server:
        enabled: true

server:
  servlet:
    context-path: /a2a
```

**A2A SDK properties** can be configured directly in Spring's Environment (application.properties, environment variables, etc.):
```properties
# Blocking call timeouts
a2a.blocking.agent.timeout.seconds=30
a2a.blocking.consumption.timeout.seconds=5

# Thread pool configuration
a2a.executor.core-pool-size=5
a2a.executor.max-pool-size=50
a2a.executor.keep-alive-seconds=60
```

The `SpringA2AConfigProvider` first checks Spring's `Environment` for property values and falls back to the SDK's `DefaultValuesConfigProvider` for any missing keys. This follows the [custom config provider pattern](https://github.com/a2aproject/a2a-java/blob/main/integrations/microprofile-config/README.md#custom-config-providers) recommended by the A2A Java SDK.

## Building & Testing

```bash
mvn clean install           # Build all
mvn test                    # Run tests
cd spring-ai-a2a-examples/airbnb-planner/weather-agent && mvn spring-boot:run
```

## Requirements

Java 17+ • Maven 3.8+ • Spring Boot 4.0+ • Spring AI 2.0.0-M2+ • A2A SDK 0.3.3.Final

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Requests timeout | Check `AgentExecutor` bean, API key, increase timeout |
| 404 on endpoint | Verify `spring.ai.a2a.server.enabled=true` and context-path |
| Tools not called | Ensure tools are `@Service`/`@Component` and registered via `.defaultTools()` |

## Resources

- [A2A Protocol](https://a2a.anthropic.com/) • [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk)
- [Spring AI Docs](https://docs.spring.io/spring-ai/reference/) • [Spring Boot](https://docs.spring.io/spring-boot/reference/)

## License

Apache License 2.0

## Support

[GitHub Issues](https://github.com/spring-ai-community/spring-ai-a2a/issues) • [Spring AI Community](https://github.com/spring-ai-community)
