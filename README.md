# Gradle Clover plugin

![Clover Logo](https://www.appfusions.com/download/attachments/131128/LOGO_Clover_dark.png)

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
            classpath 'bmuschko:gradle-clover-plugin:0.1'
        }
    }

    dependencies {
        testRuntime 'com.cenqua.clover:clover:3.1.0'
    }

## Tasks

The Clover plugin defines the following tasks:

* `cloverGenerateReport`: Generates Clover code coverage report.

## Convention properties

* `instrSrcDir`: The directory that is used to write the instrumented source files to (defaults to `file("$buildDir/instrSrc")`).
* `classesBackupDir`: The temporary backup directory for classes (defaults to `file("${sourceSets.main.classesDir}-bak")`).
* `licenseFile`: The [Clover license file](http://confluence.atlassian.com/display/CLOVER/How+to+configure+your+clover.license)
to be used (defaults to `file('clover.license')`).

Within `clover` you can define which report types should be generated in a closure named `report`:

* `xml`: Generates XML report (defaults to `true`).
* `html`: Generates HTML report (defaults to `false`).
* `pdf`: Generates PDF report (defaults to `true`).

The Clover plugin defines the following convention properties in the `clover` closure:

### Example

    clover {
        instrSrcDir = file("$buildDir/instrumentationSrc")
        classesBackupDir = file("${sourceSets.main.classesDir}-backup")
        licenseFile = file('clover-license.txt')

        report {
            html = true
            pdf = true
        }
    }