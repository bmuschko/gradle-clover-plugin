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

/**
 * Task for generating Clover code coverage report.
 *
 * @author Benjamin Muschko
 */
class GenerateCoverageReportTask extends CloverReportTask {
    String filter
    String targetPercentage

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

        writeReports(getFilter())
        checkTargetPercentage()

        logger.info 'Finished generating Clover code coverage report.'
    }

    private void checkTargetPercentage() {
        if(getTargetPercentage()) {
            ant."clover-check"(initString: "$project.buildDir/${getInitString()}", target: getTargetPercentage(), haltOnFailure: true)
        }
    }
}
