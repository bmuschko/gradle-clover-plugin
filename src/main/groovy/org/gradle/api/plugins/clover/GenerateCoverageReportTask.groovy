/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.plugins.clover

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * Task for generating Clover code coverage report.
 *
 * @author Benjamin Muschko
 */
class GenerateCoverageReportTask extends CloverReportTask {
    String initString
    File buildDir
    File classesDir
    File testClassesDir
    File classesBackupDir
    File testClassesBackupDir
    FileCollection cloverClasspath
    @InputFile File licenseFile
    String filter
    String targetPercentage

    @TaskAction
    void start() {
        validateConfiguration()

        if(allowReportGeneration()) {
            generateReport()
        }
    }

    private boolean allowReportGeneration() {
        isCloverDatabaseExistent() && existsClassesDir() && existsClassesBackupDir()
    }

    private boolean existsClassesDir() {
        getClassesDir() && getClassesDir().exists()
    }

    private boolean existsClassesBackupDir() {
        getClassesBackupDir() && getClassesBackupDir().exists()
    }

    private boolean isCloverDatabaseExistent() {
        new File("${getBuildDir()}/${getInitString()}").exists()
    }

    private void generateReport() {
        logger.info 'Starting to generate Clover code coverage report.'

        ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
        ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)

        restoreOriginalClasses()
        String cloverReportDir = "${getReportsDir()}/clover"

        if(getXml()) {
            writeReport("$cloverReportDir/clover.xml", ReportType.XML.format)
        }

        if(getJson()) {
            writeReport("$cloverReportDir/json", ReportType.JSON.format)
        }

        if(getHtml()) {
            writeReport("$cloverReportDir/html", ReportType.HTML.format)
        }

        if(getPdf()) {
            ant."clover-pdf-report"(initString: "${getBuildDir()}/${getInitString()}", outfile: "$cloverReportDir/clover.pdf",
                                    title: getProjectName())
        }

        if(getTargetPercentage()) {
            ant."clover-check"(initString: "${getBuildDir()}/${getInitString()}", target: getTargetPercentage(), haltOnFailure: true)
        }

        logger.info 'Finished generating Clover code coverage report.'
    }

    private void restoreOriginalClasses() {
        ant.delete(includeEmptyDirs: true) {
            fileset(dir: getClassesDir().canonicalPath, includes: '**/*')
        }
        ant.delete(includeEmptyDirs: true) {
            fileset(dir: getTestClassesDir().canonicalPath, includes: '**/*')
        }
        ant.move(file: getClassesBackupDir().canonicalPath, tofile: getClassesDir().canonicalPath, failonerror: true)
        ant.move(file: getTestClassesBackupDir().canonicalPath, tofile: getTestClassesDir().canonicalPath, failonerror: false)
    }

    private void writeReport(String outfile, String type) {
        ant."clover-report"(initString: "${getBuildDir()}/${getInitString()}") {
            current(outfile: outfile, title: getProjectName()) {
                if(getFilter()) {
                    format(type: type, filter: getFilter())
                }
                else {
                    format(type: type)
                }
            }
        }
    }
}
