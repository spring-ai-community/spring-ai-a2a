# Travel Planner - Multi-Agent Example

This example demonstrates a **simple multi-agent system** using the A2A Java SDK directly, with a travel planning orchestrator that delegates to specialized remote agents.

## Overview

A travel planning assistant that coordinates specialized agents:
- **Travel Planner** (Port 8080) - Orchestrator using direct A2A SDK calls
- **Weather Agent** (Port 10001) - Remote A2A agent for weather forecasts
- **Accommodation Agent** (Port 10002) - Remote A2A agent for hotel recommendations

## Architecture

```
┌────────────────────────────────────────────────────────────┐
│                   User Request                             │
│  "Find weather in London"                                  │
└───────────────────────┬────────────────────────────────────┘
                        │
                        │ Interactive Console
                        ▼
            ┌───────────────────────┐
            │ Travel Planner        │ Port: 8080
            │                       │
            │  ChatClient with:     │
            │  • sendMessage tool   │
            │    (RemoteAgent       │
            │     Connections)      │
            └───────────┬───────────┘
                        │
                        │ LLM autonomously decides which agent to call
                        ▼
        ┌───────────────┴───────────────┐
        │                               │
        ▼                               ▼
┌───────────────────┐          ┌──────────────────┐
│ Weather Agent     │          │ Accommodation    │
│ (Port 10001)      │          │ Agent            │
│                   │          │ (Port 10002)     │
│ Tools:            │          │                  │
│ • getCurrentWeather│          │ Tools:           │
│ • getWeatherForecast│         │ • searchAccommodation│
└───────────────────┘          └──────────────────┘
     HTTP POST /a2a                HTTP POST /a2a
```

## Key Components

### 1. Travel Planner (Orchestrator)

**Pattern**: LLM-driven orchestration with direct A2A SDK calls

**How it works**:
```java
@Service
public class RemoteAgentConnections {

    @Tool(description = "Sends a task to a remote agent...")
    public String sendMessage(
            @ToolParam(description = "The name of the agent") String agentName,
            @ToolParam(description = "The task to send") String task) {

        // 1. Get agent card
        AgentCard agentCard = this.cards.get(agentName);

        // 2. Create A2A client
        Client client = Client.builder(agentCard)
            .clientConfig(clientConfig)
            .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
            .addConsumers(List.of(consumer))
            .build();

        // 3. Send message and wait for response
        client.sendMessage(message);
        String result = responseFuture.get(60, TimeUnit.SECONDS);

        return result;
    }
}
```

**LLM sees one tool**: `sendMessage(agentName, task)`

**Benefits**:
- ✅ LLM decides which agent to call based on user query
- ✅ Simple pattern using A2A Java SDK directly
- ✅ Automatic agent discovery via `/a2a/card` endpoint
- ✅ Fault tolerance with timeout handling

### 2. Specialized Agents (Weather & Accommodation)

**Pattern**: Simple agents using `DefaultChatClientAgentExecutor` with domain-specific tools

**How it works**:
```java
@SpringBootApplication
public class WeatherAgentApplication {

    private static final String WEATHER_SYSTEM_INSTRUCTION = """
        You are a specialized weather forecast assistant.
        Use the provided tools to retrieve weather information.
        """;

    @Bean
    public AgentCard agentCard() {
        return new AgentCard.Builder()
            .name("Weather Agent")
            .description("Helps with weather forecasts and climate data")
            .url("http://localhost:10001/a2a")
            .version("1.0.0")
            .protocolVersion("0.3.0")
            .capabilities(new AgentCapabilities.Builder().streaming(false).build())
            .defaultInputModes(List.of("text"))
            .defaultOutputModes(List.of("text"))
            .skills(List.of(new AgentSkill.Builder()
                .id("weather_search")
                .name("Search weather")
                .description("Helps with weather in cities, states, and countries")
                .build()))
            .build();
    }

    @Bean
    public AgentExecutor agentExecutor(
            ChatClient.Builder chatClientBuilder,
            WeatherTools weatherTools) {

        ChatClient chatClient = chatClientBuilder.clone()
            .defaultSystem(WEATHER_SYSTEM_INSTRUCTION)
            .defaultTools(weatherTools)
            .build();

        return new DefaultChatClientAgentExecutor(chatClient);
    }
}
```

**Tools**:
```java
@Component
public class WeatherTools {

    @Tool(description = "Get current weather for a location")
    public String getCurrentWeather(
            @ToolParam(description = "The city and state, e.g. San Francisco, CA")
            String location) {
        // Mock implementation - replace with real API call
        return String.format("Current weather in %s: Sunny, 72°F (22°C), Humidity: 45%%", location);
    }

    @Tool(description = "Get weather forecast for a location")
    public String getWeatherForecast(
            @ToolParam(description = "The city and state") String location,
            @ToolParam(description = "Number of days for forecast (1-7)") int days) {
        // Mock implementation
        // ... return forecast data
    }
}
```

**Benefits**:
- ✅ Uses framework's `DefaultChatClientAgentExecutor` (no boilerplate)
- ✅ Domain-specific tools via `@Tool` annotation
- ✅ Independent deployment and scaling
- ✅ Specialized system prompts per agent

## Running the Example

### Prerequisites

- Java 17+
- Maven 3.8+
- OpenAI API key

### Step 1: Set Environment

```bash
export OPENAI_API_KEY=your-key-here
```

### Step 2: Start Weather Agent (Port 10001)

```bash
cd weather-agent
mvn spring-boot:run
```

**Verify it's running**:
```bash
curl http://localhost:10001/a2a/card
```

You should see the Weather Agent card with its capabilities.

### Step 3: Start Accommodation Agent (Port 10002)

```bash
cd accommodation-agent
mvn spring-boot:run
```

**Verify it's running**:
```bash
curl http://localhost:10002/a2a/card
```

### Step 4: Start Travel Planner

```bash
cd travel-planner
mvn spring-boot:run
```

The Travel Planner will:
1. Auto-discover Weather Agent at http://localhost:10001
2. Auto-discover Accommodation Agent at http://localhost:10002
3. Start interactive console

### Step 5: Use the Interactive Console

```
=== Travel Planning Assistant ===
Ask me anything about trip planning, weather, or accommodations!
Type 'exit' or 'quit' to end the session.

You: What's the weather in Paris?
Assistant: Current weather in Paris: Sunny, 72°F (22°C), Humidity: 45%

Weather forecast for Paris (7 days):
Day 1: Partly cloudy, High: 71°F, Low: 56°F
Day 2: Partly cloudy, High: 72°F, Low: 57°F
...

You: Find me hotels in Paris for 3 nights
Assistant: Found 3 accommodations in Paris:

1. **Le Grand Hotel Paris**
   - Price: €250/night
   - Rating: 4.5/5
   - Location: Near Eiffel Tower
   - Link: https://example.com/hotel1
...
```

## How It Works Internally

1. User submits query to Travel Planner
2. LLM analyzes query and decides which agent(s) to call
3. Travel Planner's `sendMessage` tool is invoked with agent name and task
4. A2A client sends HTTP POST to `/a2a` endpoint of target agent
5. Specialized agent processes request using its domain tools
6. Response flows back through A2A protocol
7. Travel Planner synthesizes final response for user

## Example Queries

Try these queries in the interactive console:

- `What's the weather in London?`
- `Find hotels in Paris for next week`
- `Plan a 3-day trip to Tokyo - I need weather and accommodation`
- `What's the forecast for San Francisco for the next 7 days?`
- `Recommend accommodations in New York for 2 guests`

## Configuration

### Application Properties

**Note**: The configurations shown below are simplified to highlight key settings. See the actual `application.yml` files in each module for complete configurations including logging, agent metadata, and OpenAI settings.

**travel-planner/src/main/resources/application.yml**:
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

# Remote A2A Agent URLs (comma-separated)
remote:
  agents:
    urls: http://localhost:10001,http://localhost:10002
```

**weather-agent/src/main/resources/application.yml**:
```yaml
server:
  port: 10001

spring:
  ai:
    a2a:
      server:
        enabled: true
        base-path: /a2a
    openai:
      api-key: ${OPENAI_API_KEY}
```

**accommodation-agent/src/main/resources/application.yml**:
```yaml
server:
  port: 10002

spring:
  ai:
    a2a:
      server:
        enabled: true
        base-path: /a2a
    openai:
      api-key: ${OPENAI_API_KEY}
```

## Project Structure

```
airbnb-planner-multiagent/
├── travel-planner/
│   └── src/main/java/.../planner/
│       ├── TravelPlannerApplication.java
│       └── RemoteAgentConnections.java
├── weather-agent/
│   └── src/main/java/.../weather/
│       ├── WeatherAgentApplication.java
│       └── WeatherTools.java
└── accommodation-agent/
    └── src/main/java/.../accommodation/
        ├── AccommodationAgentApplication.java
        └── AccommodationTools.java
```

## Troubleshooting

### Port Already in Use
```bash
lsof -ti:10001 | xargs kill -9
lsof -ti:10002 | xargs kill -9
lsof -ti:8080 | xargs kill -9
```

### Agent Discovery Fails
Make sure all agents are running before starting the travel planner. Check logs for connection errors.

### 404 Errors
Verify AgentCard URLs include the `/a2a` path:
- ✅ `http://localhost:10001/a2a`
- ❌ `http://localhost:10001`

### Interactive Console Doesn't Start
The console requires foreground execution. Don't run with `&` or in background mode.

## Key Benefits

1. **Simple Architecture** - Uses A2A Java SDK directly without additional abstractions
2. **LLM-Driven Routing** - No manual routing logic, LLM decides which agent to call
3. **Independent Agents** - Each agent runs independently and can be scaled separately
4. **Standard Patterns** - Uses Spring Boot and Spring AI's `@Tool` annotations
5. **Easy to Extend** - Add new agents by implementing `DefaultChatClientAgentExecutor` and registering tools

## Learn More

- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [A2A Java SDK Documentation](https://github.com/anthropics/a2a-java-sdk)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/reference/)
