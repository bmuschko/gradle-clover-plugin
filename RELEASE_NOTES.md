### Version 2.0 (October 18, 2014)

* Upgrade to Gradle Wrapper 2.1.
* Changed package name to `com.bmuschko.gradle.clover`.
* Changed group ID to `com.bmuschko`.
* Adapted plugin IDs to be compatible with Gradle's plugin portal.

### Version 0.8.2 (June 8, 2014)

* Expose compiler encoding and executable of underlying Ant task - [Pull Request 47](https://github.com/bmuschko/gradle-clover-plugin/pull/47).

### Version 0.8.1 (December 21, 2013)

* Expose property through DSL for disabling code instrumation and coverage reporting - [Issue 37](https://github.com/bmuschko/gradle-clover-plugin/issues/37).
* Fix aggregation step on sub-project containing no tests - [Issue 34](https://github.com/bmuschko/gradle-clover-plugin/issues/34).
* Apply filter for report aggregation task - [Issue 40](https://github.com/bmuschko/gradle-clover-plugin/issues/40).
* Upgrade to Clover 3.2.0.
* Upgrade to Gradle Wrapper 1.10.
* Publish to Bintray instead of Maven Central.

### Version 0.8 (October 18, 2013)

* Context filters not applied to 'clover-check' task. - [Pull Request 35](https://github.com/bmuschko/gradle-clover-plugin/pull/35).

### Version 0.7 (September 8, 2013)

* Fix AggregateReportsTask String coercion - [Issue 31](https://github.com/bmuschko/gradle-clover-plugin/issues/31).
* Fix deprecation warnings - [Issue 29](https://github.com/bmuschko/gradle-clover-plugin/issues/29).
* Remove dependency on project.configurations.groovy (Gradle 1.4+) - [Issue 23](https://github.com/bmuschko/gradle-clover-plugin/issues/23).
* Upgrade to Gradle Wrapper 1.7.

### Version 0.6.2 (September 6, 2013)

* Coercing GStrings into Strings - [Pull Request 30](https://github.com/bmuschko/gradle-clover-plugin/pull/30).

### Version 0.6.1 (May 5, 2013)

* Allow to apply Clover plugin before applying Java plugin - [Issue 24](https://github.com/bmuschko/gradle-clover-plugin/issues/24).
* Support multiple Test tasks per project - [Issue 25](https://github.com/bmuschko/gradle-clover-plugin/issues/25).
* Allow usage of Clover plugin for projects where not all subprojects have the plugin applied to them - [Issue 26](https://github.com/bmuschko/gradle-clover-plugin/issues/26).

### Version 0.6 (March 9, 2013)

* Added support for test optimization - [Pull Request 20](https://github.com/bmuschko/gradle-clover-plugin/pull/20).
* Upgrade to Clover 3.1.10.
* Upgrade to Gradle Wrapper 1.4.

### Version 0.5.3 (May 8, 2012)

* Correctly evaluate `targetPercentage` convention property - [Issue 16](https://github.com/bmuschko/gradle-clover-plugin/issues/16).

### Version 0.5.2 (March 21, 2012)

* Resolve Clover Ant task definitions only using the `clover` configuration.

### Version 0.5.1 (March 18, 2012)

* Original classes directory didn't get restored correctly - [Issue 12](https://github.com/bmuschko/gradle-clover-plugin/issues/12).
* The task `cloverAggregateReports` depends on `cloverGenerateReport` - [Issue 13](https://github.com/bmuschko/gradle-clover-plugin/issues/13).
* Upgrade to Gradle Wrapper 1.0-m9.

### Version 0.5 (January 12, 2012)

* Make root project not automatically apply Java plugin - [Issue 4](https://github.com/bmuschko/gradle-clover-plugin/issues/4).
* Support for custom source sets - [Issue 8](https://github.com/bmuschko/gradle-clover-plugin/issues/8).

### Version 0.4 (December 20, 2011)

* Upgrade to Gradle Wrapper 1.0-m6 - [Issue 2](https://github.com/bmuschko/gradle-clover-plugin/issues/2).
* Provide license file as file or URL - [Issue 3](https://github.com/bmuschko/gradle-clover-plugin/issues/3).

### Version 0.3 (September 29, 2011)

* Allowing HTML to report on per-test coverage.
* Compilation of Java/Groovy classes is not forked anymore.

### Version 0.2 (September 25, 2011)

* Support for multi-module projects.
* Support for Java/Groovy joint compilation.
* Code coverage include/excludes.
* More convention properties to configure Clover.
* Context filters for exlcuding code blocks from report.
* Configurable JSON code coverage report.
* Task for aggregating coverage reports.
* Bug fixes.

### Version 0.1 (September 20, 2011)

* Initial release.