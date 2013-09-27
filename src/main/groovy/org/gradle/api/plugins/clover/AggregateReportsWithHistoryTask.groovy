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

import java.io.File;
import java.util.List;

import groovy.util.logging.Slf4j

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction

@Slf4j
class AggregateReportsWithHistoryTask extends AggregateReportsTask {
	
	@OutputDirectory File historyDir
	
	@Override
    void generateCodeCoverage() {
		aggregateReportsWithHistory()
    }
	
	private void aggregateReportsWithHistory() {
		logger.info 'Starting to aggregate Clover code coverage reports with history.'

		mergeSubprojectCloverDatabases()
		createHistoryPoint()
		writeReports()

		logger.info 'Finished aggregating Clover code coverage reports with history.'
	}
	
	private void createHistoryPoint() {
		log.info 'Starting to create a Clover history point to ${getHistoryDir()}.'
		
		ant."clover-historypoint"(initString: "$project.buildDir/${getInitString()}", historyDir: getHistoryDir())
		
		log.info 'Finished creating a Clover history point.'
	}
	
	/**
	 * Writes reports.
	 *
	 * @param filter Optional filter
	 */
	@Override
	protected void writeReports(String filter = null) {
		File cloverReportDir = new File("${getReportsDir()}/clover")
		
		if(getXml()) {
			log.info "Format 'xml' is not allowed for historical reports. Reporting without historical data."
			writeReport(new File(cloverReportDir, 'clover.xml'), ReportType.XML, filter)
		}

		if(getJson()) {
			log.warn "Format 'json' is not allowed for historical reports. Reporting without historical data."
			writeReport(new File(cloverReportDir, 'json'), ReportType.JSON, filter)
		}
		
		if(getHtml()) {
			writeHistoricalReport(new File(cloverReportDir, 'html'), ReportType.HTML, filter)
		}

		if(getPdf()) {
			log.warn "Format 'pdf' is not allowed for historical reports. Reporting without historical data."
			// note: 'pdf' would be allowed as well in the ant task, but is not implemented in this plugin
			ant."clover-pdf-report"(initString: "${project.buildDir.canonicalPath}/${getInitString()}",
					outfile: new File(cloverReportDir, 'clover.pdf'), title: project.name)
		}
	}
	
    private void writeHistoricalReport(File outfile, ReportType type, String filter) {
        ant."clover-report"(initString: "$project.buildDir/${getInitString()}") {
            current(outfile: outfile, title: project.name) {
                format(type: type)
            }
			if (type == ReportType.HTML.format) {
				historical(outfile: outfile, title: project.name, historyDir: getHistoryDir()) {
					format(type: type)
					overview()
					added(range: 20, interval: "4 week")
					movers(range: 20, interval: "4 weeks")
					coverage()
					metrics()
				}
			} else {
				log.info 'Format '+type+' is not allowed for historical reports. Allowed formats are: html.'
			}
        }
    }
}