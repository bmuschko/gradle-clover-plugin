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