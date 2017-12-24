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

The plugin provides generation of code coverage reports using [OpenClover](http://openclover.org/).

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
            classpath 'com.bmuschko:gradle-clover-plugin:2.2.0'
        }
    }

To define the Clover dependency please use the `clover` configuration name in your `dependencies` closure. If you are working
with a multi-module project make sure you apply the plugin and declare the `clover` dependency within the `allprojects` closure.

    dependencies {
        clover 'org.openclover:clover:4.2.0'
    }

With the introduction of the OpenClover support the `licenseLocation` clover convention is
now unused. For compatibility with existing clover Gradle configurations the plugin will accept a value set but will do nothing with it.

## Tasks

The Clover plugin defines the following tasks:

* `cloverGenerateReport`: Generates Clover code coverage report.
* `cloverAggregateReports`: Aggregate Clover code coverage reports in a multi-module project setup. This task can only be
run from the root directory of your project and requires at least one submodule. This task depends on `cloverGenerateReport`.

## Convention properties

* `initString`: The location to write the Clover coverage database (defaults to `.clover/clover.db`). The location you
define will always be relative to the project's build directory.
* `enabled`: Controls whether Clover will instrument code during code compilation (defaults to `true`).
* `classesBackupDir`: *Deprecated - this is not used anymore*
* `licenseLocation`: *Deprecated - this is not used anymore*
* `includes`: A list of String Ant Glob Patterns to include for instrumentation (defaults to `'**/*.java'` for Java projects, defaults
to `'**/*.java'` and `'**/*.groovy'` for Groovy projects).
* `excludes`: A list of String Ant Glob Patterns to exclude for instrumentation. By default no files are excluded.
* `testIncludes`: A list of String Ant Glob Patterns to include for instrumentation for
[per-test coverage](http://confluence.atlassian.com/display/CLOVER/Unit+Test+Results+and+Per-Test+Coverage) (defaults to
`'**/*Test.java'` for Java projects, defaults to `'**/*Test.java,**/*Test.groovy,**/*Spec.groovy'` for Groovy and Grails3 projects).
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
* `useClover3`: *Deprecated - this is not used anymore*
* `flushpolicy`: This String attribute controls how Clover flushes coverage data during a test run. Valid values are directed, interval, or threaded. [clover-setup Parameters](https://confluence.atlassian.com/clover/clover-setup-71600085.html#clover-setup-parametersParameters)
* `flushinterval`: When the flushpolicy is set to interval or threaded this value is the minimum period between flush operations (in milliseconds). [clover-setup Parameters](https://confluence.atlassian.com/clover/clover-setup-71600085.html#clover-setup-parametersParameters)

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

Within `report` closure you can define a closure named `columns` to enable selection of columns for the report output. This feature implements support for the columns defined in Clover documentation [Clover ReportComumns Nested Element](https://confluence.atlassian.com/clover/clover-report-71600095.html#clover-report-Columns). Each line in the closure must begin with the name of the column to add followed by a Groovy map with the 4 optional attributes for the column. We support `format`, `min`, `max` and `scope`. The format and scope values are checked against the documented supported contents and will throw errors if unsupported values are used. We do not implement support for the `expression` column at this time, if you attempt to use it the plugin will throw an error:

* `format`: Determines how the value is rendered. Depending on the column, this may be one of raw, bar, % or longbar.
* `min`: Sets a minimum threshold on the value of the column. If the value is less than this it will be highlighted.
* `max`: Sets a maximum threshold on the value of the column. If the value is greater than this it will be highlighted.
* `scope`: Controls at which level in the report the column will appear. The scope attribute can be one of: "package", "class" or "method". If omitted, the column will be used at every level in the report. Note that only the following columns support the scope attribute: expression, complexity, complexityDensity, coveredXXX, uncoveredXXX and totalXXX.


Within `report` closure you can define a closure named `historical` to enable Clover historical reports generation:

* `enabled`: Enables generation of historical reports (defaults to `false`)
* `historyIncludes`: An Ant GLOB to select specific history point files within the historyDir directory (defaults to `clover-*.xml.gz`)
* `packageFilter`: Restricts the report to a particular package (optional, see `package` attribute of Clover historical element)
* `from`: Specifies the date before which data points will be ignored (optional, see `from` attribute of Clover historical element)
* `to`: Specifies the date after which data points will be ignored (optional, see `to` attribute of Clover historical element)
* `added`: Closure to support nested `added` element from Clover `historical` element. Only the `range` and `interval` attributes are supported (optional, see `added` nested element of Clover historical element)
* `mover`: Closure that can appear multiple times to support the `movers` element from Clover `historical` element. Only the `threshold`, `range` and `interval` attributes are supported (optional, see `movers` nested element of Clover historical element)
See [Clover Report Historical Nested Element](https://confluence.atlassian.com/clover/clover-report-71600095.html#clover-report-<historical>)

Furthermore, within `clover` you can define compiler settings which will be passed to java and groovyc upon compilation of instrumented sources.
This is useful when specific compiler settings have been set on the main Java/Groovy compiler for your buildscript and
need to be carried over to the compilation of the instrumented sources.  These are held within a closure named  `compiler`.

* `encoding`: The (optional) encoding name.  If not provided, the platform default according to the JVM will be used. See [java.nio.charset.StandardCharsets](http://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html) for a full list of charsets.
* `executable`: The (optional) javac executable to use, should be an absolute file.
* `debug`: Controls whether to invoke javac with the -g flag. This is useful for Spring MVC code that uses reflection for parameter mapping. (defaults to `false`).
* `additionalArgs`: The (optional) additional command line arguments for the compiler. This is useful for Spring MVC code that uses reflection for parameter mapping. This should be valid command line arguments as a spaces separated string. No attempt is made to validate this line, it is passed verbatim to the <compilerarg> nested element for the Ant `javac` task.

The Clover plugin defines the following convention properties in the `clover` closure:

### Example

    clover {
        excludes = ['**/SynchronizedMultiValueMap.java']

        // This is the default testIncludes configuration
        testIncludes = ['**/*Test.java', '**/*Spec.groovy']
        testExcludes = ['**/Mock*.java']

        targetPercentage = '85%'

        // Closure based syntax for additionalSourceSets and
        // additionalTestSourceSets is also supported. Both
        // srcDirs and classesDir properties are required.
        // The syntax allows the following to occur as many times
        // as necessary to define each additional sourceSet.
        // From version 3.0.0 and later the configuration is
        // requiring the Gradle 4.0 outputDir for each language
        // in the sourceSet. If you have Java and Groovy sourceSets
        // you may need to specify each language in the sourceSet
        // separately.
        additionalSourceSet {
            srcDirs = sourceSets.generatedCode.java.srcDirs
            classesDir = sourceSets.generatedCode.java.outputDir
        }
        additionalSourceSet {
            srcDirs = sourceSets.generatedCode.groovy.srcDirs
            classesDir = sourceSets.generatedCode.groovy.outputDir
        }
        additionalTestSourceSet {
            srcDirs = sourceSets.integrationTest.java.srcDirs,
            classesDir = sourceSets.integrationTest.java.outputDir
        }
        additionalTestSourceSet {
            srcDirs = sourceSets.integrationTest.groovy.srcDirs,
            classesDir = sourceSets.integrationTest.groovy.outputDir
        }

        compiler {
            encoding = 'UTF-8'

            // if the javac executable used by ant should be the same as the one used elsewhere.
            executable = file('/usr/local/java/jdk1.8.0_05')
            // used to add debug information for Spring applications
            debug = true
            additionalArgs = '-parameters'
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

            // Support capturing test results from JUnix XML report
            testResultsDir = project.tasks.getByName('test').reports.junitXml.destination
            testResultsInclude = 'TEST-*.xml'

            // Clover report nested columns support
            columns {
                coveredMethods format: 'longbar', min: '75'
                coveredStatements format: '%'
                coveredBranches format: 'raw'
                totalPercentageCovered format: '%', scope: 'package'
            }

            // Clover history generation support
            // See Clover documentation for details of the values supported
            historical {
                enabled = true
                historyIncludes = 'clover-*.xml.gz'
                packageFilter = null
                from = null
                to = null

                added {
                    range = 10
                    interval = '3 weeks'
                }
                mover {
                    threshold = 1
                    range = 10
                    interval = '3 weeks'
                }
                mover {
                    threshold = 1
                    range = 10
                    interval = '3 months'
                }
                mover {
                    threshold = 1
                    range = 10
                    interval = '1 year'
                }
            }
        }
    }

## Project Properties

The Clover plugin uses the following properties:
* `-PcloverInstrumentedJar`: This property can be used to prepare a JAR or EAR for distributed code coverage. When using this property the instrumented classes are left in the `classes` directory so that the `jar` tasks will bundle them. The property causes the `jar` tasks to execute after the `test` tasks to ensure that the Clover instrumentation has happened. This property should be used with a separate test execution in a Continuous Integration environment because the JAR files will be created with Clover instrumented code which cannot be used in a production environment.
