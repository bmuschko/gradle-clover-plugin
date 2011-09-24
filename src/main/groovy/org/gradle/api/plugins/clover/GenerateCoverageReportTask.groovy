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
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Task for generating Clover code coverage report.
 *
 * @author Benjamin Muschko
 */
class GenerateCoverageReportTask extends ConventionTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(GenerateCoverageReportTask)
    @InputDirectory File classesDir
    File classesBackupDir
    @OutputDirectory File reportsDir
    FileCollection classpath
    @InputFile File licenseFile
    Boolean xml
    Boolean json
    Boolean html
    Boolean pdf
    String projectName
    String targetPercentage

    @TaskAction
    void start() {
        if(getClassesBackupDir().exists()) {
            LOGGER.info 'Starting to generate Clover code coverage report.'

            // Restore original classes
            ant.taskdef(resource: 'cloverlib.xml', classpath: getClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
            ant.delete(file: getClassesDir().canonicalPath)
            ant.move(file: getClassesBackupDir().canonicalPath, tofile: getClassesDir().canonicalPath)
            String cloverReportDir = "${getReportsDir()}/clover"

            if(getXml()) {
                ant."clover-report" {
                    current(outfile: "$cloverReportDir/clover.xml", title: getProjectName())
                }
            }

            if(getJson()) {
                ant."clover-report" {
                    current(outfile: "$cloverReportDir/json", title: getProjectName()) {
                        format(type: 'json')
                    }
                }
            }

            if(getHtml()) {
                ant."clover-html-report"(outdir: "$cloverReportDir/html", title: getProjectName())
            }

            if(getPdf()) {
                ant."clover-pdf-report"(outfile: "$cloverReportDir/clover.pdf", title: getProjectName())
            }

            LOGGER.info 'Finished generating Clover code coverage report.'
        }
    }
}
