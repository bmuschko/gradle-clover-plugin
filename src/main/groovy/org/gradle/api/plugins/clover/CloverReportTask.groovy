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
import org.gradle.api.tasks.OutputDirectory

/**
 * Base class for Clover report tasks.
 *
 * @author Benjamin Muschko
 */
class CloverReportTask extends DefaultTask {
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
            logger.info "Selected report types = ${getSelectedReportTypes()}"
        }
    }
}
