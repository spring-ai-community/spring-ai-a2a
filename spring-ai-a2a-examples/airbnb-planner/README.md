# Airbnb Planner Multi-Agent Example

A multi-agent travel planning demo using the Spring AI A2A framework, based on the [A2A Samples Airbnb Planner](https://github.com/a2aproject/a2a-samples/tree/main/samples/python/agents/airbnb_planner_multiagent).

## Architecture

```
User → Host Agent (10000/host) → Airbnb Agent (10001/airbnb) → MCP Airbnb Server
                               → Weather Agent (10002/weather) → Open-Meteo API
```

The Host Agent is an orchestrator that routes user requests to specialized agents:

| Agent | Port | Context Path | Description |
|-------|------|--------------|-------------|
| `host-agent` | 10000 | `/host` | Orchestrator that routes requests to remote agents |
| `airbnb-agent` | 10001 | `/airbnb` | Accommodation search via MCP Airbnb server |
| `weather-agent` | 10002 | `/weather` | Weather forecasts via Open-Meteo API |

## Prerequisites

- Java 17+
- Maven 3.8+
- An LLM API key (Anthropic, OpenAI, or Google GenAI)

## Running the Example

### 1. Set your API key

Choose one of the supported LLM providers:

```bash
# Anthropic Claude
export ANTHROPIC_API_KEY=your-key

# OR OpenAI
export OPENAI_API_KEY=your-key

# OR Google GenAI
export GOOGLE_CLOUD_PROJECT=your-project-id
```

### 2. Start the agents

Start each agent in a separate terminal from the project root:

```bash
# Terminal 1: Start Weather Agent
cd spring-ai-a2a-examples/airbnb-planner/weather-agent
mvn spring-boot:run

# Terminal 2: Start Airbnb Agent
cd spring-ai-a2a-examples/airbnb-planner/airbnb-agent
mvn spring-boot:run

# Terminal 3: Start Host Agent (after the other agents are running)
cd spring-ai-a2a-examples/airbnb-planner/host-agent
mvn spring-boot:run
```

### 3. Verify agents are running

Check each agent's card endpoint:

```bash
# Weather Agent
curl http://localhost:10002/weather/card | jq

# Airbnb Agent
curl http://localhost:10001/airbnb/card | jq
```

### 4. Send requests

Use the Host Agent's chat endpoint:

```bash
curl -X POST http://localhost:10000/host/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Find accommodation in LA for April 15-18, 2025 for 2 adults and tell me the weather"}'
```

Or send an A2A protocol message directly:

```bash
curl -X POST http://localhost:10000/host \
  -H "Content-Type: application/json" \
  -d '{
    "jsonrpc": "2.0",
    "method": "sendMessage",
    "params": {
      "message": {
        "role": "user",
        "parts": [{"type": "text", "text": "What is the weather in Paris?"}]
      }
    },
    "id": "1"
  }'
```

## How It Works

### Host Agent

The Host Agent is an LLM-powered orchestrator that:
1. Discovers remote agents at startup via their A2A card endpoints
2. Uses `@Tool` methods to delegate tasks to remote agents
3. Routes requests based on the user's query content

Key components:
- [HostApplication.java](host-agent/src/main/java/org/springaicommunity/a2a/examples/host/HostApplication.java) - Main application with ChatClient configuration
- [RemoteAgentConnections.java](host-agent/src/main/java/org/springaicommunity/a2a/examples/host/RemoteAgentConnections.java) - A2A client tools for remote agent communication
- [RoutingController.java](host-agent/src/main/java/org/springaicommunity/a2a/examples/host/RoutingController.java) - HTTP endpoint for chat

### Weather Agent

A specialized agent for weather forecasting:
- Uses [Open-Meteo API](https://open-meteo.com/) for real weather data
- Exposes temperature lookup via `@Tool` annotation

Key components:
- [WeatherAgentApplication.java](weather-agent/src/main/java/org/springaicommunity/a2a/examples/composable/weather/WeatherAgentApplication.java) - Agent configuration
- [WeatherTools.java](weather-agent/src/main/java/org/springaicommunity/a2a/examples/composable/weather/WeatherTools.java) - Weather API integration

### Airbnb Agent

A specialized agent for accommodation search:
- Uses MCP (Model Context Protocol) to connect to an Airbnb search server
- Tools are provided by the MCP server

Key components:
- [AirbnbPlannerApplication.java](airbnb-agent/src/main/java/org/springaicommunity/a2a/examples/composable/airbnb/AirbnbPlannerApplication.java) - Agent configuration with MCP tools

## Configuration

Each agent supports multiple LLM providers. Configure via `application.properties`:

```properties
# Anthropic Claude
spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY}
spring.ai.anthropic.chat.options.model=claude-sonnet-4-5-20250929

# OpenAI
spring.ai.openai-sdk.api-key=${OPENAI_API_KEY}
spring.ai.openai-sdk.chat.options.model=gpt-5-mini-2025-08-07

# Google GenAI
spring.ai.google.genai.project-id=${GOOGLE_CLOUD_PROJECT}
spring.ai.google.genai.chat.options.model=gemini-3-pro-preview
```

## A2A Endpoints

Each agent exposes standard A2A protocol endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/card` | GET | Agent metadata (AgentCard) |
| `/` | POST | Send message (JSON-RPC sendMessage) |
| `/tasks/{taskId}` | GET | Get task status |
| `/tasks/{taskId}/cancel` | POST | Cancel task |

## License

Apache License 2.0
