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

## Project Links

- Demo deployment: https://useraccesscontrol.onrender.com
- Render service dashboard: https://dashboard.render.com/web/srv-d931suuh2hms73d4jsrg
- SonarCloud summary: https://sonarcloud.io/summary/new_code?id=vtsyryuk_UserAccessControl&branch=master

## Build

This project uses Gradle 9.6.1 and JDK 25 by default. Dependency versions live in `gradle/libs.versions.toml`; build knobs such as `javaVersion` and `coverageMinimum` live in `gradle.properties` and can be overridden with `-P`.

```sh
./gradlew clean check
```

The CI workflow runs tests, enforces JaCoCo coverage verification, uploads the HTML/XML coverage reports as artifacts, and publishes a Gradle build scan. CodeQL, SonarCloud, Dependency Review, Dependabot, Gradle dependency submission, cloud UI E2E tests, and GitHub Actions workflow linting are enabled for quality, supply-chain, deployment, and workflow scanning.

## Coverage

Current JaCoCo aggregate coverage is 100% for instructions, branches, lines, methods, and classes. The CI workflow enforces the configured `coverageMinimum` and publishes the full JaCoCo HTML/XML reports as workflow artifacts.

## SonarCloud

SonarCloud analysis runs from the `SonarCloud Analysis` workflow on pushes, pull requests, and manual dispatch. The workflow builds the Java test and JaCoCo XML reports before invoking the Sonar Gradle scanner.

Required GitHub Actions configuration:

- secret `SONAR_TOKEN`: generated from SonarCloud
- variable `SONAR_ORGANIZATION`: the SonarCloud organization key
- variable `SONAR_PROJECT_KEY`: the SonarCloud project key for this repository

The workflow reports coverage from `build/reports/jacoco/test/jacocoTestReport.xml`. The demo HTTP service is excluded from Sonar coverage to match the JaCoCo verification scope for the reusable library. Until the secret and both variables exist, the workflow builds the reports and exits successfully with a notice instead of failing the PR.

## Publishing

GitHub Packages publishing runs from the `Publish` workflow when a GitHub release is created, or manually through `workflow_dispatch`.

```sh
./gradlew publish -PreleaseVersion=2.0.0
```

## Deployment

The library includes a small HTTP demo service with a browser UI and JSON API. It uses `UserAccessChecker` against a fake repository of keyed resources and demonstrates:

- write permission checks before resource acquisition
- concurrent access attempts against the same resource key
- automatic release when a lease TTL expires
- explicit release through an HTTP command

Run it locally:

```sh
./gradlew run
```

Then try:

```sh
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

### Free Cloud Demo

The repository includes `Dockerfile` and `render.yaml` for deploying the demo as a Render Free web service. In Render, create a new Blueprint from this repository. The service starts the Java demo container, exposes `/health`, and keeps lease state in memory.

The demo is auto-deployed on Render at https://useraccesscontrol.onrender.com. The Render service dashboard is available at https://dashboard.render.com/web/srv-d931suuh2hms73d4jsrg.

Render Free web services are suitable for demos and hobby projects, but they can spin down after idle time and their local filesystem is ephemeral. Do not use the demo deployment as production storage or coordination infrastructure.

### Cloud UI E2E

The `Cloud E2E` workflow runs Playwright browser tests against the deployed Render demo. It is triggered by successful deployment status events, runs daily to keep the cloud demo and status badge fresh, and can also be run manually from GitHub Actions with an optional `base_url` override.

Run the same tests locally against any deployed demo:

```sh
npm ci --ignore-scripts
PLAYWRIGHT_BASE_URL=https://useraccesscontrol.onrender.com npm run test:e2e
```
