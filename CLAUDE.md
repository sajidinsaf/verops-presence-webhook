# verops-presence-webhook

A simple Spring Boot WAR application that acts as a webhook receiver for the VerOps weekly presence report.

## Project Overview

- **Purpose**: Receive the weekly presence report from VerOps and persist it to the filesystem
- **Deployment**: MochaHost shared Tomcat — `o11y.attunedtechnology.com`
- **Context path**: `/` (ROOT context — WAR is named `ROOT.war`)
- **Webhook URL**: `https://o11y.attunedtechnology.com/verops/demo/weekly-presence-report`
- **Storage path**: `/home/ifaru02/attunedtechnology.com/verops/demo/presence-report/`
- **GitHub repo**: `sajidinsaf/verops-presence-webhook`

## Build

```bash
mvn clean package
```

WAR file will be at `target/ROOT.war`. Deploy by uploading to MochaHost Tomcat `webapps/` — **no renaming needed**.

## Skills

- `/new-change` — SDLC gate: create issue + branch before any code change
- `/build`       — compile, test, package
- `/test`        — run tests
- `/pr`          — create pull request
- `/issue`       — create GitHub issue
- `/deploy`      — build WAR and guide deployment

## Rules

- WAR is always built as `ROOT.war` (context path `/`); the `/verops` prefix is in the controller mapping
- Java 17, Spring Boot 3.x
- No database — file system only
- The storage directory is configurable via `presence.report.storage-dir` property
- Overwrite existing files with the same name — never append
- Tests must pass before any commit
