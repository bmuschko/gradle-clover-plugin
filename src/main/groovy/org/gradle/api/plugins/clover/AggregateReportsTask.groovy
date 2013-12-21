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
 * Task for aggregrating Clover code coverage reports.
 *
 * @author Benjamin Muschko
 */
class AggregateReportsTask extends CloverReportTask {
    List<File> subprojectBuildDirs
    String filter

    @Override
    void generateCodeCoverage() {
        aggregateReports()
    }

    private void aggregateReports() {
        logger.info 'Starting to aggregate Clover code coverage reports.'

        mergeSubprojectCloverDatabases()
        writeReports(getFilter())

        logger.info 'Finished aggregating Clover code coverage reports.'
    }

    private void mergeSubprojectCloverDatabases() {
        ant.'clover-merge'(initString: "${project.buildDir.canonicalPath}/${getInitString()}") {
            getSubprojectBuildDirs().each { subprojectBuildDir ->
                File cloverDb = new File("$subprojectBuildDir.canonicalPath/${getInitString()}")

                if(cloverDb.exists()) {
                    ant.cloverDb(initString: cloverDb.canonicalPath)
                }
                else {
                    logger.debug "Unable to find Clover DB file $cloverDb; subproject may not have any tests."
                }
            }
        }
    }
}
