# Global Exception Contract Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make global exception responses clearer and safer by unifying framework error codes, returning client-facing messages in `message`, masking unknown Java exceptions, and verifying representative handlers with tests.

**Architecture:** Keep the existing `ServerResponse` envelope and `@RestControllerAdvice` entry points, but centralize framework error code constants and move handler logic onto a clearer contract. Preserve business-specific `BizException` codes while standardizing framework-generated request/auth/database/system codes.

**Tech Stack:** Spring Boot 4, Spring MVC, MockMvc, JUnit 5, AssertJ

---

### Task 1: Lock the desired response contract with tests

**Files:**
- Create: `common-starter/src/test/java/com/dev/lib/web/GlobalExceptionHandlingTest.java`
- Create: `common-data-jpa/src/test/java/com/dev/lib/jpa/handler/DatabaseExceptionHandlerTest.java`
- Modify: `common-starter/src/test/java/com/dev/lib/config/ServerResponseJacksonNullExclusionTest.java`

**Step 1: Write failing tests**

- Assert unknown exceptions return a stable system code and a masked human-readable `message`
- Assert `BizException` returns its business code and readable `message`
- Assert `IllegalArgumentException` does not leak raw Java parser text
- Assert unmatched routes are converted into the unified 404 payload
- Assert duplicate-key/database exceptions use reserved database codes and readable `message`
- Assert `ServerResponse.fail(...)` serializes the client-facing message in `message`

**Step 2: Run tests to verify they fail**

Run targeted Maven tests for the new test classes and confirm failures point at the current response contract.

### Task 2: Implement the clearer framework error code contract

**Files:**
- Create: `common-core/src/main/java/com/dev/lib/web/model/StandardErrorCodes.java`
- Modify: `common-core/src/main/java/com/dev/lib/web/model/ServerResponse.java`
- Modify: `common-core/src/main/java/com/dev/lib/handler/ExceptionHandle.java`
- Modify: `common-security/src/main/java/com/dev/lib/security/TokenException.java`
- Modify: `common-security/src/main/java/com/dev/lib/security/interceptor/PermissionValidator.java`
- Modify: `common-data-jpa/src/main/java/com/dev/lib/jpa/handler/DatabaseExceptionHandler.java`

**Step 1: Add central constants for framework-level codes**

- Reserve consistent ranges for request, auth, HTTP routing, database, and system failures

**Step 2: Make `ServerResponse` put the client-facing failure text into `message`**

- Keep success payloads unchanged
- Preserve the legacy `error` field for compatibility, but mirror the same failure text there

**Step 3: Update exception handlers to use the new code ranges**

- Replace magic numbers in MVC/database handlers
- Always mask unknown exceptions with a generic system message
- Stop returning raw `IllegalArgumentException` messages

**Step 4: Align security-generated framework business exceptions**

- Use the same auth/permission code range in token and permission validation failures

### Task 3: Make 404s reach the global handler

**Files:**
- Modify: `common-starter/src/main/resources/application-lib.yaml`

**Step 1: Enable throwing handler-not-found exceptions**

- Ensure unmatched MVC routes are routed through the existing global advice rather than falling back to `/error`

### Task 4: Verify and summarize residual gaps

**Files:**
- None unless test fixes require minimal follow-up

**Step 1: Run targeted tests**

- Run the `common-starter` and `common-data-jpa` test classes added above

**Step 2: Run a focused regression test already in the repo**

- Re-run `ServerResponseJacksonNullExclusionTest` to confirm serialization compatibility remains acceptable

**Step 3: Record remaining non-MVC gaps**

- Note that async/MQ/background-task exceptions remain outside `@RestControllerAdvice`
