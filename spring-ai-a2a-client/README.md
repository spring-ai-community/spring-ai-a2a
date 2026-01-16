# Spring AI A2A Client

This module provides client-side support for the Agent-to-Agent (A2A) protocol, enabling Java applications to communicate with remote A2A agents.

## Overview

The A2A Client module offers **two distinct patterns** for integrating with remote A2A agents, depending on your use case:

1. **Spring AI Integration** - Use `A2AToolCallback` to register A2A agents as tools in Spring AI's ChatClient
2. **Direct SDK Usage** - Use the A2A Java SDK directly for standalone client applications

Both patterns use the **A2A Java SDK** under the hood with **zero wrapper abstractions**, providing direct access to the SDK's event-driven API.

## Usage Patterns Comparison

### Pattern 1: Spring AI Integration with A2AToolCallback

**Best for:** Integrating A2A agents with Spring AI's ChatClient so the LLM can delegate to remote agents as tools.

**Key Features:**
- A2A agents appear as tool definitions to the LLM
- Automatic tool registration via Spring Boot auto-configuration
- LLM-driven orchestration (LLM decides when to call which agent)
- Works alongside MCP tools and local tools uniformly
- Task-based or message-based execution modes

**Example:**

```java
import org.springaicommunity.a2a.client.tool.A2AToolCallback;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import io.a2a.spec.AgentCard;

@Configuration
public class MyAgentConfiguration {

    @Bean
    public ToolCallback weatherAgentTool(
            @Value("${weather.agent.url}") String weatherAgentUrl) {

        AgentCard weatherAgent = AgentCard.builder()
            .name("Weather Agent")
            .description("Provides weather information and forecasts")
            .version("1.0.0")
            .protocolVersion("0.1.0")
            .supportedInterfaces(List.of(
                new AgentInterface("JSONRPC", weatherAgentUrl)
            ))
            .build();

        // Simple message-based for quick queries
        return new A2AToolCallback(weatherAgent, Duration.ofSeconds(30));
    }

    @Bean
    public ChatClient chatClient(
            ChatModel chatModel,
            List<ToolCallback> tools) {  // Auto-injected!

        return ChatClient.builder(chatModel)
            .defaultToolCallbacks(tools)  // All tools registered
            .build();
    }
}
```

**Usage in application:**

```java
String response = chatClient.prompt()
    .user("What's the weather in Paris?")
    .call()
    .content();

// LLM autonomously decides to call weatherAgentTool
// Response includes weather information from remote agent
```

**When to use:**
- ✅ Building Spring AI applications with ChatClient
- ✅ Want LLM to orchestrate multiple tools/agents
- ✅ Need uniform tool registration (A2A + MCP + local tools)
- ✅ Building conversational AI assistants

### Pattern 2: Direct SDK Usage

**Best for:** Standalone client applications that need direct control over A2A communication without Spring AI integration.

**Key Features:**
- No Spring AI dependency
- Full control over SDK lifecycle
- Event-driven API with BiConsumer callbacks
- Synchronous or streaming execution modes
- Minimal abstraction layer

**Example (Synchronous):**

```java
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class DirectClientExample {

    public static void main(String[] args) throws Exception {
        String agentUrl = "http://localhost:8080/a2a";
        Duration timeout = Duration.ofSeconds(30);

        // 1. Build AgentCard for remote agent
        AgentCard agentCard = AgentCard.builder()
            .name("Weather Agent")
            .description("Weather forecast agent")
            .version("1.0.0")
            .protocolVersion("0.1.0")
            .supportedInterfaces(List.of(
                new io.a2a.spec.AgentInterface("JSONRPC", agentUrl)
            ))
            .build();

        // 2. Create client configuration
        io.a2a.client.config.ClientConfig clientConfig =
            new io.a2a.client.config.ClientConfig.Builder()
                .setStreaming(false)
                .setAcceptedOutputModes(List.of("text"))
                .build();

        // 3. Create message request
        Message request = Message.builder()
            .role(Message.Role.USER)
            .parts(List.of(new TextPart("What's the weather in Paris?")))
            .build();

        // 4. Setup synchronous response handling
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Message> responseRef = new AtomicReference<>();

        BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
            if (event instanceof MessageEvent messageEvent) {
                responseRef.set(messageEvent.getMessage());
                latch.countDown();
            }
        };

        // 5. Build client with consumer registered
        Client client = Client.builder(agentCard)
            .clientConfig(clientConfig)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
            .addConsumers(List.of(consumer))
            .build();

        // 6. Send message and wait for response
        client.sendMessage(request);
        latch.await(timeout.toSeconds(), TimeUnit.SECONDS);

        // 7. Extract response
        Message response = responseRef.get();
        String text = ((TextPart) response.parts().get(0)).text();
        System.out.println("Weather: " + text);
    }
}
```

**When to use:**
- ✅ Building standalone Java applications
- ✅ Need full control over SDK configuration
- ✅ Don't need LLM orchestration
- ✅ Implementing custom client logic

## Decision Guide

### Use A2AToolCallback when:

| Requirement | A2AToolCallback |
|-------------|-----------------|
| Spring AI ChatClient integration | ✅ |
| LLM-driven tool selection | ✅ |
| Uniform tool registration (A2A + MCP + local) | ✅ |
| Automatic Spring Boot configuration | ✅ |
| Conversational AI applications | ✅ |

### Use Direct SDK when:

| Requirement | Direct SDK |
|-------------|------------|
| Standalone Java application | ✅ |
| No Spring AI dependency needed | ✅ |
| Custom client logic | ✅ |
| Full control over SDK lifecycle | ✅ |
| Low-level event handling | ✅ |

## Module Contents

### Core Classes

- **`A2AToolCallback`** - Wraps A2A agents as Spring AI ToolCallbacks
  - Location: `org.springaicommunity.a2a.client.tool.A2AToolCallback`
  - Purpose: Spring AI integration
  - Dependencies: Spring AI, A2A Java SDK

### Examples

- **`DirectSdkUsageExample`** - Complete examples of direct SDK usage
  - Location: `src/test/java/org/springaicommunity/a2a/client/examples/DirectSdkUsageExample.java`
  - Shows: Synchronous and streaming patterns
  - Demonstrates: Event handling, error handling, progress tracking

## Configuration

### Spring AI Integration Configuration

```yaml
# application.yml

# Remote agent URLs (for A2AToolCallback beans)
weather:
  agent:
    url: http://localhost:10001/a2a

accommodation:
  agent:
    url: http://localhost:10002/a2a
```

### Direct SDK Configuration

No configuration required - create clients programmatically using the A2A Java SDK:

```java
// Build AgentCard
AgentCard agentCard = AgentCard.builder()
    .name("My Agent")
    .version("1.0.0")
    .protocolVersion("0.1.0")
    .supportedInterfaces(List.of(
        new AgentInterface("JSONRPC", "http://localhost:8080/a2a")
    ))
    .build();

// Build ClientConfig
ClientConfig clientConfig = new ClientConfig.Builder()
    .setStreaming(false)
    .setAcceptedOutputModes(List.of("text"))
    .build();

// Create Client
Client client = Client.builder(agentCard)
    .clientConfig(clientConfig)
    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
    .build();
```

## Event-Driven API Patterns

The A2A Java SDK uses an event-driven callback pattern:

### Synchronous Pattern (Blocking)

```java
CountDownLatch latch = new CountDownLatch(1);
AtomicReference<Message> responseRef = new AtomicReference<>();

BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
    if (event instanceof MessageEvent messageEvent) {
        responseRef.set(messageEvent.getMessage());
        latch.countDown();
    }
};

// Register consumer at build time
Client client = Client.builder(agentCard)
    .addConsumers(List.of(consumer))
    .build();

client.sendMessage(request);
latch.await(timeout.toSeconds(), TimeUnit.SECONDS);
```

### Streaming Pattern (Progressive)

```java
List<String> progressUpdates = new ArrayList<>();
CountDownLatch latch = new CountDownLatch(1);

BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
    if (event instanceof TaskEvent taskEvent) {
        String status = taskEvent.getTask().status();
        if ("running".equals(status)) {
            progressUpdates.add(extractProgress(taskEvent));
        } else if ("completed".equals(status)) {
            latch.countDown();
        }
    }
};

Client client = Client.builder(agentCard)
    .clientConfig(new ClientConfig.Builder().setStreaming(true).build())
    .addConsumers(List.of(consumer))
    .build();

client.sendMessage(request);
latch.await();
```

## SDK Consumer Registration

**Important:** The A2A Java SDK requires consumers to be registered at **client build time**:

```java
// ✅ CORRECT - Register at build time
Client client = Client.builder(agentCard)
    .addConsumers(List.of(consumer))  // Register here
    .build();

client.sendMessage(request);  // No consumer parameter

// ❌ INCORRECT - Cannot pass to sendMessage
client.sendMessage(request, consumer);  // This doesn't exist!
```

This means you typically need to create a **new Client instance** for each invocation if you want different event handling.

## Dependencies

### Maven

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-client</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>

<!-- A2A Java SDK (transitive dependency) -->
<dependency>
    <groupId>io.github.a2asdk</groupId>
    <artifactId>a2a-java-sdk-client</artifactId>
    <version>0.4.0.Alpha1-SNAPSHOT</version>
</dependency>

<!-- For Spring AI integration only -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
    <version>2.0.0-M1</version>
</dependency>
```

## Examples

See the following for complete working examples:

- **Spring AI Integration:** `spring-ai-a2a-examples/composable-airbnb-planner/`
- **Direct SDK Usage:** `DirectSdkUsageExample.java` in this module's test sources

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Your Application                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Choice 1: Spring AI Integration                           │
│  ┌──────────────────────────────────────────────┐          │
│  │  ChatClient                                  │          │
│  │    └─> A2AToolCallback                       │          │
│  │          └─> A2A Java SDK Client             │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
│  Choice 2: Direct SDK Usage                                │
│  ┌──────────────────────────────────────────────┐          │
│  │  Your Code                                   │          │
│  │    └─> A2A Java SDK Client.builder()        │          │
│  │          └─> A2A Java SDK Client             │          │
│  └──────────────────────────────────────────────┘          │
│                                                             │
└─────────────────────────────────────────────────────────────┘
                          ↓
                     JSON-RPC / HTTP
                          ↓
┌─────────────────────────────────────────────────────────────┐
│                    Remote A2A Agent                         │
│         (spring-ai-a2a-server or any A2A-compatible)        │
└─────────────────────────────────────────────────────────────┘
```

## Key Design Principles

1. **Zero Wrapper Abstractions** - Direct use of A2A Java SDK, no custom client interfaces
2. **Event-Driven First** - Embrace SDK's BiConsumer event model
3. **Spring AI Native** - ToolCallback integration follows Spring AI patterns
4. **Choice of Patterns** - Two clear patterns for different use cases
5. **Direct SDK Usage** - No utility wrappers, use the A2A SDK directly

## Further Reading

- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [A2A Java SDK Documentation](https://github.com/a2asdk/a2a-java-sdk)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI A2A Examples](../spring-ai-a2a-examples/)

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
