# A2A Agent ToolCallback Implementation

This package provides `A2AToolCallback` for integrating remote A2A agents with Spring AI's native tool calling mechanism.

## A2AToolCallback - Remote Agent Delegation

**Use Case:** Delegate tasks to remote A2A agents over HTTP using the A2A protocol.

### Features
- Remote agent invocation via A2A protocol
- Streaming response collection with progress tracking
- Synchronous and asynchronous (background) execution
- Automatic client caching per URL
- Configurable timeouts
- Collects progress updates during task execution
- Supports rich artifact responses

### Example - Basic Usage
```java
@Bean
public ToolCallback weatherAgentTool() {
    Map<String, String> agentUrls = Map.of(
        "weather", "http://localhost:10001/a2a",
        "accommodation", "http://localhost:10002/a2a"
    );

    return new A2AToolCallback(agentUrls, Duration.ofMinutes(5));
}
```

### Example - With Multiple Agents
```java
@Configuration
public class A2AAgentConfiguration {

    @Bean
    public ToolCallback a2aAgentTool(
            @Value("${weather.agent.url}") String weatherUrl,
            @Value("${accommodation.agent.url}") String accommodationUrl) {

        Map<String, String> agentUrls = Map.of(
            "weather", weatherUrl,
            "accommodation", accommodationUrl
        );

        return new A2AToolCallback(agentUrls, Duration.ofMinutes(5));
    }
}
```

### Tool Invocation Parameters

The LLM calls the tool with these parameters:

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `description` | String | Yes | Short 3-5 word task summary |
| `prompt` | String | Yes | Full instructions for the remote A2A agent |
| `subagent_type` | String | Yes | Type of remote agent to invoke (key from agentUrls map) |
| `run_in_background` | Boolean | No | Whether to run asynchronously (default: false) |
| `include_progress` | Boolean | No | Include progress updates in result (default: true) |
| `model` | String | No | Optional model override (reserved for future use) |
| `resume` | String | No | Optional task ID to resume (reserved for future use) |

### Response Formats

#### Synchronous Execution (default)
```markdown
## Task Result: Research Paris hotels

**Agent Type:** accommodation (remote A2A)

**Progress:**
- Analyzing hotel search criteria...
- Fetching hotel listings...
- Ranking by user preferences...

[Final hotel recommendations with details]
```

#### Background Execution (when `run_in_background: true`)
```markdown
## Background Task Started

Task ID: `abc-123-def`
Description: Research Paris hotels
Agent Type: accommodation (remote A2A)

**To get the result, use the TaskOutput tool:**
```
TaskOutputTool(task_id: "abc-123-def")
```

The task is running in the background. You can continue with other work.
```

### Configuration Options

| Constructor Parameter | Type | Default | Description |
|----------------------|------|---------|-------------|
| `agentUrls` | Map<String, String> | Required | Map of agent type to A2A endpoint URL |
| `defaultTimeout` | Duration | 5 minutes | Default timeout for agent calls |

### Internal Features

- **Client Caching**: A2AClient instances are cached per URL
- **Progress Tracking**: Automatically collects progress updates from streaming responses
- **Task Management**: Manages background tasks with CompletableFuture
- **Error Handling**: Graceful handling of timeouts, network errors, and agent failures

---

## Integration with Spring AI

A2AToolCallback integrates seamlessly with Spring AI's tool calling flow:

```
User Query
    ↓
LLM analyzes available tools
    ↓
LLM decides to call remote A2A agent
    ↓
Spring AI's ToolCallingManager executes ToolCallback
    ↓
A2A request sent to remote agent
    ↓
Progress updates stream back (collected)
    ↓
Final result with progress returned to LLM
    ↓
LLM synthesizes response
```

---

## Tool Definition

The generated tool definition for the LLM:

```json
{
  "name": "A2AAgent",
  "description": "Delegate work to a specialized remote A2A agent for complex tasks...",
  "input_schema": {
    "type": "object",
    "properties": {
      "description": {
        "type": "string",
        "description": "Short 3-5 word task summary"
      },
      "prompt": {
        "type": "string",
        "description": "Full instructions for the remote A2A agent"
      },
      "subagent_type": {
        "type": "string",
        "description": "Type of remote agent to invoke (e.g., weather, accommodation)"
      },
      "run_in_background": {
        "type": "boolean",
        "description": "Whether to run the task asynchronously (default: false)"
      }
    },
    "required": ["description", "prompt", "subagent_type"]
  }
}
```

---

## Advanced Usage

### Background Task Retrieval

When a task runs in background, you need to poll for results:

```java
// The ToolCallback tracks background tasks internally
String taskOutput = toolCallback.getTaskOutput(taskId);
```

**Note:** Currently, background task retrieval requires manual polling. Future enhancements may provide automatic polling or webhook support.

### Custom Error Handling

Errors are formatted as markdown and returned to the LLM:

```markdown
## Remote A2A Agent Error

**Error:** Remote A2A agent not found: weather

**Details:**
```
Available agents: [accommodation, research]
```

Please check the agent URL and parameters.
```

---

## Error Handling

The implementation handles various error scenarios:

| Error Type | Behavior |
|------------|----------|
| **Agent Not Found** | Returns formatted error with available agent types |
| **Network Timeout** | Throws exception after configured timeout |
| **Streaming Error** | Captures and propagates exception |
| **Task Failure** | Returns error context from A2A agent |
| **Background Task Completion Failure** | Error captured in future, retrievable via getTaskOutput |

---

## Performance Considerations

### Synchronous Execution
- **Latency**: Agent response time + network overhead
- **Memory**: Progress updates and response accumulated in memory
- **Throughput**: Moderate - waits for completion

### Background Execution
- **Latency**: Immediate return (task ID)
- **Memory**: Holds CompletableFuture until completion
- **Throughput**: High - non-blocking

### Recommendations
- Use synchronous for operations < 30 seconds
- Use background for operations > 1 minute
- Consider operation complexity and user experience

---

## Comparison with Local Agent Delegation

For delegating to **local** in-process agents (not remote A2A agents), use `spring-ai-agent-utils` TaskToolCallbackProvider instead:

| Feature | A2AToolCallback (Remote) | TaskToolCallbackProvider (Local) |
|---------|-------------------------------|----------------------------------|
| **Location** | Remote HTTP endpoints | In-process ChatClient |
| **Protocol** | A2A over HTTP | Direct Java calls |
| **Discovery** | Manual URL configuration | File-based markdown definitions |
| **Network** | Yes - requires network | No - same JVM |
| **Use Case** | Distributed agents | Local specialized agents |

---

## Future Enhancements

Potential additions (not yet implemented):

1. **Automatic Polling for Background Tasks**
   - Provide polling tool for LLM
   - Auto-retrieve when task completes

2. **Webhook Support**
   - Agent pushes completion notification
   - Reduces polling overhead

3. **Retry Logic**
   - Automatic retries on transient failures
   - Configurable retry strategies

4. **Circuit Breaker**
   - Prevent cascading failures
   - Graceful degradation

5. **Agent Discovery**
   - Automatic discovery of A2A agents
   - Dynamic registration

6. **Load Balancing**
   - Distribute requests across agent replicas
   - Health checking

---

## References

- [A2A Protocol Specification](https://a2a.anthropic.com/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/)
- [A2A Java SDK](https://github.com/a2asdk/a2a-java-sdk)
- [spring-ai-agent-utils](https://github.com/spring-ai-community/spring-ai-agent-utils) - For local agent delegation
