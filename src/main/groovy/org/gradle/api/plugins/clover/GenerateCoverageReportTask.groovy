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
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Task for generating Clover code coverage report.
 *
 * @author Benjamin Muschko
 */
class GenerateCoverageReportTask extends ReportTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCoverageReportTask)
    String initString
    @InputDirectory File buildDir
    File classesDir
    File classesBackupDir
    FileCollection classpath
    @InputFile File licenseFile
    String filter
    String targetPercentage

    @TaskAction
    void start() {
        if(allowReportGeneration()) {
            LOGGER.info 'Starting to generate Clover code coverage report.'

            // Restore original classes
            ant.taskdef(resource: 'cloverlib.xml', classpath: getClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
            ant.delete(file: getClassesDir().canonicalPath)
            ant.move(file: getClassesBackupDir().canonicalPath, tofile: getClassesDir().canonicalPath)
            String cloverReportDir = "${getReportsDir()}/clover"

            if(getXml()) {
                writeReport("$cloverReportDir/clover.xml", 'xml')
            }

            if(getJson()) {
                writeReport("$cloverReportDir/json", 'json')
            }

            if(getHtml()) {
                writeReport("$cloverReportDir/html", 'html')
            }

            if(getPdf()) {
                ant."clover-pdf-report"(initString: "${getBuildDir()}/${getInitString()}", outfile: "$cloverReportDir/clover.pdf",
                                        title: getProjectName())
            }

            LOGGER.info 'Finished generating Clover code coverage report.'
        }
    }

    private boolean allowReportGeneration() {
        isCloverDatabaseExistent() && getClassesDir().exists() && getClassesBackupDir().exists() && isAtLeastOneReportTypeSelected()
    }

    private boolean isCloverDatabaseExistent() {
        new File("${getBuildDir()}/${getInitString()}").exists()
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
