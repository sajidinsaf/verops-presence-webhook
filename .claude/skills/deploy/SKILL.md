---
name: deploy
description: Build and deploy WAR to MochaHost Tomcat. Use when the user wants to deploy, release, or push to production.
disable-model-invocation: false
allowed-tools: "Bash(mvn *) Bash(git *) Read Grep"
---

# Deploy — Build and Deploy WAR to MochaHost

Build the `verops.war` and guide deployment to MochaHost Tomcat.

## Instructions

### Step 1: Pre-deploy Checks
1. Verify on the `main` branch
2. Run `mvn test` — ALL tests must pass
3. Check for uncommitted changes — must be clean

### Step 2: Build Production WAR
```
mvn clean package -DskipTests
```
Confirm `target/verops.war` was created. Report file size.

### Step 3: Deployment Checklist
- [ ] Upload `target/verops.war` to MochaHost Tomcat `webapps/` as `verops.war`
- [ ] Ensure storage directory `/home/ifaru02/attunedtechnology.com/verops/demo/presence-report/` exists and is writable
- [ ] Restart Tomcat if needed (via MochaHost cPanel or `startup.sh`)
- [ ] Verify endpoint at `https://o11y.attunedtechnology.com/verops/demo/weekly-presence-report`

### Step 4: Post-deploy Verification
- POST a test payload to the webhook URL and confirm a file is written to the storage directory

### Important Rules
- NEVER deploy with failing tests
- NEVER deploy with uncommitted changes
- WAR MUST be named `verops.war` for the context path `/verops` to work
