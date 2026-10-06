---
name: test
description: Run JUnit tests. Use when the user wants to run tests or validate the webhook controller.
context: fork
allowed-tools: "Bash(mvn *) Read Grep Glob"
---

# Test — Run and Verify Tests

Run JUnit tests for the webhook receiver.

## Instructions

### Step 1: Run Tests
```
mvn test
```

### Step 2: Analyze Results
- Report total tests, passed, failed, skipped
- If any tests fail, investigate and show failure details

### Step 3: Coverage Check
Every class must have a corresponding test:
- `com.attunedtechnology.verops.webhook.PresenceReportWebhookController` — MockMvc POST tests
- `com.attunedtechnology.verops.webhook.PresenceReportStorageService` — file write/overwrite tests

### Important Rules
- Tests must NOT write to the real production storage path — use a temp directory
- All tests must pass before any commit
