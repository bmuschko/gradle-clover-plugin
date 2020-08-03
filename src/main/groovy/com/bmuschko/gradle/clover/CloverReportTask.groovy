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
package com.bmuschko.gradle.clover

import javax.inject.Inject

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.IsolatedAntBuilder
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
    @Classpath
    FileCollection cloverClasspath

    /**
     * The location to write the Clover coverage database.
     */
    @Input
    String initString

    /**
     * Directory for writing reports.
     */
    @Internal
    File reportsDir

    // Current report parameters
    @Input
    Boolean alwaysReport
    @Input
    Boolean includeFailedTestCoverage
    @Input
    Integer numThreads
    @Input
    String timeoutInterval

    // Enabled report types.
    @Input
    Boolean xml
    @Input
    Boolean json
    @Input
    Boolean html
    @Input
    Boolean pdf
    @Input
    Boolean historical

    @Input
    Collection<String> additionalColumns

    /**
     * Optional Clover history directory.
     */
    @Internal
    File historyDir

    // Historical report parameters.
    @Optional
    @Input
    String historyIncludes
    @Optional
    @Input
    String packageFilter
    @Optional
    @Input
    String from
    @Optional
    @Input
    String to
    @Optional
    @Input
    String added
    @Optional
    @Input
    Collection<String> movers

    @OutputDirectory
    @Optional
    File getHistoryDirOrNull() {
        return historical ? historyDir : null
    }

    @OutputDirectory
    File getCloverReportsDir() {
        return new File( "${getReportsDir()}/clover")
    }

    @Inject
    IsolatedAntBuilder getAntBuilder() {
        throw new UnsupportedOperationException();
    }

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
    protected void writeReports(def ant, String filter, String testResultsDir = null, String testResultsInclude = null) {
        File cloverReportDir = getCloverReportsDir()

        if (getHistorical()) {
            createHistoryPoint(ant, filter, testResultsDir, testResultsInclude)
        }

        if(getXml()) {
            writeReport(ant, new File(cloverReportDir, 'clover.xml'), ReportType.XML, filter, testResultsDir, testResultsInclude)
        }

        if(getJson()) {
            writeReport(ant, new File(cloverReportDir, 'json'), ReportType.JSON, filter, testResultsDir, testResultsInclude)
        }

        if(getHtml()) {
            writeReport(ant, new File(cloverReportDir, 'html'), ReportType.HTML, filter, testResultsDir, testResultsInclude)
        }

        if(getPdf()) {
            writeReport(ant, new File(cloverReportDir, 'clover.pdf'), ReportType.PDF, filter, testResultsDir, testResultsInclude)
        }
    }

    private void createHistoryPoint(def ant, String filter, String testResultsDir, String testResultsInclude) {
        logger.info 'Starting to create a Clover history point in ${getHistoryDir()}.'

        ant."clover-historypoint"(initString: "${databasePath}", historyDir: getHistoryDir(), overwrite: 'true') {
            if (testResultsDir) {
                testresults(dir: testResultsDir, includes: testResultsInclude)
            }
        }

        logger.info 'Finished creating a Clover history point.'
    }

    /**
     * Writes the report with a given type.
     *
     * @param outfile Report output file
     * @param reportType Report type
     * @param filter Optional filter
     */
    private void writeReport(def ant, File outfile, ReportType reportType, String filter, String testResultsDir, String testResultsInclude) {
        ant."clover-report"(initString: "${databasePath}") {
            def params = [
                outfile: outfile,
                title: project.name,
                alwaysReport: alwaysReport,
                includeFailedTestCoverage: includeFailedTestCoverage,
                numThreads: numThreads,
            ]
            if (timeoutInterval) {
                params.timeout = timeoutInterval
            }
            if (reportType == ReportType.PDF)
                params.summary = 'true'
            def formatParams = [ type: reportType.format ]
            if (filter) {
                formatParams.filter = filter
            }

            current(params) {
                format(formatParams)
                if (testResultsDir) {
                    testresults(dir: testResultsDir, includes: testResultsInclude)
                }
                if (getAdditionalColumns()) {
                    columns {
                        for (String jsonString in getAdditionalColumns()) {
                            CloverReportColumn col = CloverReportColumn.fromJson(jsonString)
                            String name = col.getColumn()
                            "$name"(col.getAttributes())
                        }
                    }
                }
            }

            // Historical report is supported only for HTML and PDF reports
            if (getHistorical() && (reportType == ReportType.HTML || reportType == ReportType.PDF)) {
                if (reportType == ReportType.PDF) {
                    outfile = new File(outfile.parentFile, 'historical.pdf')
                }
                def historyParams = [ outfile: outfile, title: project.name, historyDir: getHistoryDir(), historyIncludes: getHistoryIncludes() ]
                if (getPackageFilter()) {
                    historyParams.packageFilter = getPackageFilter()
                }
                if (getFrom()) {
                    historyParams.from = getFrom()
                }
                if (getTo()) {
                    historyParams.to = getTo()
                }

                historical(historyParams) {
                    format(formatParams)
                    overview()
                    coverage()
                    metrics()

                    if (getAdded()) {
                        HistoricalAdded fromJson = HistoricalAdded.fromJson(getAdded())
                        fromJson.with {
                            added(range: range, interval: interval)
                        }
                    }
                    for (String jsonString : getMovers()) {
                        HistoricalMover mover = HistoricalMover.fromJson(jsonString)
                        mover.with {
                            movers(threshold: "${threshold}%", range: range, interval: interval)
                        }
                    }
                }
            }
        }
    }

    @Internal
    protected String getDatabasePath() {
        return databaseFile.canonicalFile
    }

    @TaskAction
    void start() {
        validateConfiguration()
        antBuilder.withClasspath(getCloverClasspath().files).execute {
            CloverUtils.injectCloverClasspath(ant.getBuilder(), getCloverClasspath().files)
            CloverUtils.loadCloverlib(ant.getBuilder())

            generateCodeCoverage(ant)
        }
    }

    abstract void generateCodeCoverage(def ant)

    @Internal
    abstract File getDatabaseFile()
}
