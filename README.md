# Spring AI A2A

Spring Boot integration for building AI agent servers using the [A2A Protocol](https://a2a.anthropic.com/).

## Overview

Spring AI A2A provides **server-side** support for exposing Spring AI agents via the A2A protocol. Add the dependency, provide `ChatClient`, `AgentCard`, and `AgentExecutor` beans, and your agent is automatically exposed at `/a2a` endpoint.

### Key Features

- **A2A Protocol Server**: Expose agents via A2A protocol HTTP/JSON-RPC endpoints
- **Spring AI Integration**: Built on Spring AI's ChatClient
- **A2A SDK Based**: Uses the official A2A Java SDK (0.3.3.Final)
- **Tool Support**: Full support for Spring AI's `@Tool` annotations
- **Extensible**: Override any auto-configured component

## Quick Start

### 1. Add Dependencies

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-a2a-server-autoconfigure</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>

<!-- Spring AI OpenAI (or your preferred model) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-model-openai</artifactId>
</dependency>
```

### 2. Create Your Agent

```java
@SpringBootApplication
public class WeatherAgentApplication {

    private static final String SYSTEM_INSTRUCTION = """
        You are a weather forecasting assistant.
        Use the provided tools to retrieve weather information.
        """;

    public static void main(String[] args) {
        SpringApplication.run(WeatherAgentApplication.class, args);
    }

    @Bean
    public AgentCard agentCard(@Value("${server.port:8080}") int port) {
        return new AgentCard.Builder()
            .name("Weather Agent")
            .description("Provides weather forecasts and climate data")
            .url("http://localhost:" + port + "/a2a/")
            .version("1.0.0")
            .capabilities(new AgentCapabilities.Builder().streaming(false).build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of(new AgentSkill.Builder()
                .id("weather_search")
                .name("Weather Search")
                .description("Provides current weather and forecasts")
                .tags(List.of("weather", "forecast"))
                .build()))
            .protocolVersion("0.3.0")
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

        return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, requestContext) -> {
            String userMessage = DefaultA2AChatClientAgentExecutor
                .extractTextFromMessage(requestContext.getMessage());
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

### 4. Configure Application

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

### 5. Run

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
│  │   ChatClient (your bean)     │  │
│  │   AgentCard (your bean)      │  │
│  │   AgentExecutor (your bean)  │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│  ┌──────────────▼───────────────┐  │
│  │  DefaultA2AChatClientAgent   │  │
│  │  Executor                    │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│  ┌──────────────▼───────────────┐  │
│  │  A2A Server Controllers      │  │
│  │  (auto-configured)           │  │
│  └──────────────┬───────────────┘  │
│                 │                   │
│                 ▼                   │
│         HTTP POST /a2a              │
└─────────────────────────────────────┘
```

**Key Points**:
- You provide `ChatClient`, `AgentCard`, and `AgentExecutor` beans
- Framework auto-configures A2A server components
- A2A endpoints exposed automatically at `/a2a`

### A2A Protocol Endpoints

The framework automatically exposes three REST controllers at the configured `base-path` (default: `/a2a`):

#### 1. MessageController - Core Agent Communication

**Endpoint**: `POST /a2a`

**Purpose**: Handles the A2A protocol's `sendMessage` JSON-RPC requests

**Request Example**:
```json
{
  "jsonrpc": "2.0",
  "method": "sendMessage",
  "params": {
    "message": {
      "role": "user",
      "parts": [{"type": "text", "text": "What's the weather in Paris?"}]
    }
  },
  "id": "1"
}
```

**Response**:
```json
{
  "jsonrpc": "2.0",
  "result": {
    "kind": "task",
    "taskId": "abc123"
  },
  "id": "1"
}
```

**Flow**:
1. Receives `SendMessageRequest` from A2A client
2. Extracts message parameters
3. Delegates to A2A SDK's `RequestHandler.onMessageSend()`
4. `RequestHandler` invokes your `AgentExecutor.execute()`
5. Returns `SendMessageResponse` with task event

**Location**: [MessageController.java](spring-ai-a2a-server/src/main/java/org/springaicommunity/a2a/server/controller/MessageController.java)

#### 2. TaskController - Asynchronous Task Management

**Endpoints**:
- `GET /a2a/tasks/{taskId}` - Get task status and results
- `POST /a2a/tasks/{taskId}/cancel` - Cancel a running task

**Purpose**: Provides task API for long-running operations

**Get Task Example**:
```bash
curl http://localhost:8080/a2a/tasks/abc123
```

**Response**:
```json
{
  "id": "abc123",
  "status": {
    "state": "COMPLETED",
    "message": "Task completed successfully"
  },
  "artifacts": [{
    "parts": [{"type": "text", "text": "Current weather in Paris: Sunny, 72°F"}]
  }]
}
```

**Task States**:
- `SUBMITTED` - Task created and queued
- `WORKING` - Task is executing
- `COMPLETED` - Task finished successfully
- `FAILED` - Task encountered an error
- `CANCELED` - Task was canceled

**Location**: [TaskController.java](spring-ai-a2a-server/src/main/java/org/springaicommunity/a2a/server/controller/TaskController.java)

#### 3. AgentCardController - Agent Discovery

**Endpoint**: `GET /a2a/card`

**Purpose**: Returns agent metadata for discovery

**Response Example**:
```json
{
  "name": "Weather Agent",
  "description": "Provides weather forecasts and climate data",
  "version": "1.0.0",
  "protocolVersion": "0.3.0",
  "capabilities": {
    "streaming": false,
    "pushNotifications": false
  },
  "defaultInputModes": ["text"],
  "defaultOutputModes": ["text"],
  "skills": [{
    "id": "weather_search",
    "name": "Weather Search",
    "description": "Provides current weather and forecasts",
    "tags": ["weather", "forecast"]
  }],
  "supportedInterfaces": [{
    "protocol": "JSONRPC",
    "url": "http://localhost:8080/a2a"
  }]
}
```

**Location**: [AgentCardController.java](spring-ai-a2a-server/src/main/java/org/springaicommunity/a2a/server/controller/AgentCardController.java)

### Complete Request Flow

Here's how an A2A message flows through the system:

```
┌─────────────────────────────────────────────────────────────────┐
│  A2A Client (Remote Agent or Application)                      │
└───────────────────────────┬─────────────────────────────────────┘
                            │
                            │ HTTP POST /a2a
                            │ JSON-RPC sendMessage
                            ▼
┌─────────────────────────────────────────────────────────────────┐
│  Spring Boot Application                                        │
│                                                                  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  1. MessageController (@RestController)                    │ │
│  │     - Receives SendMessageRequest                          │ │
│  │     - Extracts MessageSendParams                           │ │
│  │     - Creates ServerCallContext                            │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  2. RequestHandler (A2A SDK)                               │ │
│  │     - Handles A2A protocol logic                           │ │
│  │     - Creates Task and EventQueue                          │ │
│  │     - Manages task lifecycle                               │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  3. DefaultA2AChatClientAgentExecutor                      │ │
│  │     - Your AgentExecutor bean                              │ │
│  │     - Extracts text from A2A Message                       │ │
│  │     - Updates task state: SUBMITTED → WORKING              │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  4. ChatClientExecutorHandler - YOUR CODE                  │ │
│  │     - Your lambda/function that processes the message      │ │
│  │     - Calls ChatClient with user message                   │ │
│  │     - Returns String response                              │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  5. Spring AI ChatClient                                   │ │
│  │     - Calls LLM with system prompt + user message          │ │
│  │     - Executes @Tool methods if LLM requests them          │ │
│  │     - Returns final response text                          │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  6. DefaultA2AChatClientAgentExecutor                      │ │
│  │     - Wraps response as TextPart artifact                  │ │
│  │     - Updates task: WORKING → COMPLETED                    │ │
│  │     - Adds artifact to task                                │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
│                          ▼                                       │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │  7. RequestHandler (A2A SDK)                               │ │
│  │     - Fires task completion event                          │ │
│  │     - Returns EventKind to MessageController               │ │
│  └───────────────────────┬────────────────────────────────────┘ │
│                          │                                       │
└──────────────────────────┼──────────────────────────────────────┘
                           │
                           ▼
            JSON-RPC Response with TaskEvent
```

**Key Flow Details**:

1. **MessageController** receives the HTTP POST request and deserializes JSON-RPC
2. **RequestHandler** (A2A SDK) manages the A2A protocol including task creation
3. **DefaultA2AChatClientAgentExecutor** bridges A2A protocol to your ChatClient
4. **ChatClientExecutorHandler** (your lambda) implements the actual business logic
5. **ChatClient** handles LLM interaction and tool execution
6. **Response wrapping** happens automatically - you return String, framework wraps as Task artifact
7. **Task lifecycle** is managed entirely by the framework (SUBMITTED → WORKING → COMPLETED)

**What You Implement**:
- `AgentExecutor` bean using `DefaultA2AChatClientAgentExecutor` with a `ChatClientExecutorHandler`
- Your `@Tool` annotated methods (if needed)

**What Framework Handles**:
- A2A protocol JSON-RPC serialization/deserialization
- HTTP endpoint exposure and routing
- Task creation and lifecycle management
- Message extraction from A2A protocol
- Response wrapping as task artifacts
- Error handling and JSON-RPC errors

### Auto-Configuration

All components are automatically configured via `A2AServerAutoConfiguration`:

**Component Scanning**:
```java
@ComponentScan(basePackages = {
    "org.springaicommunity.a2a.server.controller"
})
```

This automatically registers:
- `MessageController` - For A2A protocol messages
- `TaskController` - For task management
- `AgentCardController` - For agent discovery

**Bean Auto-Configuration**:
- `TaskStore` - In-memory task storage (override for Redis/Database)
- `QueueManager` - Event queue management
- `RequestHandler` - A2A SDK request processor
- `A2AConfigProvider` - Configuration from `META-INF/a2a-defaults.properties`
- `PushNotificationConfigStore` - Push notification configuration
- `PushNotificationSender` - No-op implementation (override to enable)
- `Executor` - Thread pool for async operations

**Note**: You must provide your own `AgentExecutor`, `ChatClient`, and `AgentCard` beans.

**Conditional Configuration**:
```yaml
spring:
  ai:
    a2a:
      server:
        enabled: true  # Set to false to disable A2A server
```

### Client-Side: Calling Remote Agents

Use direct A2A SDK calls to communicate with remote agents:

```java
@Service
public class RemoteAgentConnections {

    private final Map<String, AgentCard> cards = new HashMap<>();

    public RemoteAgentConnections(@Value("${remote.agents.urls}") List<String> agentUrls) {
        // Discover agents at startup
        for (String url : agentUrls) {
            AgentCard card = new A2ACardResolver(new JdkA2AHttpClient(), url, "/a2a/card", null)
                .getAgentCard();
            this.cards.put(card.name(), card);
        }
    }

    @Tool(description = "Sends a task to a remote agent")
    public String sendMessage(
            @ToolParam(description = "The agent name") String agentName,
            @ToolParam(description = "The task") String task) {

        AgentCard agentCard = this.cards.get(agentName);

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
```

## Examples

The `spring-ai-a2a-examples/` directory contains complete working examples:

### Multi-Agent System: Airbnb Planner

- **airbnb-planner** - Host agent that orchestrates weather and airbnb agents for travel planning

This example demonstrates:
- LLM-driven agent orchestration
- Direct A2A SDK client calls
- Automatic agent discovery
- Tool-based agent delegation

**Components**:
- `weather-agent` - Weather forecasting agent (Port 10001)
- `airbnb-agent` - Accommodation search agent (Port 10002)
- `host-agent` - Orchestrator agent (Port 8080)

## Project Structure

```
spring-ai-a2a/
├── spring-ai-a2a-server/              # A2A protocol server implementation
│   ├── executor/                      # DefaultA2AChatClientAgentExecutor, ChatClientExecutorHandler
│   └── controller/                    # MessageController, TaskController, AgentCardController
├── spring-ai-a2a-server-autoconfigure/ # Spring Boot auto-configuration
└── spring-ai-a2a-examples/            # Example applications
    └── airbnb-planner/                # Multi-agent example
        ├── weather-agent/             # Weather forecasting agent (Port 10001)
        ├── airbnb-agent/              # Accommodation search agent (Port 10002)
        └── host-agent/                # Orchestrator agent (Port 8080)
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
```

## Implementation Patterns

### Pattern 1: Simple Agent with DefaultA2AChatClientAgentExecutor

Best for most agents - use `DefaultA2AChatClientAgentExecutor` with a `ChatClientExecutorHandler` lambda:

```java
@Bean
public AgentCard agentCard(@Value("${server.port:8080}") int port) {
    return new AgentCard.Builder()
        .name("My Agent")
        .description("Description of what my agent does")
        .url("http://localhost:" + port + "/a2a/")
        .version("1.0.0")
        .capabilities(new AgentCapabilities.Builder().streaming(false).build())
        .defaultInputModes(List.of("text"))
        .defaultOutputModes(List.of("text"))
        .protocolVersion("0.3.0")
        .build();
}

@Bean
public AgentExecutor agentExecutor(
        ChatClient.Builder chatClientBuilder,
        MyTools tools) {

    ChatClient chatClient = chatClientBuilder.clone()
        .defaultSystem("You are a helpful assistant...")
        .defaultTools(tools)
        .build();

    return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, requestContext) -> {
        String userMessage = DefaultA2AChatClientAgentExecutor
            .extractTextFromMessage(requestContext.getMessage());
        return chat.prompt().user(userMessage).call().content();
    });
}
```

**Use when**:
- Simple agents with tools
- Standard ChatClient patterns
- No custom execution logic needed

**How it works**:
- You provide `ChatClient`, `AgentCard`, and `AgentExecutor` beans
- `DefaultA2AChatClientAgentExecutor` handles A2A protocol and task lifecycle
- Your `ChatClientExecutorHandler` lambda implements the business logic
- Framework exposes endpoints at `/a2a`

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
            updater.addArtifact(List.of(new TextPart(response)), null, null, null);

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
public AgentExecutor orchestratorExecutor(
        ChatClient.Builder chatClientBuilder,
        RemoteAgentTools tools) {

    ChatClient chatClient = chatClientBuilder.clone()
        .defaultSystem("You are an orchestrator that delegates to specialized agents...")
        .defaultTools(tools)
        .build();

    return new DefaultA2AChatClientAgentExecutor(chatClient, (chat, requestContext) -> {
        String userMessage = DefaultA2AChatClientAgentExecutor
            .extractTextFromMessage(requestContext.getMessage());
        return chat.prompt().user(userMessage).call().content();
    });
}
```

**Use when**:
- Building multi-agent systems
- Delegating to specialized remote A2A agents
- Composing agent capabilities
- LLM-driven agent orchestration

See the [airbnb-planner example](spring-ai-a2a-examples/airbnb-planner/) for a complete implementation.

## Building

```bash
# Build all modules
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run an example
cd spring-ai-a2a-examples/airbnb-planner/weather-agent
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
- Check `AgentExecutor` is registered as Spring bean
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
- Verify tools are Spring beans (`@Component` or `@Service`)
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
