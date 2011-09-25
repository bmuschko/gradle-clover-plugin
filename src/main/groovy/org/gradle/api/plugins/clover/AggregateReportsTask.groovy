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
 * Task for aggregrating Clover code coverage reports.
 *
 * @author Benjamin Muschko
 */
class AggregateReportsTask extends CloverReportTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(AggregateReportsTask)
    String initString
    FileCollection testRuntimeClasspath
    @InputFile File licenseFile
    @InputDirectory File buildDir
    List<File> subprojectBuildDirs

    @TaskAction
    void start() {
        validateConfiguration()
        aggregateReports()
    }

    private void aggregateReports() {
        LOGGER.info 'Starting to aggregate Clover code coverage reports.'

        ant.taskdef(resource: 'cloverlib.xml', classpath: getTestRuntimeClasspath().asPath)
        ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)

        ant.'clover-merge'(initString: "${getBuildDir().canonicalPath}/${getInitString()}") {
            getSubprojectBuildDirs().each { subprojectBuildDir ->
                File cloverDb = new File("$subprojectBuildDir.canonicalPath/${getInitString()}")

                // Some subproject might not have any tests
                if(cloverDb.exists()) {
                    ant.cloverDb(initString: cloverDb.canonicalPath)
                }
            }
        }

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
            ant."clover-pdf-report"(initString: "${getBuildDir().canonicalPath}/${getInitString()}",
                                    outfile: "$cloverReportDir/clover.pdf", title: getProjectName())
        }

        LOGGER.info 'Finished aggregating Clover code coverage reports.'
    }

    private void writeReport(String outfile, String type) {
        ant."clover-report"(initString: "${getBuildDir().canonicalPath}/${getInitString()}") {
            current(outfile: outfile, title: getProjectName()) {
                format(type: type)
            }
        }
    }
}
