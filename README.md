# Gradle Clover plugin [![Build Status](https://travis-ci.org/bmuschko/gradle-clover-plugin.svg?branch=master)](https://travis-ci.org/bmuschko/gradle-clover-plugin)

![Clover Logo](https://www.appfusions.com/download/attachments/13959273/logoClover.png?version=1&modificationDate=1353815596170)

<table border=1>
    <tr>
        <td>
            Over the past couple of years this plugin has seen many releases. Thanks to everyone involved!
            Unfortunately, I don't have much time to contribute anymore. In practice this means far less activity,
            responsiveness on issues and new releases from my end.
        </td>
    </tr>
    <tr>
        <td>
            I am
            <a href="https://discuss.gradle.org/t/looking-for-new-owners-for-gradle-plugins/9735">actively looking for contributors</a>
            willing to take on maintenance and implementation of the project. If you are interested and would love to see this
            plugin continue to thrive, shoot me a <a href="mailto:benjamin.muschko@gmail.com">mail</a>.
        </td>
    </tr>
</table>

The plugin provides generation of code coverage reports using [Clover](http://www.atlassian.com/software/clover/).

## Usage

To use the Clover plugin, include in your build script:

    apply plugin: 'com.bmuschko.clover'

The plugin JAR needs to be defined in the classpath of your build script. It is directly available on
[Bintray](https://bintray.com/bmuschko/gradle-plugins/com.bmuschko%3Agradle-clover-plugin).
Alternatively, you can download it from GitHub and deploy it to your local repository. The following code snippet shows an
example on how to retrieve it from Bintray:

    buildscript {
        repositories {
            jcenter()
        }

        dependencies {
            classpath 'com.bmuschko:gradle-clover-plugin:2.1.0'
        }
    }

To define the Clover dependency please use the `clover` configuration name in your `dependencies` closure. If you are working
with a multi-module project make sure you apply the plugin and declare the `clover` dependency within the `allprojects` closure.

    dependencies {
        clover 'org.openclover:clover:4.2.0'
    }

With the introduction of the OpenClover support the `licenseLocation` clover convention is
now completely optional. If specified the plugin will verify it is an existing file or URL
location. If unspecified the default location `${project.rootDir}/clover.license` will be
used only if the default is an existing file. Omitting the specification of the `licenseLocation`
when the Atlassian Clover library is used will cause an error.

## Tasks

The Clover plugin defines the following tasks:

* `cloverGenerateReport`: Generates Clover code coverage report.
* `cloverAggregateReports`: Aggregate Clover code coverage reports in a multi-module project setup. This task can only be
run from the root directory of your project and requires at least one submodule. This task depends on `cloverGenerateReport`.

## Convention properties

* `initString`: The location to write the Clover coverage database (defaults to `.clover/clover.db`). The location you
define will always be relative to the project's build directory.
* `enabled`: Controls whether Clover will instrument code during code compilation (defaults to `true`).
* `classesBackupDir`: The temporary backup directory for classes (defaults to `file("${sourceSets.main.classesDir}-bak")`).
* `licenseLocation`: The [Clover license](http://confluence.atlassian.com/display/CLOVER/How+to+configure+your+clover.license)
to be used (defaults to `'${project.rootDir}/clover.license'`). The location can either be a file or URL defined as String.
* `includes`: A list of String Ant Glob Patterns to include for instrumentation (defaults to `'**/*.java'` for Java projects, defaults
to `'**/*.java'` and `'**/*.groovy'` for Groovy projects).
* `excludes`: A list of String Ant Glob Patterns to exclude for instrumentation. By default no files are excluded.
* `testIncludes`: A list of String Ant Glob Patterns to include for instrumentation for
[per-test coverage](http://confluence.atlassian.com/display/CLOVER/Unit+Test+Results+and+Per-Test+Coverage) (defaults to
`'**/*Test.java'` for Java projects, defaults to `'**/*Test.java'` and `'**/*Test.groovy'` for Groovy projects).
* `testExcludes`: A list of String Ant Glob Patterns to exclude from instrumentation for
[per-test coverage](http://confluence.atlassian.com/display/CLOVER/Unit+Test+Results+and+Per-Test+Coverage) (for example mock classes, defaults to
empty list - no excludes).
* `additionalSourceSets`: Defines custom source sets to be added for instrumentation. See example clover closure for details below.
* `additionalTestSourceSets`: Defines custom test source sets to be added for instrumentation. See example clover closure for details below.
* `targetPercentage`: The required target percentage total coverage e.g. "10%". The build fails if that goals is not met.
If not specified no target percentage will be checked.
* `optimizeTests`: If `true`, Clover will try to [optimize your tests](https://confluence.atlassian.com/display/CLOVER/About+Test+Optimization);
if `false` Clover will not try to optimize your tests. Test optimization is disabled by default. Note that Clover does not
yet fully support test optimization for Groovy code; see [CLOV-1152](https://jira.atlassian.com/browse/CLOV-1152) for more information.
* `snapshotFile`: The location of the Clover snapshot file used for test optimization, relative to the project directory.
The snapshot file should survive clean builds, so it should *not* be placed in the project's build directory. The default
location is `.clover/coverage.db.snapshot`.
* `includeTasks`: A list of task names, allows to explicitly specify which test tasks should be introspected and used to gather coverage information - useful if there are more than one `Test` tasks in a project.
* `excludeTasks`: A list of task names, allows to exclude test tasks from introspection and gathering coverage information - useful if there are more than one `Test` tasks in a project.
* `instrumentLambda`: Controls which lambda types to instrument. [Expression lambdas may cause instrumentation to crash](https://confluence.atlassian.com/cloverkb/java-8-code-instrumented-by-clover-fails-to-compile-442270815.html).
* `useClover3`: Controls the selection of the Clover 3 compatible Ant task resource file when the Clover dependency does not have a versioned artifact. Normally the correct value is determined by inspecting the version of the dependency artifact.

Within `clover` you can define [coverage contexts](http://confluence.atlassian.com/display/CLOVER/Using+Coverage+Contexts)
in a closure named `contexts`. There are two types of coverage contexts: statement contexts and method contexts. You can
define as many contexts as you want. Each of the context type closure define the following attributes:

* `name`: A unique name that identifies the context. If you want to apply the context to your report you got to use this
name as part of report `filter` attribute.
* `regexp`: The specified structure or pattern that matches a context as part of the instrumented source code. Make sure
that you correctly escape special characters.

Additionally, method contexts can be provided any of the following attributes to further configure how Clover should apply method coverage filters:

* `maxComplexity`: Match a method to this pattern if its cyclomatic complexity is not greater than maxComplexity. In other words - all methods with complexity <= maxComplexity will be filtered out.
* `maxStatements`: Match a method to this pattern if its number of statements is not greater than maxComplexity. In other words - all methods with statements <= maxStatements will be filtered out.
* `maxAggregatedComplexity`: Match a method to this pattern if its aggregated cyclomatic complexity is not greater than maxAggregatedComplexity. In other words - all methods with aggregated complexity <= maxAggregatedComplexity will be filtered out. Aggregated complexity metric is a sum of the method complexity and complexity of all anonymous inline classes declared in the method.
* `maxAggregatedStatements`: Match a method to this pattern if its number of aggregated statements is not greater than maxAggregatedStatements. In other words - all methods with aggregated statements <= maxAggregatedStaments will be filtered out. Aggregated statements metric is a sum of the method statements and statements of all anonymous inline classes declared in the method.

For more information on method contexts, see the [main Clover documentation](https://confluence.atlassian.com/display/CLOVER/methodContext) for this feature.


Within `clover` you can define which report types should be generated in a closure named `report`:

* `xml`: Generates XML report (defaults to `true`).
* `json`: Generates JSON report (defaults to `false`).
* `html`: Generates HTML report (defaults to `false`).
* `pdf`: Generates PDF report (defaults to `false`).
* `filter`: A comma or space separated list of contexts to exclude when generating coverage reports.
See [Using Coverage Contexts](http://confluence.atlassian.com/display/CLOVER/Using+Coverage+Contexts). By default no filter
is applied.
* `testResultsDir`: Specifies the location of the JUnit4 test results XML report. This is necessary when the test use the new JUnit4 @Rule mechanism to declare expected exceptions. Clover fails to detect coverage for methods when this mechanism is used. This solution uses a feature of Clover that takes the coverage information directly from the JUnit XML report.
* `testResultsInclude`: If testResultsDir is specified this must be an Ant file name pattern to select the correct XML files within the directory (defaults to TEST-*.xml).

Furthermore, within `clover` you can define compiler settings which will be passed to java and groovyc upon compilation of instrumented sources.
This is useful when specific compiler settings have been set on the main Java/Groovy compiler for your buildscript and
need to be carried over to the compilation of the instrumented sources.  These are held within a closure named  `compiler`.

* `encoding`: The (optional) encoding name.  If not provided, the platform default according to the JVM will be used. See [java.nio.charset.StandardCharsets](http://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html) for a full list of charsets.
* `executable`: The (optional) javac executable to use, should be an absolute file.
* `debug`: Controls whether to invoke javac with the -g flag. This is useful for Spring MVC code that uses debugging symbols for parameter mapping. (defaults to `false`).

The Clover plugin defines the following convention properties in the `clover` closure:

### Example

    clover {
        classesBackupDir = file("${sourceSets.main.classesDir}-backup")
        licenseLocation = 'clover-license.txt'
        excludes = ['**/SynchronizedMultiValueMap.java']
        testExcludes = ['**/Mock*.java']
        targetPercentage = '85%'

        // Each additional source set is defined by a map
        // having a srcDirs and classesDir element.
        additionalSourceSets << [
            srcDirs: sourceSets.generated.allSource.srcDirs,
            classesDir: sourceSets.generated.output.classesDir
        ]
        additionalTestSourceSets << [
            srcDirs: sourceSets.integration.allSource.srcDirs,
            classesDir: sourceSets.integration.output.classesDir
        ]

        // Closure based syntax for additionalSourceSets and
        // additionalTestSourceSets is also supported
        additionalSourceSet {
            srcDirs = sourceSets.generatedJava.allSource.srcDirs
            classesDir = sourceSets.generated.output.classesDir
        }

        compiler {
            encoding = 'UTF-8'

            // if the javac executable used by ant should be the same as the one used elsewhere.
            executable = file('/usr/local/java/jdk1.8.0_05')
        }

        contexts {
            statement {
                name = 'log'
                regexp = '^.*LOG\\..*'
            }

            method {
                name = 'main'
                regexp = 'public static void main\\(String args\\[\\]\\).*'
            }
            method {
                name = 'getters'
                regexp = 'public [^\\s]+ get[A-Z][^\\s]+\\(\\)'
                maxStatements = 1
            }
            method {
                name = 'setters'
                regexp = 'public void set[A-Z][^\\s]+\\(.+\\)'
                maxStatements = 1
            }
        }

        report {
            html = true
            pdf = true
            filter = 'log,main,getters,setters'
            testResultsDir = project.tasks.getByName('test').reports.junitXml.destination
            testResultsInclude = 'TEST-*.xml'
        }
    }

## FAQ

**How do I use the code coverage results produced by this plugin with the Gradle Sonar plugin?**

You will have to configure the [Gradle Sonar plugin](http://www.gradle.org/docs/current/userguide/sonar_plugin.html)
to parse the Clover coverage result file. The convention property [cloverReportPath](http://gradle.org/docs/current/groovydoc/org/gradle/api/plugins/sonar/model/SonarProject.html#cloverReportPath)
defines the path to the code coverage XML file. This path is only used if the property [dynamicAnalysis](http://gradle.org/docs/current/groovydoc/org/gradle/api/plugins/sonar/model/SonarProject.html#dynamicAnalysis)
has the value of `reuseReports`. The following code snippet shows an example.

    sonar {
        project {
            cloverReportPath = file("$reportsDir/clover/clover.xml")
        }
    }

In Sonar you will need to install the [Clover plugin](http://docs.codehaus.org/display/SONAR/Clover+Plugin). In order to
make Sonar use the Clover code coverage engine, the property `sonar.core.codeCoveragePlugin` must be set to `clover` under
Configuration | General Settings | Code Coverage. If you run `gradle sonarAnalyze -i` you will see that the Gradle Sonar plugin parses your file.
It should look something like this:

    Parsing /Users/ben/dev/projects/testproject/build/reports/clover/clover.xml
