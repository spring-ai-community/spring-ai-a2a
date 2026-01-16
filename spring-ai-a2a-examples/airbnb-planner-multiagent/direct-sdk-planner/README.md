# Airbnb Direct SDK Example

This example demonstrates using the **A2A Java SDK directly** without A2AToolCallback or Spring AI ChatClient integration. It shows programmatic coordination with multiple remote A2A agents.

## Overview

**Pattern:** Direct SDK usage with BiConsumer event handlers

**Architecture:**
```
REST Controller
    ↓
TravelPlannerService
    ├─> Weather Agent (direct SDK call)
    └─> Accommodation Agent (direct SDK call)
```

**Key Difference from A2AToolCallback approach:**
- **This example:** Programmatic control - you decide when to call each agent
- **A2AToolCallback:** LLM control - ChatClient decides when to call agents based on context

## Use Cases

**Use Direct SDK when:**
- ✅ You have deterministic workflows (always call agent X, then agent Y)
- ✅ You don't need LLM orchestration
- ✅ You want full programmatic control
- ✅ You're building non-AI applications that need A2A integration

**Use A2AToolCallback when:**
- ✅ You want LLM to decide which agents to call
- ✅ You need conversational AI capabilities
- ✅ You're building Spring AI ChatClient applications
- ✅ You want automatic tool registration

## Running the Example

### 1. Start the Remote Agents

**Terminal 1 - Weather Agent:**
```bash
cd ../weather-agent
mvn spring-boot:run
# Runs on http://localhost:10001
```

**Terminal 2 - Accommodation Agent:**
```bash
cd ../accommodation-agent
mvn spring-boot:run
# Runs on http://localhost:10002
```

### 2. Start This Application

**Terminal 3:**
```bash
cd direct-sdk-planner
mvn spring-boot:run
# Runs on http://localhost:8081
```

### 3. Test the API

```bash
curl -X POST http://localhost:8081/api/travel/plan \
  -H 'Content-Type: application/json' \
  -d '{
    "destination": "Paris",
    "startDate": "2024-07-01",
    "endDate": "2024-07-05"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "plan": "# Travel Plan for Paris\n\n**Dates:** 2024-07-01 to 2024-07-05\n\n## Weather Forecast\n\n[Weather information from weather agent]\n\n## Accommodation Recommendations\n\n[Hotel recommendations from accommodation agent]\n\n---\n*Generated using direct A2A SDK integration*",
  "error": null
}
```

## Code Walkthrough

### Direct SDK Pattern

The core pattern used in `TravelPlannerService` for each agent call:

```java
private String callAgent(String agentUrl, String query) throws Exception {
    // 1. Create AgentCard for remote agent
    AgentCard agentCard = AgentCard.builder()
        .name("Remote Agent")
        .description("Remote A2A agent")
        .version("1.0.0")
        .protocolVersion("0.1.0")
        .supportedInterfaces(List.of(new AgentInterface("JSONRPC", agentUrl)))
        .build();

    // 2. Create client configuration
    ClientConfig clientConfig = new ClientConfig.Builder()
        .setStreaming(false)
        .setAcceptedOutputModes(List.of("text"))
        .build();

    // 3. Create message request
    Message request = Message.builder()
        .role(Message.Role.USER)
        .parts(List.of(new TextPart(query)))
        .build();

    // 4. Setup synchronous response handling with CountDownLatch
    CountDownLatch latch = new CountDownLatch(1);
    AtomicReference<Message> responseRef = new AtomicReference<>();
    AtomicReference<Throwable> errorRef = new AtomicReference<>();

    // 5. Create BiConsumer event handler
    BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
        if (event instanceof MessageEvent messageEvent) {
            responseRef.set(messageEvent.getMessage());
            latch.countDown();
        }
    };

    // 6. Build client with consumer registered (SDK requirement)
    Client client = Client.builder(agentCard)
        .clientConfig(clientConfig)
        .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
        .addConsumers(List.of(consumer))
        .build();

    // 7. Send message and wait for response
    client.sendMessage(request);
    boolean completed = latch.await(30, TimeUnit.SECONDS);

    // 8. Check errors and extract response
    if (!completed) {
        throw new RuntimeException("Request timed out");
    }
    if (errorRef.get() != null) {
        throw new RuntimeException("Agent error", errorRef.get());
    }

    Message response = responseRef.get();
    return extractTextFromMessage(response);
}
```

### Sequential Coordination

The `planTrip` method shows how to coordinate multiple agents sequentially:

```java
public TravelPlanResponse planTrip(TravelPlanRequest request) {
    // Step 1: Get weather forecast
    String weatherInfo = getWeatherForecast(
        request.destination(),
        request.startDate()
    );

    // Step 2: Get accommodation recommendations
    String accommodationInfo = getAccommodationRecommendations(
        request.destination(),
        request.startDate(),
        request.endDate()
    );

    // Step 3: Combine results programmatically
    String plan = formatTravelPlan(request, weatherInfo, accommodationInfo);

    return new TravelPlanResponse(true, plan, null);
}
```

## Comparison Table

| Aspect | Direct SDK (this example) | A2AToolCallback |
|--------|---------------------------|-----------------|
| **Orchestration** | Manual (programmatic) | Automatic (LLM-driven) |
| **Control Flow** | Explicit method calls | LLM decides when to call |
| **Dependencies** | A2A SDK only | Spring AI + A2A SDK |
| **Agent Selection** | Hardcoded in code | Dynamic by LLM |
| **Workflow Type** | Deterministic | Conversational |
| **Result Combination** | Manual formatting | LLM synthesizes |
| **Error Handling** | Try-catch blocks | Through LLM context |
| **Use Case** | Predictable workflows | AI assistants |

## Key Files

- **`DirectSdkTravelPlannerApplication.java`** - Spring Boot application
- **`TravelPlannerService.java`** - Core service demonstrating direct SDK pattern
- **`TravelPlannerController.java`** - REST API endpoint
- **`application.yml`** - Configuration with agent URLs

## Technical Details

### Why Create New Client for Each Call?

The A2A Java SDK requires BiConsumer event handlers to be registered at **client build time**:

```java
// ✅ CORRECT - Register consumer at build time
Client client = Client.builder(agentCard)
    .addConsumers(List.of(consumer))  // Must be here
    .build();

client.sendMessage(request);  // No consumer parameter

// ❌ INCORRECT - Cannot pass consumer to sendMessage
client.sendMessage(request, consumer);  // Doesn't exist!
```

This means you need a **new Client instance** for each call if you want different event handling per call.

### Event-Driven API

The SDK uses `BiConsumer<ClientEvent, AgentCard>` for callbacks:

```java
BiConsumer<ClientEvent, AgentCard> consumer = (event, card) -> {
    if (event instanceof MessageEvent messageEvent) {
        // Handle message response
    }
    else if (event instanceof TaskEvent taskEvent) {
        // Handle task events (for streaming)
    }
};
```

### Synchronous Blocking with CountDownLatch

Since the SDK is event-driven, we use `CountDownLatch` to block until response arrives:

```java
CountDownLatch latch = new CountDownLatch(1);
AtomicReference<Message> responseRef = new AtomicReference<>();

consumer = (event, card) -> {
    responseRef.set(event.getMessage());
    latch.countDown();  // Unblock waiting thread
};

// Block until latch counts down to 0
latch.await(timeout.toSeconds(), TimeUnit.SECONDS);
```

## Alternative: Parallel Coordination

For parallel agent calls (not shown in this example but easy to add):

```java
public TravelPlanResponse planTripParallel(TravelPlanRequest request) {
    CompletableFuture<String> weatherFuture = CompletableFuture.supplyAsync(
        () -> getWeatherForecast(request.destination(), request.startDate())
    );

    CompletableFuture<String> accommodationFuture = CompletableFuture.supplyAsync(
        () -> getAccommodationRecommendations(/* ... */)
    );

    // Wait for both to complete
    String weatherInfo = weatherFuture.join();
    String accommodationInfo = accommodationFuture.join();

    // Combine and return
    return formatTravelPlan(request, weatherInfo, accommodationInfo);
}
```

## Benefits of Direct SDK Approach

1. **Predictable Execution** - You control exactly when each agent is called
2. **No LLM Costs** - No LLM inference needed for orchestration
3. **Simple Dependencies** - Just A2A SDK, no Spring AI
4. **Deterministic Workflows** - Same input always follows same path
5. **Full Control** - Complete control over error handling, retries, etc.

## When NOT to Use Direct SDK

- ❌ You need conversational AI capabilities
- ❌ You want LLM to decide which tools to use
- ❌ You're building a ChatClient application
- ❌ You need automatic tool discovery
- ❌ You want LLM to synthesize results naturally

For these cases, use the **A2AToolCallback** pattern instead.

## Further Reading

- [A2A Client README](../../spring-ai-a2a-client/README.md) - Comparison of usage patterns
- [DirectSdkUsageExample.java](../../spring-ai-a2a-client/src/test/java/org/springaicommunity/a2a/client/examples/DirectSdkUsageExample.java) - More SDK examples
- [A2AClientUtils](../../spring-ai-a2a-client/src/main/java/org/springaicommunity/a2a/client/A2AClientUtils.java) - Utility documentation

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
