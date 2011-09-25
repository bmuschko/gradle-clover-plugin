package org.gradle.api.plugins.clover

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory

/**
 * Created by IntelliJ IDEA.
 * User: Ben
 * Date: 9/24/11
 * Time: 7:30 PM
 * To change this template use File | Settings | File Templates.
 */
class ReportTask extends ConventionTask {
    @OutputDirectory File reportsDir
    Boolean xml
    Boolean json
    Boolean html
    Boolean pdf
    String projectName

    boolean isAtLeastOneReportTypeSelected() {
        getXml() || getJson() || getHtml() || getPdf()
    }
}
