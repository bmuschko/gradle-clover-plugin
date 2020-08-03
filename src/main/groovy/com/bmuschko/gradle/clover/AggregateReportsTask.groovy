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

import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

/**
 * Task for aggregrating Clover code coverage reports.
 *
 * @author Benjamin Muschko
 */
@CacheableTask
class AggregateReportsTask extends CloverReportTask {
    @Internal
    List<File> subprojectBuildDirs
    @Optional
    @Input
    String filter
    @Optional
    @Input
    String testResultsDir
    @Optional
    @Input
    String testResultsInclude

    @Override
    void generateCodeCoverage(def ant) {
        aggregateReports(ant)
    }

    private void aggregateReports(def ant) {
        logger.info 'Starting to aggregate Clover code coverage reports.'

        mergeSubprojectCloverDatabases(ant)
        writeReports(ant, getFilter(), getTestResultsDir(), getTestResultsInclude())

        logger.info 'Finished aggregating Clover code coverage reports.'
    }

    private void mergeSubprojectCloverDatabases(def ant) {
        ant.'clover-merge'(initString: "${databasePath}") {
            databasesToMerge.each { File cloverDb ->
                if(cloverDb.exists()) {
                    ant.cloverDb(initString: cloverDb.canonicalPath)
                }
                else {
                    logger.debug "Unable to find Clover DB file $cloverDb; subproject may not have any tests."
                }
            }
        }
    }

    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    Set<File> getDatabasesToMerge() {
        def databasesToMerge = getSubprojectBuildDirs().collect { dir -> new File(dir, getInitString()) } as Set
        return databasesToMerge
    }

    @OutputFile
    Provider<RegularFile> getMergedCloverDatabaseFile() {
        return project.layout.buildDirectory.file("${getInitString()}-all")
    }

    @Override
    @Internal
    File getDatabaseFile() {
        return mergedCloverDatabaseFile.get().asFile
    }
}
