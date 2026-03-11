# Common Agent Foundation Implementation Plan

> **For Codex:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

## Context

`common-agent` is a reusable library module inside `common-lib`, not a standalone Spring Boot application.

Implementation goal for this phase:

- add the session core
- add HTTP and SSE entrypoints
- add an in-memory MCP registry
- add a default placeholder agent executor

## Files To Create

- `src/main/java/com/dev/lib/agent/config/AgentProperties.java`
- `src/main/java/com/dev/lib/agent/config/AgentAutoConfiguration.java`
- `src/main/java/com/dev/lib/agent/domain/model/AgentMessage.java`
- `src/main/java/com/dev/lib/agent/domain/model/MessageRole.java`
- `src/main/java/com/dev/lib/agent/domain/model/MessageType.java`
- `src/main/java/com/dev/lib/agent/domain/model/PendingUserMessage.java`
- `src/main/java/com/dev/lib/agent/domain/model/AgentSession.java`
- `src/main/java/com/dev/lib/agent/domain/model/SessionStatus.java`
- `src/main/java/com/dev/lib/agent/domain/service/SessionManager.java`
- `src/main/java/com/dev/lib/agent/infra/session/InMemorySessionRepository.java`
- `src/main/java/com/dev/lib/agent/infra/session/DefaultSessionManager.java`
- `src/main/java/com/dev/lib/agent/infra/stream/SessionStreamHub.java`
- `src/main/java/com/dev/lib/agent/infra/stream/SseSessionStreamHub.java`
- `src/main/java/com/dev/lib/agent/infra/agent/AgentExecutor.java`
- `src/main/java/com/dev/lib/agent/infra/agent/NoOpAgentExecutor.java`
- `src/main/java/com/dev/lib/agent/infra/mcp/McpServerDefinition.java`
- `src/main/java/com/dev/lib/agent/infra/mcp/McpServerRegistry.java`
- `src/main/java/com/dev/lib/agent/infra/mcp/InMemoryMcpServerRegistry.java`
- `src/main/java/com/dev/lib/agent/app/AgentChatAppService.java`
- `src/main/java/com/dev/lib/agent/app/SessionQueryAppService.java`
- `src/main/java/com/dev/lib/agent/app/SessionStreamAppService.java`
- `src/main/java/com/dev/lib/agent/app/McpRegistryAppService.java`
- `src/main/java/com/dev/lib/agent/trigger/http/controller/AgentChatController.java`
- `src/main/java/com/dev/lib/agent/trigger/http/controller/AgentSessionController.java`
- `src/main/java/com/dev/lib/agent/trigger/http/controller/AgentStreamController.java`
- `src/main/java/com/dev/lib/agent/trigger/http/controller/AgentMcpController.java`
- `src/main/java/com/dev/lib/agent/trigger/http/request/ChatRequest.java`
- `src/main/java/com/dev/lib/agent/trigger/http/request/McpRegisterRequest.java`
- `src/main/java/com/dev/lib/agent/trigger/http/response/ChatAcceptedResponse.java`
- `src/main/java/com/dev/lib/agent/trigger/http/response/SessionDetailResponse.java`
- `src/main/java/com/dev/lib/agent/trigger/http/response/McpServerResponse.java`
- `src/test/java/com/dev/lib/agent/domain/model/AgentSessionTest.java`
- `src/test/java/com/dev/lib/agent/infra/session/DefaultSessionManagerTest.java`
- `src/test/java/com/dev/lib/agent/infra/mcp/InMemoryMcpServerRegistryTest.java`
- `src/test/java/com/dev/lib/agent/trigger/http/controller/AgentChatControllerTest.java`

## Task List

### Task 1: Session domain TDD

1. Write `src/test/java/com/dev/lib/agent/domain/model/AgentSessionTest.java`
2. Cover:
   - new session starts in `IDLE`
   - append history trims old non-system messages
   - enqueue rejects overflow
   - close prevents new messages
3. Run:
   ```bash
   mvn -pl common-agent -Dtest=AgentSessionTest test
   ```
4. Expected: FAIL because domain classes do not exist
5. Write minimal domain classes
6. Re-run same command
7. Expected: PASS

### Task 2: Session manager TDD

1. Write `src/test/java/com/dev/lib/agent/infra/session/DefaultSessionManagerTest.java`
2. Cover:
   - create new session
   - reuse existing session
   - mark running only once
   - destroy session removes it
3. Run:
   ```bash
   mvn -pl common-agent -Dtest=DefaultSessionManagerTest test
   ```
4. Expected: FAIL
5. Write repository and manager implementation
6. Re-run same command
7. Expected: PASS

### Task 3: MCP registry TDD

1. Write `src/test/java/com/dev/lib/agent/infra/mcp/InMemoryMcpServerRegistryTest.java`
2. Cover:
   - register new server
   - register same id updates server
   - list returns current entries
3. Run:
   ```bash
   mvn -pl common-agent -Dtest=InMemoryMcpServerRegistryTest test
   ```
4. Expected: FAIL
5. Write registry implementation
6. Re-run same command
7. Expected: PASS

### Task 4: HTTP controller TDD

1. Write `src/test/java/com/dev/lib/agent/trigger/http/controller/AgentChatControllerTest.java`
2. Cover:
   - valid request returns accepted response
   - blank prompt is rejected
3. Run:
   ```bash
   mvn -pl common-agent -Dtest=AgentChatControllerTest test
   ```
4. Expected: FAIL
5. Write request/response DTOs, app service, controller, auto-configuration
6. Re-run same command
7. Expected: PASS

### Task 5: Full module verification

1. Run:
   ```bash
   mvn -pl common-agent test
   ```
2. If failures appear, fix implementation instead of weakening tests
3. Run:
   ```bash
   mvn -pl common-agent -DskipTests compile
   ```
4. Record final evidence in completion summary

## Notes

- Keep queue and history limits configurable
- Do not add real LLM or real MCP network calls in this phase
- Return explicit errors when executor capability is not configured
- Reuse `ServerResponse` from `common-core` for REST responses
