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

import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Task for generating Clover code coverage report.
 *
 * @author Benjamin Muschko
 */
@CacheableTask
class GenerateCoverageReportTask extends CloverReportTask {
    @Optional
    @Input
    String filter
    @Optional
    @Input
    String targetPercentage
    @Optional
    @Input
    String testResultsDir
    @Optional
    @Input
    String testResultsInclude

    @Override
    void generateCodeCoverage() {
        if(allowReportGeneration()) {
            generateReport()
        }
    }

    private boolean allowReportGeneration() {
        isCloverDatabaseExistent()
    }

    private boolean isCloverDatabaseExistent() {
        new File("$project.buildDir/${getInitString()}").exists()
    }

    private void generateReport() {
        logger.info 'Starting to generate Clover code coverage report.'

        String filter = getFilter()
        writeReports(filter, getTestResultsDir(), getTestResultsInclude())

        File cloverXml = new File("${getReportsDir()}/clover/clover.xml")
        if (cloverXml.canRead()) {
            showConsoleCoverage(cloverXml)
        }

        checkTargetPercentage(filter)

        logger.info 'Finished generating Clover code coverage report.'
    }

    // Crude but effective way to integrate a console coverage display
    private void showConsoleCoverage(File cloverXml) {
        def coverage = new XmlSlurper().parse(cloverXml)

        logCoverage('Project ' + coverage.project.@name, coverage.project.metrics)
        logCoverage('Project ' + coverage.project.@name + ' test', coverage.testproject.metrics)
    }

    private void logCoverage(String heading, groovy.util.slurpersupport.NodeChildren metrics) {
        logger.quiet '{} classes coverage', heading
        logger.quiet 'Files: {} Packages: {} Classes: {} LOC: {} NCLOC: {}', metrics.@files, metrics.@packages, metrics.@classes, metrics.@loc, metrics.@ncloc
        logger.quiet 'Methods coverage {}', computePercentage(metrics.@coveredmethods?.toString(), metrics.@methods?.toString())
        logger.quiet 'Elements coverage {}', computePercentage(metrics.@coveredelements?.toString(), metrics.@elements?.toString())
        logger.quiet 'Statements coverage {}', computePercentage(metrics.@coveredstatements?.toString(), metrics.@statements?.toString())
        logger.quiet 'Conditionals coverage {}', computePercentage(metrics.@coveredconditionals?.toString(), metrics.@conditionals?.toString())
        logger.quiet ''
    }

    private String computePercentage(String covered, String total) {
        try {
            if (covered && total && total != '0') {
                Double percent = Double.valueOf(covered) * 100.0 / Double.valueOf(total)
                return String.format('%.2f', percent)
            }
            return 'N/A'
        } catch (NumberFormatException e) {
            logger.warn 'Caught exception with {} and {}', covered, total, e
            return 'N/A'
        }
    }

    private void checkTargetPercentage(String filter) {
        if(getTargetPercentage()) {
            Map arguments = [
                initString: "$project.buildDir/${getInitString()}",
                target: getTargetPercentage(),
                haltOnFailure: true
            ]

            if(filter) {
                arguments['filter'] = filter
            }

            ant."clover-check"(arguments)
        }
    }
}
