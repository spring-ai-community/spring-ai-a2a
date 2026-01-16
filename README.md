# Spring AI A2A

Spring AI integration for the Agent-to-Agent (A2A) Protocol, enabling composable AI agents that communicate via the A2A protocol specification.

## Overview

Spring AI A2A provides Spring Boot integration for building AI agents that can communicate with each other using the [A2A Protocol](https://a2a.anthropic.com/). The project uses the [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk) directly for maximum compatibility and minimal abstraction overhead.

### Key Features

- **A2A Protocol Support**: Full implementation of the A2A protocol for agent-to-agent communication
- **Spring Boot Integration**: Auto-configuration and Spring Boot starters for rapid development
- **Direct A2A SDK Usage**: Built directly on the A2A Java SDK without intermediate abstraction layers
- **JSON-RPC Server**: Exposes agents via HTTP/JSON-RPC endpoints
- **Client Library**: Call remote A2A agents from your Spring applications
- **Spring AI Integration**: Seamless integration with Spring AI ChatClient for LLM interactions

## Architecture

### Core Components

The project consists of several modules:

```
spring-ai-a2a/
├── spring-ai-a2a-utils/          # Utility classes and A2A client tools
├── spring-ai-a2a-server/         # A2A server implementation and agent execution
├── spring-ai-a2a-examples/       # Example applications
└── spring-ai-a2a-integration-tests/  # Integration tests
```

### Agent Execution Model

Agents implement the `A2AExecutor` interface, which extends the A2A SDK's `AgentExecutor`:

```java
public interface A2AExecutor extends AgentExecutor {
    ChatClient getChatClient();
    String getSystemPrompt();

    Message executeSynchronous(Message request);
    Flux<Message> executeStreaming(Message request);
}
```

### Message Types

The project uses A2A SDK message types directly:

- `io.a2a.spec.Message` - Messages between agents
- `io.a2a.spec.Part` - Message parts (text, data, etc.)
- `io.a2a.spec.TextPart` - Text message parts
- `io.a2a.spec.AgentCard` - Agent metadata and capabilities

## Quick Start

### Prerequisites

- Java 17 or later
- Maven 3.8+
- OpenAI API key (for examples)

### Building the Project

```bash
# Clone the repository
git clone https://github.com/your-org/spring-ai-a2a.git
cd spring-ai-a2a

# Build all modules
mvn clean install -DskipTests
```

### Creating an A2A Agent

1. **Add the Spring Boot starter dependency:**

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-boot-starter-spring-ai-a2a</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

2. **Create an agent executor:**

```java
@Component
public class MyAgent extends DefaultA2AExecutor {

    public MyAgent(ChatModel chatModel) {
        super(ChatClient.builder(chatModel).build());
    }

    @Override
    public String getSystemPrompt() {
        return "You are a helpful assistant that can answer questions.";
    }
}
```

3. **Configure agent metadata:**

```java
@Bean
public AgentCard agentCard() {
    return AgentCard.builder()
        .name("My Agent")
        .description("A helpful AI assistant")
        .version("1.0.0")
        .protocolVersion("0.1.0")
        .capabilities(AgentCapabilities.builder()
            .streaming(true)
            .build())
        .defaultInputModes(List.of("text"))
        .defaultOutputModes(List.of("text"))
        .supportedInterfaces(List.of(
            new AgentInterface("JSONRPC", "http://localhost:8080/a2a")
        ))
        .build();
}
```

4. **Configure the server (application.yml):**

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
```

5. **Run your agent:**

```bash
mvn spring-boot:run
```

Your agent is now available at `http://localhost:8080/a2a`

### Calling an Agent

Use the A2A client to call remote agents:

```java
@Component
public class MyService {

    private final A2AClient weatherAgent;

    public MyService(@Value("${weather.agent.url}") String weatherAgentUrl) {
        this.weatherAgent = DefaultA2AClient.builder()
            .agentUrl(weatherAgentUrl)
            .timeout(Duration.ofSeconds(30))
            .build();
    }

    public String getWeather(String location) {
        Message request = Message.builder()
            .role(Message.Role.USER)
            .parts(List.of(new TextPart("What's the weather in " + location + "?")))
            .build();

        Message response = weatherAgent.sendMessage(request);
        return MessageUtils.extractText(response.parts());
    }
}
```

## Examples

The project includes several example applications demonstrating different A2A patterns:

### Airbnb Planner Multi-Agent Example

Location: `spring-ai-a2a-examples/airbnb-planner-multiagent/`

Demonstrates a multi-agent system where a travel planning agent delegates to specialized agents:

- **Travel Planner Agent** (port 8080): Main orchestration agent
- **Accommodation Agent** (port 10002): Provides hotel recommendations

**Running the example:**

```bash
# Terminal 1: Start accommodation agent
cd spring-ai-a2a-examples/airbnb-planner-multiagent/accommodation-agent
mvn spring-boot:run

# Terminal 2: Start travel planner agent
cd spring-ai-a2a-examples/airbnb-planner-multiagent/travel-planner-agent
mvn spring-boot:run

# Terminal 3: Test the agent
curl -X POST http://localhost:8080/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "sendMessage",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"text": "Plan a 3-day trip to Tokyo"}]
      }
    },
    "id": 1
  }'
```

## Configuration

### Agent Server Configuration

Configure the A2A server in `application.yml`:

```yaml
spring:
  ai:
    a2a:
      server:
        enabled: true
        base-path: /a2a              # Default: /a2a
      agent:
        name: "My Agent"
        description: "Agent description"
        version: "1.0.0"
        protocol-version: "0.1.0"
        capabilities:
          streaming: true
          push-notifications: false
          state-transition-history: false
        default-input-modes:
          - text
        default-output-modes:
          - text
```

### ChatModel Configuration

The project supports any Spring AI ChatModel. Example with OpenAI:

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
```

## Task Management

Spring AI A2A integrates with [spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils) for standardized background task management. This allows A2A agents to execute long-running operations asynchronously while providing status tracking and result retrieval.

### Features

- **TaskRepository**: Centralized storage for background tasks
- **TaskOutputTool**: Spring AI ToolCallback for retrieving task results
- **Auto-configuration**: Automatic setup via Spring Boot
- **Customizable**: Override default beans for custom implementations

### How It Works

When an A2A agent is called with `run_in_background: true`, the task is stored in the `TaskRepository` and a task ID is returned. The LLM can then use the `TaskOutputTool` to check the task status and retrieve results.

### Usage Example

**1. Background Task Execution:**

```java
// A2AToolCallback automatically uses TaskRepository
Map<String, String> agentUrls = Map.of(
    "research", "http://localhost:10003/a2a"
);

ToolCallback a2aTool = new A2AToolCallback(agentUrls);

// When LLM calls with run_in_background=true:
// A2AAgent(subagent_type: "research", prompt: "Research quantum computing", run_in_background: true)
// Returns: "Task ID: abc-123"
```

**2. Task Result Retrieval:**

The `TaskOutputTool` is automatically registered as a ToolCallback:

```java
// LLM can call TaskOutput tool to check results:
// TaskOutput(task_id: "abc-123")
// Returns task status and result
```

**3. Custom TaskRepository:**

Override the default in-memory implementation:

```java
@Configuration
public class CustomTaskConfiguration {

    @Bean
    public TaskRepository taskRepository() {
        // Custom implementation (e.g., Redis-backed)
        return new RedisTaskRepository(...);
    }
}
```

### Auto-Configuration

The `TaskConfiguration` class provides automatic setup:

- **TaskRepository Bean**: `DefaultTaskRepository` (in-memory ConcurrentHashMap)
- **TaskOutputTool Bean**: Tool for retrieving task results
- **Conditional Beans**: Use `@ConditionalOnMissingBean` for customization

### Integration with A2AToolCallback

The `A2AToolCallback` constructor accepts an optional `TaskRepository`:

```java
// With TaskRepository injection
@Bean
public ToolCallback a2aTool(
        @Value("${agents.urls}") Map<String, String> agentUrls,
        TaskRepository taskRepository) {
    return new A2AToolCallback(agentUrls, Duration.ofMinutes(5), taskRepository);
}

// Without (uses DefaultTaskRepository internally)
@Bean
public ToolCallback a2aTool(@Value("${agents.urls}") Map<String, String> agentUrls) {
    return new A2AToolCallback(agentUrls);
}
```

### Task Execution Flow

```
┌─────────────┐
│     LLM     │
└──────┬──────┘
       │ A2AAgent(run_in_background: true)
       ▼
┌─────────────────┐
│ A2AToolCallback │
└────────┬────────┘
         │ taskRepository.putTask()
         ▼
┌─────────────────┐
│ TaskRepository  │
└────────┬────────┘
         │ Returns task_id
         ▼
┌─────────────┐
│     LLM     │ Receives: "Task ID: abc-123"
└──────┬──────┘
       │ TaskOutput(task_id: "abc-123")
       ▼
┌─────────────────┐
│ TaskOutputTool  │
└────────┬────────┘
         │ taskRepository.getTask()
         ▼
┌─────────────────┐
│ TaskRepository  │
└────────┬────────┘
         │ Returns task status/result
         ▼
┌─────────────┐
│     LLM     │ Receives task result
└─────────────┘
```

### Configuration Properties

No configuration properties are required. The system uses sensible defaults:

- **Default timeout**: 5 minutes for A2A agent calls
- **Default repository**: `DefaultTaskRepository` (in-memory)
- **Automatic tool registration**: `TaskOutputTool` registered automatically

### Further Reading

- [spring-ai-agent-utils Documentation](https://github.com/spring-ai-community/spring-ai-agent-utils)
- [TaskRepository API](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/src/main/java/org/springaicommunity/agent/tools/task/repository/TaskRepository.java)
- [BackgroundTask API](https://github.com/spring-ai-community/spring-ai-agent-utils/blob/main/src/main/java/org/springaicommunity/agent/tools/task/repository/BackgroundTask.java)

## API Reference

### A2AExecutor

Main interface for implementing agents:

```java
public interface A2AExecutor extends AgentExecutor {
    // Build an agent executor
    static A2AExecutorBuilder builder() { ... }

    // Get the ChatClient for LLM interactions
    ChatClient getChatClient();

    // Get the system prompt for the agent
    String getSystemPrompt();

    // Generate response from user input
    default List<Part<?>> generateResponse(String userInput) { ... }

    // Execute synchronously
    Message executeSynchronous(Message request);

    // Execute with streaming
    default Flux<Message> executeStreaming(Message request) { ... }
}
```

### A2AClient

Client for calling remote A2A agents:

```java
public interface A2AClient {
    // Get agent metadata
    AgentCard getAgentCard();

    // Send a message to the agent
    Message sendMessage(Message request);

    // Send a message with streaming response
    Flux<Message> streamMessage(Message request);
}
```

### A2AServer

Server that exposes agents via HTTP/JSON-RPC:

```java
public interface A2AServer {
    // Get agent metadata
    AgentCard getAgentCard();

    // Process an A2A request
    Object handleRequest(A2ARequest request);
}
```

## JSON-RPC Protocol

Agents are exposed via JSON-RPC 2.0 over HTTP.

### Send Message

```json
{
  "jsonrpc": "2.0",
  "method": "sendMessage",
  "params": {
    "message": {
      "role": "user",
      "parts": [{"text": "Hello, agent!"}]
    }
  },
  "id": 1
}
```

### Get Agent Card

```json
{
  "jsonrpc": "2.0",
  "method": "getAgentCard",
  "params": {},
  "id": 1
}
```

## Testing

Run the integration tests:

```bash
# Run all tests
mvn test

# Run integration tests only
cd spring-ai-a2a-integration-tests
mvn test
```

## Project Status

**Version**: 0.1.0-SNAPSHOT
**Status**: Active Development

### Recent Changes (2026-01-13)

- Removed spring-ai-agents dependency
- Migrated to use A2A Java SDK directly
- Renamed `A2AAgentModel` to `A2AExecutor` for better clarity
- Simplified architecture with direct SDK usage

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting pull requests.

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.

## Resources

- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [A2A Java SDK](https://github.com/anthropics/a2a-java-sdk)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

## Support

For questions and support:
- GitHub Issues: [Report issues](https://github.com/your-org/spring-ai-a2a/issues)
- Discussions: [Join discussions](https://github.com/your-org/spring-ai-a2a/discussions)
