# Airbnb Travel Planner - Multi-Agent Example

This example demonstrates how to build a **composable multi-agent system** using Spring AI A2A, combining remote A2A agents with MCP tools and optional local tools.

## Overview

A travel planning assistant that orchestrates specialized agents:
- **Travel Planning Agent** (Port 8080) - Main orchestrator with A2A server
- **Accommodation Agent** (Port 10002) - Remote A2A agent for hotel recommendations
- **Weather MCP Tool** - Standard MCP protocol weather forecast
- **Optional: Local Tools** - File system, web search, calculator via spring-ai-agent-utils

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   User Request (A2A Protocol)              │
│  "Plan a 3-day trip to Paris in July with hotel options"  │
└───────────────────────┬────────────────────────────────────┘
                        │
                        │ HTTP POST /a2a
                        ▼
            ┌───────────────────────┐
            │ Travel Planning Agent │ Port: 8080
            │  (A2A Server)         │
            │                       │
            │  ChatClient with:     │
            │  • A2AAgent tool      │
            │  • get-forecast tool  │
            │  • (optional) local   │
            │    agent-utils tools  │
            └───────────┬───────────┘
                        │
                        │ LLM autonomously decides which tools to use
                        ▼
        ┌───────────────┴───────────────┐
        │                               │
        ▼                               ▼
┌───────────────────┐          ┌──────────────┐
│ A2AToolCallback │          │ MCP Weather  │
│                   │          │    Tool      │
│ Routes to:        │          │              │
│ • accommodation   │          │ get-forecast │
│   (Port 10002)    │          └──────────────┘
└───────┬───────────┘              Direct call
        │                          via stdio
        │ HTTP POST /a2a
        ▼
┌───────────────────┐
│ Accommodation     │ Port: 10002
│ Agent (A2A)       │
│                   │
│ Expert in hotel   │
│ recommendations   │
└───────────────────┘
```

## Key Integration Patterns

### 1. Remote A2A Agent - A2AToolCallback

**Pattern**: Delegate complex tasks to independent A2A services

**How it works**:
```java
@Bean
public ToolCallback a2aRemoteAgentTool(@Value("${a2a.agents.accommodation.url}") String url) {
    Map<String, String> agentUrls = Map.of("accommodation", url);
    return new A2AToolCallback(agentUrls, Duration.ofMinutes(2));
}
```

**LLM sees one tool**: `A2AAgent(description, prompt, subagent_type)`

**Configuration** (application.yml):
```yaml
a2a:
  agents:
    accommodation:
      url: http://localhost:10002/a2a
```

**Benefits**:
- ✅ Independent deployment and scaling
- ✅ Specialized expertise with own LLM/prompts
- ✅ Fault isolation
- ✅ Technology flexibility (different models, languages)

### 2. MCP Weather Tool

**Pattern**: Standard Model Context Protocol tools via stdio

**Auto-configured by Spring AI**:
```yaml
spring:
  ai:
    mcp:
      client:
        weather:
          transport: stdio
          command: npx
          args: ["-y", "@modelcontextprotocol/server-weather"]
```

**LLM sees**: `get-forecast(location, days)`

**Benefits**:
- ✅ Standard protocol, wide ecosystem
- ✅ Direct function calls (fast)
- ✅ Auto-registered as ToolCallbacks
- ✅ Language agnostic

### 3. Optional: Local Tools (spring-ai-agent-utils)

**Pattern**: In-process tools for file system, web, calculations

**Add dependency** (pom.xml):
```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

**Auto-configured tools**:
- File system operations
- Web search
- Calculator
- Shell commands
- And more...

**Benefits**:
- ✅ Fast in-process execution
- ✅ No network overhead
- ✅ Rich tool ecosystem
- ✅ Auto-discovery and registration

## Running the Example

### Prerequisites

- Java 17+
- Maven 3.8+
- OpenAI API key
- Node.js (for MCP weather server)

### Step 1: Set Environment

```bash
export OPENAI_API_KEY=your-key-here
```

### Step 2: Start Accommodation Agent (Port 10002)

```bash
cd accommodation-agent
mvn spring-boot:run
```

**Verify it's running**:
```bash
curl -X POST http://localhost:10002/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "agent.execute",
    "params": {
      "prompt": "Recommend hotels in Paris near Eiffel Tower, budget €150/night"
    },
    "id": 1
  }'
```

### Step 3: Start Travel Planning Agent (Port 8080)

```bash
cd travel-planner-agent
mvn spring-boot:run
```

The Travel Planner will automatically:
- Start MCP weather client (npx downloads server)
- Configure A2A remote agent tool
- Register all ToolCallbacks with ChatClient

### Step 4: Test the System

**Plan a complete trip**:
```bash
curl -X POST http://localhost:8080/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "agent.execute",
    "params": {
      "prompt": "Plan a 3-day trip to Paris in July. I need weather forecast and hotel recommendations near major attractions, budget €100-200/night."
    },
    "id": 1
  }'
```

**What happens internally**:

1. **LLM analyzes request** and sees available tools
2. **Calls get-forecast()** → MCP weather tool returns July weather
3. **Calls A2AAgent(subagent_type="accommodation", ...)** → Remote accommodation agent
4. **Synthesizes response** combining weather + hotels + general travel advice

**Watch the logs** to see tool invocations:
```
Travel Planning Agent log:
  • Registered tool: A2AAgent
  • Registered tool: get-forecast
  • LLM calling: get-forecast(location="Paris, France", days=3)
  • LLM calling: A2AAgent(subagent_type="accommodation", ...)

Accommodation Agent log:
  • Received A2A request from Travel Planning Agent
  • Generating hotel recommendations...
```

## Project Structure

### Travel Planning Agent (Port 8080)

```
travel-planner-agent/
├── pom.xml
│   ├── spring-boot-starter-spring-ai-a2a    # A2A server
│   ├── spring-ai-a2a-client                  # A2AToolCallback
│   ├── spring-ai-starter-model-openai        # LLM
│   └── spring-ai-mcp                         # MCP tools
├── src/main/java/.../
│   ├── AirbnbPlannerApplication.java         # Main application
│   ├── TravelPlanningAgentConfiguration.java # Agent builder config
│   └── A2AToolConfiguration.java             # Remote agent tools
└── src/main/resources/
    └── application.yml
        ├── spring.ai.a2a.server.*            # A2A server config
        ├── spring.ai.openai.*                # OpenAI config
        ├── spring.ai.mcp.client.weather.*    # MCP weather
        └── a2a.agents.accommodation.url      # Remote agent URL
```

### Accommodation Agent (Port 10002)

```
accommodation-agent/
├── pom.xml
│   ├── spring-boot-starter-spring-ai-a2a    # A2A server
│   └── spring-ai-starter-model-openai        # LLM
├── src/main/java/.../
│   ├── AccommodationAgentApplication.java    # Main application
│   └── AccommodationAgentConfiguration.java  # Agent with specialized prompt
└── src/main/resources/
    └── application.yml
        ├── server.port: 10002                # Different port
        └── spring.ai.a2a.agent.*             # Agent metadata
```

## Key Implementation Details

### Travel Planning Agent Configuration

**Agent Builder Pattern** (TravelPlanningAgentConfiguration.java):
```java
@Bean
public A2AAgentModel travelPlanningAgent(
        ChatModel chatModel,
        List<ToolCallback> toolCallbacks) {  // Auto-injected!

    return A2AAgentModel.builder()
        .chatClient(ChatClient.builder(chatModel)
            .defaultToolCallbacks(toolCallbacks)  // All tools registered
            .build())
        .systemPrompt(getSystemPrompt())
        .build();
}
```

**Spring Boot auto-injects**:
- `A2AToolCallback` from A2AToolConfiguration
- MCP `get-forecast` tool from spring-ai-mcp
- (Optional) spring-ai-agent-utils tools if dependency added

### A2A Remote Agent Tool Registration

**A2AToolConfiguration.java**:
```java
@Bean
public ToolCallback a2aRemoteAgentTool(
        @Value("${a2a.agents.accommodation.url}") String url) {

    Map<String, String> agentUrls = Map.of("accommodation", url);
    return new A2AToolCallback(agentUrls, Duration.ofMinutes(2));
}
```

**To add more remote agents**:
```java
Map<String, String> agentUrls = Map.of(
    "accommodation", accommodationUrl,
    "transportation", transportationUrl,
    "activities", activitiesUrl
);
```

### Accommodation Agent Configuration

**Simple specialized agent** (AccommodationAgentConfiguration.java):
```java
@Bean
public A2AAgentModel accommodationAgent(ChatModel chatModel) {
    return A2AAgentModel.builder()
        .chatClient(ChatClient.builder(chatModel).build())
        .systemPrompt(getSystemPrompt())  // Accommodation expertise
        .build();
}
```

**System prompt defines specialization**:
- Hotel and Airbnb recommendations
- Neighborhood insights
- Budget optimization
- Booking considerations

## Configuration Reference

### Travel Planning Agent (application.yml)

```yaml
server:
  port: 8080

spring:
  ai:
    # A2A Server (auto-configured)
    a2a:
      server:
        enabled: true
        base-path: /a2a
      agent:
        version: "1.0.0"
        protocol-version: "0.1.0"

    # OpenAI
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7

    # MCP Weather Tools
    mcp:
      client:
        weather:
          transport: stdio
          command: npx
          args: ["-y", "@modelcontextprotocol/server-weather"]

# Remote A2A Agent URLs
a2a:
  agents:
    accommodation:
      url: http://localhost:10002/a2a
```

### Accommodation Agent (application.yml)

```yaml
server:
  port: 10002

spring:
  ai:
    a2a:
      server:
        enabled: true
        base-path: /a2a
      agent:
        name: "Accommodation Agent"
        description: "Expert accommodation specialist"
        version: "1.0.0"

    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o
```

## Adding spring-ai-agent-utils (Optional)

For local in-process tools (file system, web, calculator):

### Step 1: Add Dependency

```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Step 2: That's It!

Spring Boot auto-configuration registers tools automatically:
- File system operations (read, write, list)
- Web search and fetch
- Calculator
- Shell commands
- Task management

**LLM will see additional tools** and use them autonomously.

## Testing Scenarios

### Scenario 1: Simple Hotel Query

```bash
curl -X POST http://localhost:8080/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "agent.execute",
    "params": {
      "prompt": "Find 3 hotels in Rome near Colosseum, budget €120/night"
    },
    "id": 1
  }'
```

**Expected**: Only calls A2AAgent (accommodation)

### Scenario 2: Weather + Hotels

```bash
curl -X POST http://localhost:8080/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "agent.execute",
    "params": {
      "prompt": "Plan 5-day London trip in December. Need weather and hotels."
    },
    "id": 1
  }'
```

**Expected**: Calls get-forecast() + A2AAgent

### Scenario 3: Complex Multi-Step

```bash
curl -X POST http://localhost:8080/a2a \
  -H 'Content-Type: application/json' \
  -d '{
    "jsonrpc": "2.0",
    "method": "agent.execute",
    "params": {
      "prompt": "Compare Paris vs Barcelona for 4-day trip in April. Include weather, hotels, and total budget estimate."
    },
    "id": 1
  }'
```

**Expected**: Multiple tool calls for each city

## Troubleshooting

### MCP Weather Server Fails to Start

**Error**: `Failed to start MCP client: weather`

**Solution**:
```bash
# Verify Node.js and npx
node --version
npx --version

# Test MCP server manually
npx -y @modelcontextprotocol/server-weather
```

### Accommodation Agent Connection Refused

**Error**: `Error calling remote agent: Connection refused`

**Solution**:
```bash
# Check if accommodation agent is running
lsof -i :10002

# Check logs for errors
cd accommodation-agent
mvn spring-boot:run
```

### A2A Tool Not Registered

**Error**: No accommodation recommendations in response

**Solution**: Check logs for tool registration:
```
INFO  TravelPlanningAgentConfiguration - Creating TravelPlanningAgent with 2 tools:
  • A2AAgent - Delegate work to a specialized remote A2A agent...
  • get-forecast - Get weather forecasts...
```

If missing, verify:
1. A2AToolConfiguration is in component scan path
2. `a2a.agents.accommodation.url` is set in application.yml
3. spring-ai-a2a-client dependency is present

## Benefits of This Architecture

### 1. Flexible Composition
- Mix remote A2A agents, MCP tools, and local tools
- Add/remove agents without changing orchestrator code
- LLM decides which tools to use autonomously

### 2. Independent Scaling
- Scale accommodation agent independently
- Different models/configurations per agent
- Deploy agents separately (containers, serverless)

### 3. Technology Diversity
- Remote agents can use any LLM provider
- MCP tools can be written in any language
- Local tools run in-process for speed

### 4. Fault Isolation
- Accommodation agent failure doesn't crash orchestrator
- Can implement retries, fallbacks per agent
- Timeouts configurable per tool

### 5. Clear Boundaries
- Each agent has focused responsibility
- Easier to test and maintain
- Specialized system prompts per domain

## Next Steps

### Add More Remote Agents

1. **Create new agent module** (copy accommodation-agent)
2. **Implement specialized agent** with domain expertise
3. **Register in A2AToolConfiguration**:
```java
Map<String, String> agentUrls = Map.of(
    "accommodation", accommodationUrl,
    "transportation", transportationUrl  // New!
);
```
4. **Start on different port**
5. **Update travel planner system prompt** to describe new capability

### Add More MCP Tools

```yaml
spring:
  ai:
    mcp:
      client:
        filesystem:
          transport: stdio
          command: npx
          args: ["-y", "@modelcontextprotocol/server-filesystem", "/tmp"]
```

### Enable Local Tools

Just add dependency - Spring Boot handles the rest:
```xml
<dependency>
    <groupId>org.springaicommunity</groupId>
    <artifactId>spring-ai-agent-utils</artifactId>
</dependency>
```

## Learn More

- [Spring AI A2A Documentation](../../README.md)
- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [Model Context Protocol](https://modelcontextprotocol.io/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils)

## Summary

This example demonstrates:
- ✅ **A2AToolCallback** for remote agent delegation
- ✅ **MCP integration** for standard protocol tools
- ✅ **Builder pattern** for agent configuration
- ✅ **Spring Boot auto-configuration** for zero-config setup
- ✅ **LLM-driven orchestration** - autonomous tool selection
- ✅ **Composable architecture** - mix patterns as needed

The LLM autonomously decides which tools to use based on your request, and the framework handles all routing complexity!
