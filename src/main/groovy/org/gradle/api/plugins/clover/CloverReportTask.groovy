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

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*

/**
 * Base class for Clover report tasks.
 *
 * @author Benjamin Muschko
 */
abstract class CloverReportTask extends DefaultTask {
    /**
     * Classpath for Clover Ant tasks.
     */
    @InputFiles
    FileCollection cloverClasspath

    /**
     * The location to write the Clover coverage database.
     */
    @Input
    String initString

    /**
     * Build directory.
     */
    @InputDirectory
    File buildDir

    /**
     * Directory for writing reports.
     */
    @OutputDirectory
    File reportsDir

    /**
     * Mandatory Clover license file.
     */
    @InputFile
    File licenseFile

    /**
     * Name of project used for reporting.
     */
    @Input
    String projectName

    Boolean xml
    Boolean json
    Boolean html
    Boolean pdf

    /**
     * Checks to see if at least on report type is selected.
     *
     * @return Flag
     */
    private boolean isAtLeastOneReportTypeSelected() {
        getXml() || getJson() || getHtml() || getPdf()
    }

    /**
     * Gets selected report types.
     *
     * @return Selected report types.
     */
    private List getSelectedReportTypes() {
        def selectedReportTypes = []

        if(getXml()) {
            selectedReportTypes << ReportType.XML.format
        }
        if(getJson()) {
            selectedReportTypes << ReportType.JSON.format
        }
        if(getHtml()) {
            selectedReportTypes << ReportType.HTML.format
        }
        if(getPdf()) {
            selectedReportTypes << ReportType.PDF.format
        }

        selectedReportTypes
    }

    /**
     * Validates configuration before running the task.
     */
    private void validateConfiguration() {
        if(!isAtLeastOneReportTypeSelected()) {
            throw new InvalidUserDataException("No report type selected. Please pick at least one: ${ReportType.getAllFormats()}.")
        }
        else {
            logger.info "Selected report types = ${getSelectedReportTypes()}"
        }
    }

    /**
     * Writes reports.
     *
     * @param filter Optional filter
     */
    protected void writeReports(String filter = null) {
        File cloverReportDir = new File("${getReportsDir()}/clover")

        if(getXml()) {
            writeReport(new File(cloverReportDir, 'clover.xml'), ReportType.XML, filter)
        }

        if(getJson()) {
            writeReport(new File(cloverReportDir, 'json'), ReportType.JSON, filter)
        }

        if(getHtml()) {
            writeReport(new File(cloverReportDir, 'html'), ReportType.HTML, filter)
        }

        if(getPdf()) {
            ant."clover-pdf-report"(initString: "${getBuildDir().canonicalPath}/${getInitString()}",
                    outfile: new File(cloverReportDir, 'clover.pdf'), title: getProjectName())
        }
    }

    /**
     * Writes the report with a given type.
     *
     * @param outfile Report output file
     * @param reportType Report type
     * @param filter Optional filter
     */
    private void writeReport(File outfile, ReportType reportType, String filter) {
        ant."clover-report"(initString: "${getBuildDir()}/${getInitString()}") {
            current(outfile: outfile, title: getProjectName()) {
                if(filter) {
                    format(type: reportType.format, filter: filter)
                }
                else {
                    format(type: reportType.format)
                }
            }
        }
    }

    /**
     * Initializes Clover Ant tasks.
     */
    private void initAntTasks() {
        ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
        ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
    }

    @TaskAction
    void start() {
        validateConfiguration()
        initAntTasks()
        generateCodeCoverage()
    }

    abstract void generateCodeCoverage()
}
