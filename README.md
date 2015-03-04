# Gradle Clover plugin

![Clover Logo](https://www.appfusions.com/download/attachments/13959273/logoClover.png?version=1&modificationDate=1353815596170)

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
            classpath 'com.bmuschko:gradle-clover-plugin:2.0.1'
        }
    }

To define the Clover dependency please use the `clover` configuration name in your `dependencies` closure. If you are working
with a multi-module project make sure you apply the plugin and declare the `clover` dependency within the `allprojects` closure.

    dependencies {
        clover 'com.cenqua.clover:clover:3.2.0'
    }

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
to be used (defaults to `'clover.license'`). The location can either be a file or URL defined as String.
* `includes`: A list of String Ant Glob Patterns to include for instrumentation (defaults to `'**/*.java'` for Java projects, defaults
to `'**/*.java'` and `'**/*.groovy'` for Groovy projects).
* `excludes`: A list of String Ant Glob Patterns to exclude for instrumentation. By default no files are excluded.
* `testIncludes`: A list of String Ant Glob Patterns to include for instrumentation for
[per-test coverage](http://confluence.atlassian.com/display/CLOVER/Unit+Test+Results+and+Per-Test+Coverage) (defaults to
`'**/*Test.java'` for Java projects, defaults to `'**/*Test.java'` and `'**/*Test.groovy'` for Groovy projects).
* `additionalSourceDirs`: Defines custom source sets to be added for instrumentation e.g. `sourceSets.custom.allSource.srcDirs`.
* `additionalTestDirs`: Defines custom test source sets to be added for instrumentation e.g. `sourceSets.integTest.allSource.srcDirs`.
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
* `debug`: Controls whether to invoke javac with the -g flag. This is useful for Spring MVC code that uses debugging symbols for parameter mapping. (defaults to `false`).

Within `clover` you can define [coverage contexts](http://confluence.atlassian.com/display/CLOVER/Using+Coverage+Contexts)
in a closure named `contexts`. There are two types of coverage contexts: statement contexts and method contexts. You can
define as many contexts as you want. Each of the context type closure define the following attributes:

* `name`: A unique name that identifies the context. If you want to apply the context to your report you got to use this
name as part of report `filter` attribute.
* `regexp`: The specified structure or pattern that matches a context as part of the instrumented source code. Make sure
that you correctly escape special characters.

Within `clover` you can define which report types should be generated in a closure named `report`:

* `xml`: Generates XML report (defaults to `true`).
* `json`: Generates JSON report (defaults to `false`).
* `html`: Generates HTML report (defaults to `false`).
* `pdf`: Generates PDF report (defaults to `false`).
* `filter`: A comma or space separated list of contexts to exclude when generating coverage reports.
See [Using Coverage Contexts](http://confluence.atlassian.com/display/CLOVER/Using+Coverage+Contexts). By default no filter
is applied.

Furthermore, within `clover` you can define compiler settings which will be passed to java and groovyc upon compilation of instrumented sources.
This is useful when specific compiler settings have been set on the main Java/Groovy compiler for your buildscript and
need to be carried over to the compilation of the instrumented sources.  These are held within a closure named  `compiler`.

* `encoding`: The (optional) encoding name.  If not provided, the platform default according to the JVM will be used. See [java.nio.charset.StandardCharsets](http://docs.oracle.com/javase/8/docs/api/java/nio/charset/StandardCharsets.html) for a full list of charsets.
* `executable`: The (optional) javac executable to use, should be an absolute file.

The Clover plugin defines the following convention properties in the `clover` closure:

### Example

    clover {
        classesBackupDir = file("${sourceSets.main.classesDir}-backup")
        licenseLocation = 'clover-license.txt'
        excludes = ['**/SynchronizedMultiValueMap.java']
        targetPercentage = '85%'

        contexts {
            statement {
                name = 'log'
                regexp = '^.*LOG\\..*'
            }

            method {
                name = 'main'
                regexp = 'public static void main\\(String args\\[\\]\\).*'
            }
        }
        
        compiler {
            encoding = 'UTF-8'
            
            // if the javac executable used by ant should be the same as the one used elsewhere.
            executable = file('/usr/local/java/jdk1.8.0_05') 
        }

        report {
            html = true
            pdf = true
            filter = 'log,if,else'
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
