# Spring AI A2A

Spring Boot integration for building AI agent servers using the [A2A Protocol](https://a2a.anthropic.com/).

## Overview

Spring AI A2A provides **server-side** support for exposing Spring AI agents via the A2A protocol. Simply add the dependency, configure a ChatClient, and your agent is automatically exposed at `/a2a` endpoint.

### Key Features

- **A2A Protocol Server**: Expose agents via A2A protocol HTTP/JSON-RPC endpoints
- **Spring Boot Auto-Configuration**: Zero-configuration setup
- **Spring AI Integration**: Works with any Spring AI ChatClient
- **A2A SDK Based**: Built on the official A2A Java SDK

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-server</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 2. Configure ChatClient

```java
@Configuration
public class AgentConfiguration {

    @Bean
    public ChatClient weatherAgent(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
            .defaultSystem("You are a weather forecasting assistant...")
            .build();
    }
}
```

### 3. Configure Application

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

### 4. Run

```bash
mvn spring-boot:run
```

Your agent is now available at `http://localhost:8080/a2a`

## Calling Your Agent

Use the [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk) to call your agent:

```java
import io.a2a.client.Client;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentTaskRequest;
import io.a2a.spec.AgentResponse;

// Discover agent metadata
AgentCard agentCard = Client.discoverAgent("http://localhost:8080");

// Create client
Client client = Client.builder(agentCard).build();

// Call agent
AgentTaskRequest request = AgentTaskRequest.builder("What's the weather?", null).build();
AgentResponse response = client.call(request);
System.out.println(response.getText());
```

## Project Structure

```
spring-ai-a2a/
├── spring-ai-a2a-server/         # Server implementation
├── spring-ai-a2a-examples/       # Example applications
└── spring-ai-a2a-integration-tests/  # Tests
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

### Agent Metadata (Optional)

Customize agent metadata by providing an `AgentCard` bean:

```java
@Bean
public AgentCard agentCard() {
    return AgentCard.builder()
        .name("Weather Agent")
        .description("Provides weather forecasts")
        .version("1.0.0")
        .protocolVersion("0.1.0")
        .capabilities(AgentCapabilities.builder()
            .streaming(false)
            .build())
        .defaultInputModes(List.of("text"))
        .defaultOutputModes(List.of("text"))
        .skills(List.of())
        .supportedInterfaces(List.of(
            new AgentInterface("JSONRPC", "http://localhost:8080/a2a")))
        .build();
}
```

If not provided, sensible defaults are used.

## Examples

See `spring-ai-a2a-examples/` for complete working examples:

- **weather-agent** - Simple weather forecasting agent
- **accommodation-agent** - Hotel recommendations agent
- **travel-planner-agent** - Agent that uses ChatClient for travel planning

## Building

```bash
# Build all modules
mvn clean install

# Run an example
cd spring-ai-a2a-examples/airbnb-planner-multiagent/weather-agent
mvn spring-boot:run
```

## License

Apache License 2.0

## Resources

- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
