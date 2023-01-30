# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

### Changed

- Migrated to SpringBoot 3 / JDK17

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