# Spring AI A2A Examples

This directory contains examples demonstrating Spring AI's support for the **Agent-to-Agent (A2A) Protocol**.

## Architecture Overview

Spring AI's A2A implementation enables building multi-agent systems where AI agents can discover, communicate, and collaborate using a standardized protocol.

### High-Level Architecture

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          Spring AI A2A Architecture                          │
└──────────────────────────────────────────────────────────────────────────────┘

                            ┌─────────────────┐
                            │   User/Client   │
                            │   Application   │
                            └────────┬────────┘
                                     │
                        HTTP/REST or ChatClient
                                     │
                                     ▼
            ┌────────────────────────────────────────────────┐
            │          Host Agent (Orchestrator)             │
            │     Spring Boot Application (Port 8080)        │
            │                                                │
            │  ┌──────────────────────────────────────────┐ │
            │  │      A2AEndpointRegistry                    │ │
            │  │  - Discovers agent capabilities          │ │
            │  │  - Maintains agent connections           │ │
            │  │  - Manages AgentCards                    │ │
            │  └──────────────────────────────────────────┘ │
            │                                                │
            │  ┌──────────────────────────────────────────┐ │
            │  │      HostAgentOrchestrator               │ │
            │  │  - Routes requests to A2A agents         │ │
            │  │  - Combines multi-agent responses        │ │
            │  └──────────────────────────────────────────┘ │
            └────────┬──────────────────────┬────────────────┘
                     │                      │
          A2A Protocol                   A2A Protocol
       (HTTP/JSON-RPC 2.0)          (HTTP/JSON-RPC 2.0)
                     │                      │
      ┌──────────────▼─────────┐  ┌────────▼──────────────┐
      │   Weather Agent        │  │   Airbnb Agent        │
      │   (Specialist)         │  │   (Specialist)        │
      │   Port: 10001          │  │   Port: 10002         │
      │                        │  │                       │
      │ ┌────────────────────┐ │  │ ┌───────────────────┐│
      │ │ DefaultA2AEndpoint    │ │  │ │ DefaultA2AEndpoint   ││
      │ │ Server             │ │  │ │ Server            ││
      │ │ - Exposes /a2a     │ │  │ │ - Exposes /a2a    ││
      │ │ - Returns AgentCard│ │  │ │ - Returns AgentCard││
      │ └────────────────────┘ │  │ └───────────────────┘│
      │                        │  │                       │
      │ ┌────────────────────┐ │  │ ┌───────────────────┐│
      │ │ AgentExecutor      │ │  │ │ AgentExecutor     ││
      │ │ (SimpleAgent       │ │  │ │ (SimpleAgent      ││
      │ │  Executor)         │ │  │ │  Executor)        ││
      │ │ - onExecute()      │ │  │ │ - onExecute()     ││
      │ │ - Task lifecycle   │ │  │ │ - Task lifecycle  ││
      │ └────────────────────┘ │  │ └───────────────────┘│
      │                        │  │                       │
      │ ┌────────────────────┐ │  │ ┌───────────────────┐│
      │ │ ChatClient         │ │  │ │ ChatClient        ││
      │ │ (OpenAI)           │ │  │ │ (OpenAI)          ││
      │ └────────────────────┘ │  │ └───────────────────┘│
      └────────────────────────┘  └───────────────────────┘
```

## Request Flow Diagram

This diagram shows how a multi-agent request flows through the Spring AI A2A system:

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         Multi-Agent Request Flow                           │
└────────────────────────────────────────────────────────────────────────────┘

Step 1: User Query
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
    User Request: "Find a room in LA, June 20-25. What's the weather?"
                                    │
                                    │ HTTP POST /api/query
                                    ▼
                        ┌──────────────────────┐
                        │   HostAgent          │
                        │   Controller         │
                        └──────────┬───────────┘
                                   │
                                   ▼

Step 2: Request Analysis & Routing
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        ┌──────────────────────┐
                        │  HostAgent           │
                        │  Orchestrator        │
                        │                      │
                        │ analyzeRequest()     │
                        │  - Detect "room"     │
                        │  - Detect "weather"  │
                        │  → Route to BOTH     │
                        └──────────┬───────────┘
                                   │
            ┌──────────────────────┴──────────────────────┐
            │                                             │
            ▼                                             ▼

Step 3: Parallel Agent Calls via A2A Protocol
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌─────────────────────┐                        ┌─────────────────────┐
│  A2AClient     │                        │  A2AClient     │
│  (Weather Agent)    │                        │  (Airbnb Agent)     │
└──────────┬──────────┘                        └──────────┬──────────┘
           │                                              │
           │ POST /a2a                                    │ POST /a2a
           │ JSON-RPC: sendMessage                        │ JSON-RPC: sendMessage
           │ {                                            │ {
           │   "method": "sendMessage",                   │   "method": "sendMessage",
           │   "params": {                                │   "params": {
           │     "message": {                             │     "message": {
           │       "role": "USER",                        │       "role": "USER",
           │       "parts": [{                            │       "parts": [{
           │         "text": "Weather in LA,              │         "text": "Room in LA,
           │                  June 20-25?"                │                  June 20-25?"
           │       }]                                     │       }]
           │     }                                        │     }
           │   }                                          │   }
           │ }                                            │ }
           │                                              │
           ▼                                              ▼
┌──────────────────────┐                      ┌──────────────────────┐
│  Weather Agent       │                      │  Airbnb Agent        │
│  (Port 10001)        │                      │  (Port 10002)        │
│                      │                      │                      │
│  DefaultA2AEndpoint     │                      │  DefaultA2AEndpoint     │
│  Server              │                      │  Server              │
│  ↓                   │                      │  ↓                   │
│  handleJsonRpc       │                      │  handleJsonRpc       │
│  Request()           │                      │  Request()           │
│  ↓                   │                      │  ↓                   │
│  handleSendMessage() │                      │  handleSendMessage() │
└──────────┬───────────┘                      └──────────┬───────────┘
           │                                              │
           ▼                                              ▼

Step 4: Agent Execution
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────┐                      ┌──────────────────────┐
│ WeatherAgent         │                      │ AirbnbAgent          │
│ Executor             │                      │ Executor             │
│                      │                      │                      │
│ extends SimpleAgent  │                      │ extends SimpleAgent  │
│ Executor             │                      │ Executor             │
│                      │                      │                      │
│ onExecute():         │                      │ onExecute():         │
│ ┌──────────────────┐ │                      │ ┌──────────────────┐ │
│ │ 1. Parse request │ │                      │ │ 1. Parse request │ │
│ │ 2. Create prompt │ │                      │ │ 2. Create prompt │ │
│ │ 3. Call ChatClient│ │                     │ │ 3. Call ChatClient│ │
│ │ 4. Return parts  │ │                      │ │ 4. Return parts  │ │
│ └──────────────────┘ │                      │ └──────────────────┘ │
│          │           │                      │          │           │
│          ▼           │                      │          ▼           │
│  ┌────────────────┐  │                      │  ┌────────────────┐  │
│  │  ChatClient    │  │                      │  │  ChatClient    │  │
│  │  (OpenAI)      │  │                      │  │  (OpenAI)      │  │
│  │  ↓             │  │                      │  │  ↓             │  │
│  │  GPT-4         │  │                      │  │  GPT-4         │  │
│  └────────────────┘  │                      │  └────────────────┘  │
└──────────┬───────────┘                      └──────────┬───────────┘
           │                                              │
           ▼                                              ▼

Step 5: Task Lifecycle & Response
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌──────────────────────┐                      ┌──────────────────────┐
│  Task States:        │                      │  Task States:        │
│  SUBMITTED →         │                      │  SUBMITTED →         │
│  WORKING →           │                      │  WORKING →           │
│  COMPLETED           │                      │  COMPLETED           │
│                      │                      │                      │
│  Response:           │                      │  Response:           │
│  {                   │                      │  {                   │
│    "result": {       │                      │    "result": {       │
│      "message": {    │                      │      "message": {    │
│        "role":       │                      │        "role":       │
│         "AGENT",     │                      │         "AGENT",     │
│        "parts": [{   │                      │        "parts": [{   │
│          "text":     │                      │          "text":     │
│           "Sunny,    │                      │           "2BR apt,  │
│            75°F..."  │                      │            $200/nt..." │
│        }]            │                      │        }]            │
│      }               │                      │      }               │
│    }                 │                      │    }                 │
│  }                   │                      │  }                   │
└──────────┬───────────┘                      └──────────┬───────────┘
           │                                              │
           │ A2AResponse                                  │ A2AResponse
           │                                              │
           └──────────────────────┬───────────────────────┘
                                  │
                                  ▼

Step 6: Response Aggregation
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        ┌──────────────────────┐
                        │  HostAgent           │
                        │  Orchestrator        │
                        │                      │
                        │  combineResponses(): │
                        │  ┌─────────────────┐ │
                        │  │ Weather: Sunny, │ │
                        │  │ 75°F            │ │
                        │  │                 │ │
                        │  │ Accommodation:  │ │
                        │  │ 2BR apartment,  │ │
                        │  │ $200/night...   │ │
                        │  └─────────────────┘ │
                        └──────────┬───────────┘
                                   │
                                   ▼

Step 7: Final Response
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                        ┌──────────────────────┐
                        │  HTTP Response       │
                        │  {                   │
                        │    "message": "...", │
                        │    "response": "..." │
                        │  }                   │
                        └──────────┬───────────┘
                                   │
                                   ▼
                            ┌─────────────┐
                            │    User     │
                            └─────────────┘
```

## A2A Protocol Layer Details

### Agent Discovery Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         Agent Discovery Process                            │
└────────────────────────────────────────────────────────────────────────────┘

1. Host Agent Startup
   │
   ├─→ A2AEndpointRegistry.registerAgent("weather", "http://localhost:10001/a2a")
   │
   ├─→ GET http://localhost:10001/a2a
   │   (Fetch AgentCard)
   │
   ├─→ Response:
   │   {
   │     "name": "Weather Agent",
   │     "description": "Provides weather information",
   │     "version": "1.0.0",
   │     "protocolVersion": "0.1.0",
   │     "skills": [
   │       {
   │         "id": "weather-info",
   │         "name": "Get Weather Information",
   │         "description": "Provides current weather...",
   │         "tags": ["weather", "forecast"]
   │       }
   │     ],
   │     "supportedInterfaces": [
   │       {
   │         "protocolBinding": "JSONRPC",
   │         "url": "http://localhost:10001/a2a"
   │       }
   │     ],
   │     "capabilities": {
   │       "streaming": false,
   │       "pushNotifications": false
   │     }
   │   }
   │
   └─→ Create A2AClient instance
       Store in registry as A2AEndpoint interface
```

### Message Exchange Flow

```
┌────────────────────────────────────────────────────────────────────────────┐
│                      A2A JSON-RPC Message Exchange                         │
└────────────────────────────────────────────────────────────────────────────┘

Client Side (Host Agent)
────────────────────────
A2AEndpoint weatherAgent = registry.getAgent("weather");
A2ARequest request = A2ARequest.of("What's the weather in LA?");
A2AResponse response = weatherAgent.sendMessage(request);

↓ Converts to JSON-RPC

POST http://localhost:10001/a2a
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": "uuid-12345",
  "method": "sendMessage",
  "params": {
    "message": {
      "messageId": "msg-67890",
      "role": "ROLE_USER",
      "parts": [
        {
          "text": "What's the weather in LA?"
        }
      ]
    }
  }
}

Server Side (Weather Agent)
────────────────────────────
DefaultA2AServer.handleJsonRpcRequest()
  ↓
handleSendMessage()
  ↓
Create/Update Task (SUBMITTED → WORKING)
  ↓
AgentExecutor.execute(context, eventQueue)
  ↓
WeatherAgentExecutor.onExecute()
  ↓
ChatClient.prompt().user("What's the weather in LA?").call()
  ↓
Add artifact to Task via TaskUpdater
  ↓
Update Task state to COMPLETED
  ↓
Return JSON-RPC response

↓ Response

{
  "jsonrpc": "2.0",
  "id": "uuid-12345",
  "result": {
    "message": {
      "messageId": "msg-54321",
      "role": "ROLE_AGENT",
      "parts": [
        {
          "text": "The weather in Los Angeles is sunny with a temperature of 75°F..."
        }
      ]
    }
  }
}

↓ Converts back to A2AResponse

A2AResponse {
  parts: [TextPart("The weather in Los Angeles is sunny...")]
}
```

## Component Responsibilities

### Core Components

```
┌──────────────────────────────────────────────────────────────────────────┐
│                      Spring AI A2A Components                            │
└──────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ A2AEndpoint Interface                                                      │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ • Unified abstraction for both local and remote agents                 │
│ • Methods:                                                              │
│   - getAgentCard() → AgentCard                                          │
│   - sendMessage(A2ARequest) → A2AResponse                               │
│   - sendMessageStreaming(A2ARequest) → Flux<A2AResponse>               │
│   - supportsStreaming() → boolean                                       │
│                                                                         │
│ Implementations:                                                        │
│ • DefaultA2AClient (remote agents via HTTP)                        │
│ • DefaultA2AServer (local agents, exposes HTTP endpoints)          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ DefaultA2AClient                                                   │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ • Client-side implementation of A2AEndpoint                                │
│ • Uses a2a-java SDK Client for transport                               │
│ • Supports both sync and streaming calls                               │
│ • Builder pattern for configuration                                    │
│                                                                         │
│ Example:                                                                │
│   A2AEndpoint agent = DefaultA2AClient.builder()                      │
│       .agentUrl("http://localhost:10001/a2a")                           │
│       .build();                                                         │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ DefaultA2AServer                                                   │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ • Server-side implementation of A2AEndpoint                                │
│ • @RestController exposing A2A protocol endpoints                      │
│ • Handles JSON-RPC requests (sendMessage, submitTask, getTask)         │
│ • Delegates execution to AgentExecutor                                  │
│ • Manages task lifecycle (SUBMITTED → WORKING → COMPLETED)             │
│                                                                         │
│ Endpoints:                                                              │
│ • GET  /.well-known/agent-card.json → AgentCard                         │
│ • GET  /a2a → AgentCard                                                 │
│ • POST /a2a → JSON-RPC handler                                          │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ AgentExecutorLifecycle                                                  │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ • Simplified interface for Spring AI developers                        │
│ • Bridges to a2a-java SDK AgentExecutor                                │
│ • Lifecycle hooks:                                                      │
│   - onExecute(userInput, context, taskUpdater) → List<Part<?>>         │
│   - onCancel(context, taskUpdater)                                     │
│   - onError(error, context, taskUpdater)                               │
│ • Provides executeSynchronous() for blocking operations                │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ SpringAIAgentExecutor (Interface)                                       │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ • Interface extending AgentExecutor and AgentExecutorLifecycle         │
│ • Defines contract for Spring AI agent integration                     │
│ • Exposes getChatClient() and getSystemPrompt() methods                │
│                                                                         │
│ DefaultSpringAIAgentExecutor (Implementation)                          │
│ • Implements SpringAIAgentExecutor                                      │
│ • Handles full task lifecycle management                               │
│ • Provides SynchronousEventQueue for blocking execution                │
│ • Requires only a system prompt implementation                         │
│ • Automatically routes messages to ChatClient                          │
│ • Best for LLM-based agents with minimal custom logic                  │
│                                                                         │
│ Example:                                                                │
│   public class TestAgent extends DefaultSpringAIAgentExecutor {        │
│     @Override                                                           │
│     public String getSystemPrompt() {                                   │
│       return "You are a helpful assistant...";                          │
│     }                                                                   │
│   }                                                                     │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│ A2AEndpointRegistry                                                        │
│ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ │
│ • Centralized management of A2A agents                                  │
│ • Discovers and caches AgentCards                                       │
│ • Creates DefaultA2AClient instances                               │
│ • Provides lookup by name or skill tags                                │
│                                                                         │
│ Example:                                                                │
│   A2AEndpointRegistry registry = new A2AEndpointRegistry();                   │
│   registry.registerAgent("weather", "http://localhost:10001/a2a");      │
│   A2AEndpoint agent = registry.getAgent("weather");                        │
└─────────────────────────────────────────────────────────────────────────┘
```

## Task Lifecycle

```
┌────────────────────────────────────────────────────────────────────────────┐
│                            Task State Machine                              │
└────────────────────────────────────────────────────────────────────────────┘

                              ┌─────────────┐
                              │  SUBMITTED  │
                              └──────┬──────┘
                                     │
                          Task created and acknowledged
                                     │
                                     ▼
                              ┌─────────────┐
                              │   WORKING   │
                              └──────┬──────┘
                                     │
                          Agent actively processing
                                     │
                   ┌─────────────────┼─────────────────┐
                   │                 │                 │
         onExecute returns    onCancel called   onError called
                   │                 │                 │
                   ▼                 ▼                 ▼
            ┌───────────┐     ┌───────────┐    ┌──────────┐
            │ COMPLETED │     │ CANCELLED │    │  FAILED  │
            └───────────┘     └───────────┘    └──────────┘
              (Terminal)        (Terminal)      (Terminal)


Task States (from io.a2a.spec.TaskState):
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
• SUBMITTED        - Task has been submitted but not started
• WORKING          - Task is actively being processed
• INPUT_REQUIRED   - Task needs additional input from user (not yet implemented)
• AUTH_REQUIRED    - Task requires authentication (not yet implemented)
• COMPLETED        - Task finished successfully (terminal)
• FAILED           - Task encountered an error (terminal)
• CANCELLED        - Task was cancelled by request (terminal)
• REJECTED         - Task was rejected by the agent (not yet implemented)
```

## Technology Stack

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         Technology Dependencies                            │
└────────────────────────────────────────────────────────────────────────────┘

Spring AI A2A Implementation
├── Spring Boot 4.0.0-RC2
│   └── Auto-configuration, dependency injection, web server
│
├── Spring WebFlux (via spring-boot-starter-webflux)
│   └── Reactive HTTP client and server
│
├── a2a-java SDK 0.4.0.Alpha1-SNAPSHOT
│   ├── io.a2a.spec - Protocol specification (AgentCard, Task, Message)
│   ├── io.a2a.server - AgentExecutor, RequestContext, EventQueue
│   ├── io.a2a.client - A2A client implementation
│   └── io.a2a.transport.jsonrpc - JSON-RPC 2.0 transport
│
├── Spring AI ChatClient
│   └── Integration with LLM providers (OpenAI, Anthropic, etc.)
│
└── Project Reactor
    └── Reactive streams for async operations
```

## Examples

### [Airbnb Planner Multi-Agent](./airbnb-planner-multiagent/)

A complete multi-agent travel planning system demonstrating:

- **Host Agent**: Orchestrates requests to specialized agents
- **Weather Agent**: Provides weather information using ChatClient
- **Airbnb Agent**: Simulates accommodation search

**Key Learnings**:
- Building distributed agent systems with A2A
- Agent discovery and registration
- Multi-agent orchestration patterns
- ChatClient integration with A2A agents
- Task lifecycle management

## Getting Started

### Prerequisites

- Java 25+ (required for Maven build)
- Maven or use `./mvnw` wrapper
- OpenAI API key (for ChatClient integration)

### Quick Start

1. **Set your OpenAI API key**:
   ```bash
   export OPENAI_API_KEY=your_key_here
   ```

2. **Build the examples**:
   ```bash
   cd spring-ai-a2a/spring-ai-a2a-examples
   mvn clean install
   ```

3. **Run an example**:
   ```bash
   cd airbnb-planner-multiagent
   # Follow the README.md instructions
   ```

## Key Concepts

### 1. A2AEndpoint Interface

The core abstraction representing both local and remote agents:

```java
public interface A2AEndpoint {
    AgentCard getAgentCard();
    A2AResponse sendMessage(A2ARequest request);
    Flux<A2AResponse> sendMessageStreaming(A2ARequest request);
    boolean supportsStreaming();
}
```

### 2. DefaultSpringAIAgentExecutor Pattern

Extend `DefaultSpringAIAgentExecutor` to create Spring AI agents with minimal code:

```java
public class MyAgentExecutor extends DefaultSpringAIAgentExecutor {

    public MyAgentExecutor(ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public String getSystemPrompt() {
        return "You are a helpful assistant...";
    }

    // Optional: Override onExecute() for custom logic
    @Override
    public List<Part<?>> onExecute(String userInput,
                                    RequestContext context,
                                    TaskUpdater taskUpdater) throws Exception {
        // Custom agent logic here
        String response = getChatClient().prompt()
            .system(getSystemPrompt())
            .user(userInput)
            .call()
            .content();

        return List.of(new TextPart(response));
    }
}
```

### 3. Agent Registration

Use `A2AEndpointRegistry` to discover and manage agents:

```java
A2AEndpointRegistry registry = new A2AEndpointRegistry();

// Register remote agents
registry.registerAgent("weather", "http://localhost:10001/a2a");
registry.registerAgent("airbnb", "http://localhost:10002/a2a");

// Get agent by name
A2AEndpoint weatherAgent = registry.getAgent("weather");

// Find agents by skill tags
List<A2AEndpoint> analysisAgents = registry.findBySkillTags("analysis");
```

### 4. ChatClient Integration

Seamlessly integrate A2A agents with Spring AI's ChatClient:

```java
// Direct agent usage is recommended
A2AEndpoint agent = DefaultA2AClient.builder()
    .agentUrl("http://localhost:10001/a2a")
    .build();

A2AResponse response = agent.sendMessage(A2ARequest.of("What's the weather?"));
```

## Architecture Comparison

### Spring AI A2A vs Python A2A

| Aspect | Spring AI (Java) | Python A2A Samples |
|--------|------------------|-------------------|
| **Base Classes** | `DefaultSpringAIAgentExecutor` | Direct `AgentExecutor` implementation |
| **Agent Interface** | Unified `A2AEndpoint` for local and remote; `SpringAIAgentExecutor` interface for agent executors | Separate client and server abstractions |
| **LLM Integration** | Spring AI `ChatClient` | LangChain, LangGraph, Google GenAI |
| **Lifecycle** | `AgentExecutorLifecycle` with hooks | Direct SDK `AgentExecutor` interface |
| **Discovery** | `A2AEndpointRegistry` with caching | Manual tracking or service discovery |
| **Transport** | JSON-RPC via WebFlux | JSON-RPC, gRPC (via Python SDK) |
| **Configuration** | Spring Boot properties | Environment variables, .env files |

## References

- [A2A Protocol Specification](https://a2a-protocol.org)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [A2A Python Samples](https://github.com/a2aproject/a2a-samples)
- [A2A Java SDK](https://github.com/a2aproject/a2a-java)

## License

Copyright 2025-2026 the original author or authors.

Licensed under the Apache License, Version 2.0.
