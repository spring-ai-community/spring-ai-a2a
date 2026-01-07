# Spring AI A2A (Agent-to-Agent) Protocol Support

[![Maven Central](https://img.shields.io/maven-central/v/org.springaicommunity/spring-ai-a2a-parent.svg)](https://search.maven.org/artifact/org.springaicommunity/spring-ai-a2a-parent)
[![Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

> **Part of the [Spring AI Community](https://github.com/spring-ai-community)** - Community-driven Spring AI integrations and extensions

This project provides Spring AI integration with the [A2A (Agent-to-Agent) Protocol](https://a2a-protocol.org), enabling AI agents built with Spring AI to communicate with other agents across different platforms and programming languages.

## Overview

The A2A protocol is an open standard that enables AI agents to communicate and collaborate seamlessly. This Spring AI Community project makes it as easy to build multi-agent systems as it is to build REST APIs with Spring Boot.

## Quick Start

This library supports two use cases:
1. **Building A2A Agent Servers** - Expose your Spring AI agent via A2A protocol endpoints
2. **Calling Remote A2A Agents** - Connect to and communicate with other A2A agents

Choose the approach that fits your needs:

### Option A: Building an A2A Agent Server (Recommended - Using Spring Boot Starter)

The easiest way to build an A2A agent server is using the Spring Boot Starter:

#### 1. Add the Starter Dependency

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-boot-starter-spring-ai-a2a</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### 2. Configure Your Agent

Add to your `application.yml`:

```yaml
spring:
  ai:
    a2a:
      agent:
        name: Weather Agent
        description: Provides weather information for any location
```

#### 3. Implement Your Agent Logic

```java
@Component
public class WeatherAgentExecutor extends DefaultSpringAIAgentExecutor {

    public WeatherAgentExecutor(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public String getSystemPrompt() {
        return "You are a helpful weather assistant.";
    }

    @Override
    public List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) {
        String response = getChatClient().prompt()
            .system(getSystemPrompt())
            .user(userInput)
            .call()
            .content();
        return List.of(new TextPart(response));
    }
}
```

That's it! Your agent is now available at `http://localhost:8080/a2a` with automatic configuration.

See the [Spring Boot Starter README](spring-boot-starter-spring-ai-a2a/README.md) for more configuration options.

### Option B: Building an A2A Agent Server (Manual Configuration)

If you need more control, you can manually configure your agent:

#### 1. Add Dependencies

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-server</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### 2. Create an A2A Agent

```java
@Configuration
public class WeatherAgentConfig {

    @Bean
    public AgentCard agentCard() {
        return AgentCard.builder()
            .name("Weather Assistant")
            .description("Provides weather information")
            .version("1.0.0")
            .protocolVersion("0.1.0")
            .build();
    }

    @Bean
    public AgentExecutor agentExecutor(ChatClient chatClient) {
        return new DefaultSpringAIAgentExecutor() {
            @Override
            public List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) {
                String response = chatClient.prompt()
                    .user(userInput)
                    .call()
                    .content();
                return List.of(new TextPart(response));
            }
        };
    }

    @Bean
    public A2AAgentServer agentServer(AgentCard agentCard, AgentExecutor agentExecutor) {
        return new DefaultA2AAgentServer(agentCard, agentExecutor);
    }
}
```

That's it! Your agent is now available at `http://localhost:8080/a2a`

### Option C: Calling Remote A2A Agents (Client-Only Applications)

If you only need to **call** other A2A agents (not expose your own), you only need the core dependency:

#### 1. Add Core Dependency

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

#### 2. Create and Use A2A Clients

```java
@Service
public class MyService {

    public String getWeather(String city) {
        // Create A2A agent client
        A2AAgent weatherAgent = DefaultA2AAgentClient.builder()
            .agentUrl("http://weather-agent:8080/a2a")
            .build();

        // Send message to agent
        A2ARequest request = A2ARequest.of("What's the weather in " + city + "?");
        A2AResponse response = weatherAgent.sendMessage(request);

        // Extract text from response
        return response.getParts().stream()
            .filter(part -> part instanceof TextPart)
            .map(part -> ((TextPart) part).text())
            .collect(Collectors.joining());
    }
}
```

## When to Use What

| Your Use Case | Dependency | Description |
|--------------|------------|-------------|
| **Building an A2A agent server** | `spring-boot-starter-spring-ai-a2a` | Use the Spring Boot Starter for auto-configuration |
| **Building a server with manual config** | `spring-ai-a2a-core` + `spring-ai-a2a-server` | For advanced scenarios requiring custom configuration |
| **Only calling other agents (client)** | `spring-ai-a2a-core` | Create `DefaultA2AAgentClient` instances programmatically |
| **Agent orchestration (server + client)** | `spring-boot-starter-spring-ai-a2a` | Use starter for server, create clients programmatically |

## Module Structure

```
spring-ai-a2a/
├── spring-ai-a2a-core/                    # Core interfaces and domain models
│   ├── A2AAgent interface                 # Base interface for agents
│   ├── A2AAgentClient interface           # Client for calling remote agents
│   ├── DefaultA2AAgentClient              # Client implementation
│   └── A2ARequest and A2AResponse         # Request/response models
│
├── spring-ai-a2a-server/                  # Server-side support
│   ├── AgentExecutorLifecycle interface   # Simplified lifecycle hooks
│   ├── SpringAIAgentExecutor interface    # Spring AI adapter
│   ├── DefaultSpringAIAgentExecutor       # Base implementation
│   └── DefaultA2AAgentServer              # Server implementation
│
├── spring-boot-starter-spring-ai-a2a/     # Spring Boot Starter
│   ├── A2AAutoConfiguration               # Auto-configuration
│   └── A2AProperties                      # Configuration properties
│
└── spring-ai-a2a-examples/                # Examples
    └── airbnb-planner-multiagent/         # Multi-agent orchestration example
```

## Core Concepts

### AgentExecutorLifecycle

The `AgentExecutorLifecycle` interface provides simplified lifecycle hooks for agent execution:

```java
public interface AgentExecutorLifecycle {
    // Process the user request and return response content
    List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) throws Exception;

    // Handle task cancellation (optional)
    default void onCancel(RequestContext context, TaskUpdater taskUpdater) throws JSONRPCError {
        taskUpdater.cancel();
    }

    // Handle execution errors (optional)
    default void onError(Exception error, RequestContext context, TaskUpdater taskUpdater) {
        taskUpdater.fail();
    }

    // Execute synchronously and return response
    A2AResponse executeSynchronous(A2ARequest request);
}
```

### SpringAIAgentExecutor and DefaultSpringAIAgentExecutor

**`SpringAIAgentExecutor`** is an adapter interface that combines both the A2A SDK's `AgentExecutor` and Spring AI's `AgentExecutorLifecycle` interfaces, along with `ChatClient` integration:

```java
public interface SpringAIAgentExecutor extends AgentExecutor, AgentExecutorLifecycle {
    ChatClient getChatClient();
    String getSystemPrompt();
}
```

**`DefaultSpringAIAgentExecutor`** is the base implementation that provides:
- Adapter logic bridging A2A SDK's low-level `execute(RequestContext, EventQueue)` to Spring AI's simplified lifecycle hooks
- Task lifecycle management (submit, start, complete)
- ChatClient integration for LLM interactions

Example implementation:

```java
public class MyAgentExecutor extends DefaultSpringAIAgentExecutor {

    public MyAgentExecutor(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public String getSystemPrompt() {
        return "You are a helpful assistant that...";
    }

    @Override
    public List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) {
        String response = getChatClient().prompt()
            .system(getSystemPrompt())
            .user(userInput)
            .call()
            .content();
        return List.of(new TextPart(response));
    }
}
```

This design eliminates the need for separate adapter classes - `SpringAIAgentExecutor` itself serves as the adapter, making Spring AI agents work seamlessly with the A2A protocol.

### Client API

The `A2AAgent` interface provides a unified API for calling both local and remote agents:

```java
// Create agent client
A2AAgent agent = DefaultA2AAgentClient.builder()
    .agentUrl("http://agent-url:8080/a2a")
    .build();

// Synchronous call
A2ARequest request = A2ARequest.of("your message");
A2AResponse response = agent.sendMessage(request);

// Extract text from response
String text = response.getParts().stream()
    .filter(part -> part instanceof TextPart)
    .map(part -> ((TextPart) part).text())
    .collect(Collectors.joining());

// Streaming call
Flux<A2AResponse> stream = agent.sendMessageStreaming(request);
```

## Integration with Spring AI Features

### ChatClient Integration

A2A agents work seamlessly with Spring AI's ChatClient:

```java
public class ResearchAgentExecutor extends DefaultSpringAIAgentExecutor {

    private final ChatClient chatClient;

    public ResearchAgentExecutor(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) {
        String response = chatClient.prompt()
            .user(userInput)
            .call()
            .content();
        return List.of(new TextPart(response));
    }
}
```

### Advisors

All ChatClient advisors work with A2A agents:

```java
public class RagAgentExecutor extends DefaultSpringAIAgentExecutor {

    private final ChatClient chatClient;

    public RagAgentExecutor(ChatClient.Builder builder, VectorStore vectorStore) {
        this.chatClient = builder
            .defaultAdvisors(
                new QuestionAnswerAdvisor(vectorStore),
                new MessageChatMemoryAdvisor(new InMemoryChatMemory())
            )
            .build();
    }

    @Override
    public List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) {
        String response = chatClient.prompt()
            .user(userInput)
            .call()
            .content();
        return List.of(new TextPart(response));
    }
}
```

### Vector Stores

Agents can use vector stores for knowledge retrieval:

```java
public class KnowledgeAgentExecutor extends DefaultSpringAIAgentExecutor {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public KnowledgeAgentExecutor(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    @Override
    public List<Part<?>> onExecute(String userInput, RequestContext context, TaskUpdater taskUpdater) {
        List<Document> docs = vectorStore.similaritySearch(
            SearchRequest.query(userInput).withTopK(5)
        );

        String knowledgeContext = docs.stream()
            .map(Document::getText)
            .collect(Collectors.joining("\n"));

        String response = chatClient.prompt()
            .user(u -> u.text("Context: " + knowledgeContext)
                        .text("Question: " + userInput))
            .call()
            .content();
        return List.of(new TextPart(response));
    }
}
```

## Multi-Agent Patterns

See the [airbnb-planner-multiagent](spring-ai-a2a-examples/airbnb-planner-multiagent/) example for a complete demonstration of multi-agent collaboration patterns including:
- Sequential agent chaining
- Parallel execution
- Intelligent request routing
- Response aggregation

## Examples

### Airbnb Planner Multi-Agent Example

See [airbnb-planner-multiagent](spring-ai-a2a-examples/airbnb-planner-multiagent/) for a complete working example demonstrating:
- Multiple specialized agents (Weather, Airbnb, Host)
- AgentExecutorLifecycle implementation
- ChatClient integration
- Agent orchestration and routing
- A2A protocol communication

## Building on a2a-java SDK

This implementation builds on top of the official [a2a-java SDK](https://github.com/a2aproject/a2a-java), specifically using framework-agnostic core modules:

- `a2a-java-sdk-spec` - Protocol specification and data models
- `a2a-java-sdk-client` - Client implementation
- `a2a-java-sdk-server-common` - Server core (Jakarta EE APIs, no Quarkus)
- `a2a-java-sdk-transport-jsonrpc` - JSON-RPC transport

The Quarkus-based reference implementations are **not** used, making the integration pure Spring.

## Configuration Properties

```properties
# Server port
server.port=8080

# A2A base path
spring.ai.a2a.server.base-path=/a2a
```

## Architecture

```
┌─────────────────────────────────────────────────────┐
│           Spring AI Application                     │
│                                                      │
│  ┌───────────────────────────────────────────────┐ │
│  │  AgentExecutor Implementation                 │ │
│  │  (extends DefaultSpringAIAgentExecutor)                │ │
│  │                                                │ │
│  │  - onExecute() - process requests             │ │
│  │  - onCancel() - handle cancellation           │ │
│  │  - onError() - handle errors                  │ │
│  └───────────────────────────────────────────────┘ │
│                      │                              │
│                      ▼                              │
│  ┌───────────────────────────────────────────────┐ │
│  │  A2AAgentServer                               │ │
│  │  - AgentCard configuration                    │ │
│  │  - AgentExecutor integration                  │ │
│  │  - A2A protocol handling                      │ │
│  └───────────────────────────────────────────────┘ │
│                      │                              │
│                      ▼                              │
│  ┌───────────────────────────────────────────────┐ │
│  │  a2a-java SDK Integration                     │ │
│  │  - TaskUpdater for lifecycle                  │ │
│  │  - EventQueue for streaming                   │ │
│  │  - RequestContext for metadata                │ │
│  └───────────────────────────────────────────────┘ │
│                      │                              │
│                      ▼                              │
│  ┌───────────────────────────────────────────────┐ │
│  │  A2A Protocol Endpoint                        │ │
│  │  /.well-known/agent-card.json                 │ │
│  │  /a2a (JSON-RPC POST)                         │ │
│  └───────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
                       │
                       │ A2A Protocol (JSON-RPC 2.0)
                       │
                       ▼
┌─────────────────────────────────────────────────────┐
│  Other A2A Agents (Python, JavaScript, etc.)        │
└─────────────────────────────────────────────────────┘
```

## TODO / Future Work

- [ ] Enhanced streaming support
- [ ] Integration tests with TCK compliance
- [ ] Additional multi-agent examples
- [ ] Documentation site
- [ ] Performance optimization

## Building

```bash
# Build all modules
cd spring-ai-a2a
mvn clean install -DskipTests

# Build specific module
cd spring-ai-a2a-core
mvn clean install

# Build and run example
cd spring-ai-a2a-examples/weather-agent
export OPENAI_API_KEY=your-key
mvn spring-boot:run
```

## Requirements

- Java 25+ (for Maven build, produces Java 17-compatible artifacts)
- Spring Boot 4.0.0-RC2+
- a2a-java SDK 0.4.0.Alpha1-SNAPSHOT

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines.

## References

- [A2A Protocol Specification](https://a2a-protocol.org)
- [a2a-java SDK](https://github.com/a2aproject/a2a-java)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI CLAUDE.md - A2A Design Section](../CLAUDE.md#a2a-design-for-spring-ai-developers)

## License

Apache License 2.0
