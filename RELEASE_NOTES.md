### Version 2.2.0 (December 29, 2017)

Gradle 4.x compatibility release

* Add support for Gradle 4.x - [Issue 91](https://github.com/bmuschko/gradle-clover-plugin/issues/91)

  Migrated all code to work on Gradle 4.4.1. Solved the issues with the new language specific classes directories by completing the work to solve the joint Groovy/Java compile.

* Avoid Joint Compilation of Java Sources by Groovy Compiler- [Issue 55](https://github.com/bmuschko/gradle-clover-plugin/issues/55)

  From now on the expectation is that code in groovy directories will be compiled using the Groovy compiler for both Java and Groovy code colocated. Java code in a java directory will be compiled using the Java compiler. Previously when Groovy was installed all code was compiled with Groovy without care.

* Use TestKit for defining and executing functional tests- [Issue 80](https://github.com/bmuschko/gradle-clover-plugin/issues/80)

  Enabled the use of Gradle TestKit for functional testing. This eliminated the dependency on arcane logic in the build.gradle files that set plugin classpaths in weird ways.

* Automate release process following gradle-docker-plugin scheme- [Issue 96](https://github.com/bmuschko/gradle-clover-plugin/issues/96)

  The changes to include the refined Gradle build logic from the Docker plugin were integrated.  We should have a semi-automatic release process now based on tag pushing.

Other changes:

Removed the support for Clover license. Only support OpenClover.

Cleaned up the Java test code in all functional test data
projects to use Junit 4 annotations instead of Junit 3.

### Version 2.1.3 (September 19, 2017)

* Fix compileOnly dependencies aren't properly added to classpath - [Issue 99](https://github.com/bmuschko/gradle-clover-plugin/issues/99)
* Fix Grails 3 project fails to find Groovyc class - [Issue 89](https://github.com/bmuschko/gradle-clover-plugin/issues/89)
* Fix Make compilerArgs configurable - [Issue 98](https://github.com/bmuschko/gradle-clover-plugin/issues/98)
* Fix Plugin version 2.1.2 does not work with Gradle 2.x - [Issue 94](https://github.com/bmuschko/gradle-clover-plugin/issues/94)

### Version 2.1.2 (July 11, 2017)

New features
* Support columns detail for Clover reports - [Issue 93](https://github.com/bmuschko/gradle-clover-plugin/issues/93)

  See the documentation in [README.md](README.md) for the new usage.

* Fix Add **/*Spec.groovy to default clover.testIncludes - [Issue 90](https://github.com/bmuschko/gradle-clover-plugin/issues/90)
* Fix Tests fail due to differences in polish characters - [Issue 92](https://github.com/bmuschko/gradle-clover-plugin/issues/92)

### Version 2.1.1 (June 26, 2017)

New features
* Generate Clover instrumented JAR files
* Clover Historical Reports are now supported
* Grails 3 is now officially supported by this plugin

  See the documentation in [README.md](README.md) for the new usage.

* Fix Grails 3 tests not executed - [Issue 75](https://github.com/bmuschko/gradle-clover-plugin/issues/75)
* Fix Support for Clover history - [Issue 5](https://github.com/bmuschko/gradle-clover-plugin/issues/5)
* Fix Generate JARs with instrumented classes - [Issue 33](https://github.com/bmuschko/gradle-clover-plugin/issues/33)
* Fix `licenseLocation` is enforced even when using OpenClover - [Issue 85](https://github.com/bmuschko/gradle-clover-plugin/issues/85)

### Version 2.1.0 (May 13, 2017)

Potentially breaking changes in clover convention mapping.
See the documentation in [README.md](README.md) for the new usage.
* `additionalSourceDirs`: was replaced by `additionalSourceSets`
* `additionalTestDirs`: was replaced by `additionalTestSourceSets`


New properties supported in method context filtering.

See the documentation in [README.md](README.md) for the new usage.
* `maxComplexity`: Filter by cyclomatic complexity.
* `maxStatements`: Filter by statements.
* `maxAggregatedComplexity`: Filter by aggregated complexity.
* `maxAggregatedStatements`: Filter by aggregated statements.


Support rules based exception handling with JUnit4.
JUnit4 tests using @Rules based exception expectations would appear as not having a result in previous versions. Additional properties in the report closure support the propagation of the test results from the JUnit XML to the Clover report for completeness.
See the documentation in [README.md](README.md) for the new usage.
* `testResultsDir`: Specifies the location of the JUnit4 test results XML report.
* `testResultsInclude`: Ant file Glob pattern to select Junit4 test results (defaults to TEST-*.xml).


Fixes and enhancements
* Upgrade project to use latest Gradle version (3.5) - [Issue 81](https://github.com/bmuschko/gradle-clover-plugin/issues/81)
* Fix Gradle 3.0 support - [Issue 76](https://github.com/bmuschko/gradle-clover-plugin/issues/76)
* Fix `maxComplexity` of methodContext is not supported by plugin - [Issue 42](https://github.com/bmuschko/gradle-clover-plugin/issues/42)
* Fix JUnit4 rules based exception results reporting
* Allow plugin to be applied in an afterEvaluate listener
* Added support for the newly released OpenClover 4.2.0 - [Issue 83](https://github.com/bmuschko/gradle-clover-plugin/issues/83).
* Fix Java project with groovy tests can't find groovyc - [Issue 59](https://github.com/bmuschko/gradle-clover-plugin/issues/37).
* Fix License issues in multi-project config - [Issue 66](https://github.com/bmuschko/gradle-clover-plugin/issues/66).
* Fix Support `instrumentLambda` Clover property - [Issue 62](https://github.com/bmuschko/gradle-clover-plugin/issues/62).
* Fix java is not compiled with debug info - [Issue 51](https://github.com/bmuschko/gradle-clover-plugin/issues/51).
* Fix `additionalTestDirs` adds sourceSet to test sourceSet - [Issue 11](https://github.com/bmuschko/gradle-clover-plugin/issues/11).
* Added ability to exclude certain test patterns via `testExcludes` - [Pull request 74](https://github.com/bmuschko/gradle-clover-plugin/pull/74)

### Version 2.0.1 (November 15, 2014)

* Changed log level from `warn` to `info` if project doesn't have source code - [Pull Request 49](https://github.com/bmuschko/gradle-clover-plugin/pull/49).

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