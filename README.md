# UserAccessControl

[![CI](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/ci.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/ci.yml)
[![GitHub Actions](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/actions.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/actions.yml)
[![CodeQL](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/codeql.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/codeql.yml)
[![Cloud E2E](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/cloud-e2e.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/cloud-e2e.yml)
[![SonarCloud](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/sonarcloud.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/sonarcloud.yml)
[![Dependency Review](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-review.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-review.yml)
[![Dependency Submission](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-submission.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-submission.yml)
[![Publish](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/publish.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/publish.yml)

Small Java access-control helper library for resolving the effective permission for a user/resource identity pair.

<!-- ci-status:start -->
## CI Status

| Build | Line Coverage | Branch Coverage | Instruction Coverage | Workflow Run |
| --- | ---: | ---: | ---: | --- |
| ✅ Passing | 100.00% | 94.32% | 99.27% | [#71](https://github.com/vtsyryuk/UserAccessControl/actions/runs/31557795786) |

Last updated from `master` at 2026-08-12 02:44 UTC for commit `b242c84`.
<!-- ci-status:end -->

## Project Links

- Live demo: https://useraccesscontrol.onrender.com
- Render blueprint: `render.yaml`
- Render service dashboard: https://dashboard.render.com/web/srv-d931suuh2hms73d4jsrg
- Cloud E2E workflow: https://github.com/vtsyryuk/UserAccessControl/actions/workflows/cloud-e2e.yml
- Latest verified cloud run: https://github.com/vtsyryuk/UserAccessControl/actions/runs/28636285444
- SonarCloud summary: https://sonarcloud.io/summary/new_code?id=vtsyryuk_UserAccessControl&branch=master

## Requirements

- Java 25
- Gradle 9.6+
- JUnit 6 and Mockito 5 for tests

## Build

```bash
./gradlew clean check
```

The CI workflow runs tests, enforces JaCoCo coverage verification, uploads the HTML/XML coverage reports as artifacts, and publishes a Gradle build scan. CodeQL, SonarCloud, Dependency Review, Dependabot, Gradle dependency submission, cloud UI E2E tests, and GitHub Actions workflow linting are enabled for quality, supply-chain, deployment, and workflow scanning.

Gradle is configured to use a Java 25 toolchain. Dependency versions live in `gradle/libs.versions.toml`; build knobs such as `javaVersion` and `coverageMinimum` live in `gradle.properties` and can be overridden with `-P`.

## Test and Coverage

```bash
./gradlew test jacocoTestReport
```

Current JaCoCo aggregate coverage is 100% for instructions, branches, lines, methods, and classes.

Coverage reports are written to:

- HTML: `build/reports/jacoco/test/html/index.html`
- XML: `build/reports/jacoco/test/jacocoTestReport.xml`

## SonarCloud

SonarCloud analysis runs from the `SonarCloud Analysis` workflow on pushes, pull requests, and manual dispatch. The workflow builds the Java test and JaCoCo XML reports before invoking the Sonar Gradle scanner.

Required GitHub Actions configuration:

- secret `SONAR_TOKEN`: generated from SonarCloud
- variable `SONAR_ORGANIZATION`: the SonarCloud organization key
- variable `SONAR_PROJECT_KEY`: the SonarCloud project key for this repository

The workflow reports coverage from `build/reports/jacoco/test/jacocoTestReport.xml`. The demo HTTP service is excluded from Sonar coverage to match the JaCoCo verification scope for the reusable library. Until the secret and both variables exist, the workflow builds the reports and exits successfully with a notice instead of failing the PR.

## Publish

Packages are deployed to GitHub Packages by the `Publish` workflow when a GitHub release is created, or when the workflow is run manually.

```bash
./gradlew publish -PreleaseVersion=2.0.0
```

To run the same publish path locally, provide GitHub Packages credentials through `gpr.user`/`gpr.key` Gradle properties or `GITHUB_ACTOR`/`GITHUB_TOKEN` environment variables.

## Basic Usage

```java
UserAccessControl repository = userName -> Set.of(
        new ResourcePermission(
                new ResourceIdentity.Builder()
                        .field(new ValueField("repository", "demo"))
                        .field(new WildcardField("path"))
                        .build(),
                UserAccessLevel.READ),
        new ResourcePermission(
                new ResourceIdentity.Builder()
                        .field(new ValueField("repository", "demo"))
                        .field(new ValueField("path", "config/payment.yml"))
                        .build(),
                UserAccessLevel.WRITE));

UserAccessChecker checker = new UserAccessChecker(repository);

ResourceIdentity paymentConfig = new ResourceIdentity.Builder()
        .field(new ValueField("repository", "demo"))
        .field(new ValueField("path", "config/payment.yml"))
        .build();

UserAccessLevel access = checker.getLevel("alice", paymentConfig);
```

## Demo Deployment

The library includes a small HTTP demo service with a browser UI and JSON API. It uses `UserAccessChecker` against a fake repository of keyed resources and demonstrates:

- write permission checks before resource acquisition
- concurrent access attempts against the same resource key
- automatic release when a lease TTL expires
- explicit release through an HTTP command

The hosted demo is deployed on Render at https://useraccesscontrol.onrender.com.

Run it locally:

```bash
./gradlew run
```

Then try:

```bash
open http://localhost:8080
curl http://localhost:8080/resources
curl -X POST 'http://localhost:8080/acquire?user=alice&key=config/payment.yml&ttlSeconds=20'
curl -X POST 'http://localhost:8080/simulate?key=config/payment.yml'
curl -X POST 'http://localhost:8080/command?command=release&leaseId=<lease-id>'
curl -X POST 'http://localhost:8080/release?key=config/payment.yml'
```

The demo users are:

- `alice`: write access to all demo resources
- `carol`: write access to `config/payment.yml`, read access elsewhere
- `bob`: read-only access
- `dave`: no access

### Render Free Cloud Demo

The repository includes `Dockerfile` and `render.yaml` for deploying the demo as a Render Free web service. In Render, create a new Blueprint from this repository. The service starts the Java demo container, exposes `/health`, serves the browser UI from `/`, and keeps lease state in memory.

Render Free web services are suitable for demos and hobby projects, but they can spin down after idle time and their local filesystem is ephemeral. Do not use the demo deployment as production storage or coordination infrastructure.

### Cloud UI E2E

The `Cloud E2E` workflow runs Playwright browser tests against the deployed Render demo. It can run after successful deployment status events, runs daily to keep the cloud demo and status badge fresh, and can also be run manually from GitHub Actions with an optional `base_url` override.

The current Render demo URL was verified by the `Cloud E2E` workflow in run [#28636285444](https://github.com/vtsyryuk/UserAccessControl/actions/runs/28636285444).

Run the same tests locally against any deployed demo:

```bash
npm ci --ignore-scripts
PLAYWRIGHT_BASE_URL=https://useraccesscontrol.onrender.com npm run test:e2e
```

## Access Resolution Flow

1. A caller asks `UserAccessChecker` for a user's level on a `ResourceIdentity`.
2. `UserAccessControl` supplies that user's configured `ResourcePermission` set.
3. Missing identity fields are completed as wildcards so permissions with broader patterns can match.
4. The checker scores exact field matches higher than wildcard matches.
5. The best matching permissions are reduced to `WRITE`, `READ`, or `NONE`, with `NONE` denying access.

## Notes

- `ResourceIdentity` field order is preserved during construction and exposed as an immutable map.
- Exact identity fields are more specific than wildcard fields when permissions are scored.
- If the best matching permissions conflict, `NONE` wins over `READ` or `WRITE`.
- The demo lease store is in-memory and releases expired leases with a scheduled cleanup task.
