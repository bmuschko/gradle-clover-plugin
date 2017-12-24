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

import groovy.util.logging.Slf4j
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Optional

/**
 * Create code coverage snapshot action.
 *
 * @see https://confluence.atlassian.com/display/CLOVER/About+Test+Optimization
 * @see https://confluence.atlassian.com/display/CLOVER/Test+Optimization+Quick+Start+Guide
 * @see OptimizeTestSetAction
 * @author Daniel Gredler
 */
@Slf4j
class CreateSnapshotAction implements Action<Task> {
    String initString
    boolean optimizeTests
    FileCollection cloverClasspath
    @InputDirectory File buildDir
    @OutputFile File snapshotFile

    @Override
    void execute(Task task) {
        createSnapshot()
    }

    void createSnapshot() {
        if(getOptimizeTests()) {
            log.info 'Creating Clover snapshot.'

            def ant = new AntBuilder()
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
            ant."clover-snapshot"(initString: "${getBuildDir()}/${getInitString()}", file: getSnapshotFile())

            log.info 'Finished creating Clover snapshot.'
        }
    }
}
