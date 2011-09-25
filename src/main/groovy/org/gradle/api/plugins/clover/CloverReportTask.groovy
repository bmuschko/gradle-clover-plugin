package org.gradle.api.plugins.clover

import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.OutputDirectory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * User: Ben
 * Date: 9/24/11
 * Time: 7:30 PM
 * To change this template use File | Settings | File Templates.
 */
class CloverReportTask extends ConventionTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloverReportTask)
    @OutputDirectory File reportsDir
    Boolean xml
    Boolean json
    Boolean html
    Boolean pdf
    String projectName

    /**
     * Checks to see if at least on report type is selected.
     *
     * @return Flag
     */
    boolean isAtLeastOneReportTypeSelected() {
        getXml() || getJson() || getHtml() || getPdf()
    }

    /**
     * Gets selected report types.
     *
     * @return Selected report types.
     */
    List getSelectedReportTypes() {
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
    void validateConfiguration() {
        if(!isAtLeastOneReportTypeSelected()) {
            throw new InvalidUserDataException("No report type selected. Please pick at least one: ${ReportType.getAllFormats()}.")
        }
        else {
            LOGGER.info "Selected report types = ${getSelectedReportTypes()}"
        }
    }
}
