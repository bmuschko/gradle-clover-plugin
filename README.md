# Gradle Clover plugin

![Clover Logo](http://www.atlassian.com/software/clover/images/badges/v2/code_coverage_md.png)

The plugin provides generation of code coverage reports using [Clover](http://www.atlassian.com/software/clover/).

## Usage

To use the Clover plugin, include in your build script:

    apply plugin: 'clover'

The plugin JAR needs to be defined in the classpath of your build script. You can either get the plugin from the GitHub download
section or upload it to your local repository. To define the Clover dependency please use the `testRuntime`
configuration name in your `dependencies` closure.

    buildscript {
        repositories {
            add(new org.apache.ivy.plugins.resolver.URLResolver()) {
                name = 'GitHub'
                addArtifactPattern 'http://cloud.github.com/downloads/[organisation]/[module]/[module]-[revision].[ext]'
            }
        }

        dependencies {
            classpath 'bmuschko:gradle-clover-plugin:0.2'
        }
    }

    dependencies {
        testRuntime 'com.cenqua.clover:clover:3.1.0'
    }

## Tasks

The Clover plugin defines the following tasks:

* `cloverGenerateReport`: Generates Clover code coverage report.

## Convention properties

* `classesBackupDir`: The temporary backup directory for classes (defaults to `file("${sourceSets.main.classesDir}-bak")`).
* `licenseFile`: The [Clover license file](http://confluence.atlassian.com/display/CLOVER/How+to+configure+your+clover.license)
to be used (defaults to `file('clover.license')`).
* `includes`: A list of String Ant Glob Patterns to include for instrumentation (defaults to `'**/*.java'` for Java projects, defaults
to `'**/*.java'` and `'**/*.groovy'` for Groovy projects).
* `excludes`: A list of String Ant Glob Patterns to exclude for instrumentation. By default no files are excluded.
* `targetPercentage`: The required target percentage total coverage e.g. "10%". The build fails if that goals is not met.
If not specified no target percentage will be checked.

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

The Clover plugin defines the following convention properties in the `clover` closure:

### Example

    clover {
        classesBackupDir = file("${sourceSets.main.classesDir}-backup")
        licenseFile = file('clover-license.txt')
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

        report {
            html = true
            pdf = true
            filter = 'log,if,else'
        }
    }