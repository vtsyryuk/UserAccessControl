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

This repository is currently a Java library, not a runnable service, so there is no cloud runtime to deploy directly. A good next step is to publish the package automatically to GitHub Packages and Maven Central, then deploy any consuming service from its own pipeline. If this should become a service, the clean path is to add a thin HTTP API module, containerize it, and deploy to a managed runtime such as Google Cloud Run, AWS App Runner, Azure Container Apps, or Fly.io.
