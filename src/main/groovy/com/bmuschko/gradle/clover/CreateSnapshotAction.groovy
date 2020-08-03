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

import javax.inject.Inject

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile

import groovy.util.logging.Slf4j

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
    @Input String initString
    @Input boolean optimizeTests
    @Classpath FileCollection cloverClasspath
    @Internal File buildDir
    @OutputFile File snapshotFile
    
    @Inject
    IsolatedAntBuilder getAntBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    void execute(Task task) {
        createSnapshot()
    }

    void createSnapshot() {
        if (getOptimizeTests()) {
            log.info 'Creating Clover snapshot.'

            antBuilder.withClasspath(getCloverClasspath().files).execute {
                CloverUtils.injectCloverClasspath(ant.getBuilder(), getCloverClasspath().files)
                CloverUtils.loadCloverlib(ant.getBuilder())

                ant."clover-snapshot"(initString: "${getBuildDir()}/${getInitString()}", file: getSnapshotFile())
            }

            log.info 'Finished creating Clover snapshot.'
        }
    }
}
