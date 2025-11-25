# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Changed

- *Breaking* Migration to Spring Boot 4

## [8.0.1](https://github.com/dbmdz/flusswerk/releases/tag/8.0.0) - 2025-10-08

### Fixed

- Gracefully close RabbitMQ connection when application is shutdown

## [8.0.0](https://github.com/dbmdz/flusswerk/releases/tag/8.0.0) - 2025-09-18

### Changed

- *Breaking* Changed groupId and artifactId

## [7.0.1] - 2024-07-30

### Fixed
- Flusswerk Logger can handle function calls with the format String as sole argument.

### Changed
- Reporting of partial retries can now be customized by overriding `ProcessReport.reportComplexRetry(Message message, RetryProcessingException e)` and `ProcessReport.reportComplexFailedAfterMaxRetries(Message message, RetryProcessingException e)`.

## [7.0.0] - 2024-03-15

### Fixed
- Automatically recover from channel-level exceptions. This involves a breaking change in the constructor of `FlusswerkConsumer`, which now requires a `RabbitClient` instead of a `Channel`.

### Changed
- Outgoing routes may now include a list of topics instead of a single topic. A `Route` can be used to send a message to several topics at once.
- The `DefaultFlusswerkReport` logs tracing information.

### Removed
- `FlusswerkProcessReport`, `StructuredProcessReport` and `SilentProcessReport` have been removed. Users who would like to customize their process report can subclass [DefaultProcessReport](framework/src/main/java/com/github/dbmdz/flusswerk/framework/reporting/DefaultProcessReport.java)
or implement the [ProcessReport](framework/src/main/java/com/github/dbmdz/flusswerk/framework/reporting/ProcessReport.java) interface.

## [6.0.1] - 2023-12-06

### Fixed
- Remove SNAPSHOT version from pom

## [6.0.0]

### Fixed

- The Counter `flusswerk_messages_seconds` now records the processing time in seconds ([#444](https://github.com/dbmdz/flusswerk/pull/444))
- Flusswerk default metrics names now follow best practices ([#420](https://github.com/dbmdz/flusswerk/pull/420))

### Changed

- The DefaultProcessReport now supports common defaults and sensible structured fields so that most applications don't need to implement their own ProcessReport anymore ([DefaultProcessReport](https://github.com/dbmdz/flusswerk/blob/main/framework/src/main/java/com/github/dbmdz/flusswerk/framework/reporting/DefaultProcessReport.java))
- Migrated to SpringBoot 3 / JDK17
- `flowInfo.duration()` returns a `Duration` object instead of `long` ([#444](https://github.com/dbmdz/flusswerk/pull/444))
- Support for partial retries: When a workload is split in smaller chunks, then part of these can be successful and part can be retried (e.g. when sending all images of a book individually to cloud services). See [RetryProcessingException.send()](https://github.com/dbmdz/flusswerk/blob/main/framework/src/main/java/com/github/dbmdz/flusswerk/framework/exceptions/RetryProcessingException.java#L61-L69) for details. 
- Flusswerk can now deserialize messages without `@JsonName` annotations.

## [5.1.1] - 2022-11-14
### Fixed

- Rely on automatic connection recovery instead of managing it manually ([#410](https://github.com/dbmdz/flusswerk/pull/410))
- Flusswerk metrics separated from application metrics ([#405](https://github.com/dbmdz/flusswerk/pull/405))
- Tracing information added to messages send with `SkipProcessingException` ([#404](https://github.com/dbmdz/flusswerk/pull/404))

## [5.1.0] - 2022-05-18
### Added

- Message processing can now be skipped by throwing a SkipProcessingException. Log messages now have a field `status` to indicate this. ([#355](https://github.com/dbmdz/flusswerk/pull/355))
- New framework metrics `flusswerk_messages_total` and `flusswerk_messages_seconds` (Counter). The prefix is independent from the application prefix since the metric is the same for all Flusswerk-based applications. ([#381](https://github.com/dbmdz/flusswerk/pull/381)).
- New framework metrics `flusswerk_workers` (Gauge). The prefix is independent from the application prefix since the metric is the same for all Flusswerk-based applications. ([#381](https://github.com/dbmdz/flusswerk/pull/381)).
- Flusswerk applications now can send raw messages: `Topic.sendRaw(byte[] message)` ([#381](https://github.com/dbmdz/flusswerk/pull/381)).

### Changed

- Autoformatting now uses com.spotify:fmt-maven-plugin and works with JDK17.

### Deprecated

- The framework metrics `flusswerk_processed_times` and `flusswerk_execution_time` have been deprecated since they don't follow Prometheus best practices.
- The logging field `duration_ms` is deprecated since a corresponding field `duration` exists and will be removed in the future. Logging and metrics will strictly use SI base units where possible.

### Fixed

- Tracing information got lost when the application threw certain exceptions ([#380](https://github.com/dbmdz/flusswerk/pull/380)).
- The logged values of `duration` and `duration_ms` where switched ([#345](https://github.com/dbmdz/flusswerk/pull/345)).