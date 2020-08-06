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

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

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
    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    FileCollection coverageDatabaseFiles

    @Override
    void generateCodeCoverage(def ant) {
        if (allowReportGeneration()) {
            generateReport(ant)
        }
    }

    private boolean allowReportGeneration() {
        isCloverDatabaseExistent()
    }

    private boolean isCloverDatabaseExistent() {
        databaseFile.exists()
    }

    private void generateReport(def ant) {
        logger.info 'Starting to generate Clover code coverage report.'

        writeReports(ant, getFilter(), getTestResultsDir(), getTestResultsInclude())

        File cloverXml = new File("${getCloverReportsDir()}/clover.xml")
        if (cloverXml.canRead()) {
            showConsoleCoverage(cloverXml)
        }

        checkTargetPercentage(ant)

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

    private void checkTargetPercentage(def ant) {
        if (getTargetPercentage()) {
            Map arguments = [
                initString: "${databasePath}",
                target: getTargetPercentage(),
                haltOnFailure: true
            ]

            if (getFilter()) {
                arguments['filter'] = getFilter()
            }

            ant."clover-check"(arguments)
        }
    }

    @Override
    @Internal
    File getDatabaseFile() {
        return project.layout.buildDirectory.file(getInitString()).get().asFile
    }
}
