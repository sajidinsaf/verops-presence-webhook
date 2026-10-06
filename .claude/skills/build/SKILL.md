---
name: build
description: Full project build with validation. Use when compiling, testing, and packaging the application.
disable-model-invocation: false
context: fork
allowed-tools: "Bash(mvn *) Bash(npm *) Read"
---

# Build — Full Project Build with Validation

Run a complete build pipeline with all checks.

## Instructions

### Step 1: Compile
```
mvn clean compile
```
Report any compilation errors.

### Step 2: Run Tests
```
mvn test
```
Report test results. If any test fails, STOP and investigate.

### Step 3: Build Tailwind CSS
```
npm run css:build
```

### Step 4: Package
```
mvn package -DskipTests
```
Confirm the WAR file was created in `target/`.

### Step 5: Report
Show build summary:
- Compilation: PASS/FAIL
- Tests: X passed, Y failed
- CSS Build: PASS/FAIL
- WAR Package: PASS/FAIL + file size

### Important Rules
- Never skip tests in the full build (Step 2 runs them)
- The `-DskipTests` in Step 4 is only because tests already ran in Step 2
- Report the exact error if any step fails
