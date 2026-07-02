# UserAccessControl

[![CI](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/ci.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/ci.yml)
[![Coverage](https://img.shields.io/badge/coverage-100%25-brightgreen.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/ci.yml)
[![GitHub Actions](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/actions.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/actions.yml)
[![CodeQL](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/codeql.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/codeql.yml)
[![Dependency Review](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-review.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-review.yml)
[![Dependency Submission](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-submission.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/dependency-submission.yml)
[![Publish](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/publish.yml/badge.svg)](https://github.com/vtsyryuk/UserAccessControl/actions/workflows/publish.yml)

Small Java access-control helper library for resolving the effective permission for a user/resource identity pair.

## Build

This project uses Gradle 9.5.1 and JDK 25 by default. Dependency versions live in `gradle/libs.versions.toml`; build knobs such as `javaVersion` and `coverageMinimum` live in `gradle.properties` and can be overridden with `-P`.

```sh
./gradlew clean check
```

The CI workflow runs tests, enforces JaCoCo coverage verification, uploads the HTML/XML coverage reports as artifacts, and publishes a Gradle build scan. CodeQL, Dependency Review, Dependabot, Gradle dependency submission, and GitHub Actions workflow linting are enabled for supply-chain and workflow scanning.

## Coverage

Current JaCoCo aggregate coverage is 100% for instructions, branches, lines, methods, and classes. The CI workflow enforces the configured `coverageMinimum` and publishes the full JaCoCo HTML/XML reports as workflow artifacts.

## Publishing

GitHub Packages publishing runs from the `Publish` workflow when a GitHub release is created, or manually through `workflow_dispatch`.

```sh
./gradlew publish -PreleaseVersion=2.0.0
```

## Deployment

The library includes a small HTTP demo service that uses `UserAccessChecker` against a fake repository of keyed resources. It demonstrates:

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

Render Free web services are suitable for demos and hobby projects, but they can spin down after idle time and their local filesystem is ephemeral. Do not use the demo deployment as production storage or coordination infrastructure.
