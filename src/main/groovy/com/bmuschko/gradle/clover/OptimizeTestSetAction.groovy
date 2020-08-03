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
import org.gradle.api.file.FileTreeElement
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional

import groovy.util.logging.Slf4j

/**
 * Action which optimizes the test set, based on information collected by Clover about previous test runs.
 *
 * @see https://confluence.atlassian.com/display/CLOVER/About+Test+Optimization
 * @see https://confluence.atlassian.com/display/CLOVER/Test+Optimization+Quick+Start+Guide
 * @see CreateSnapshotAction
 * @author Daniel Gredler
 */
@Slf4j
class OptimizeTestSetAction implements Action<Task>, Spec<FileTreeElement> {
    @Input String initString
    @Input boolean optimizeTests
    @Classpath FileCollection cloverClasspath
    @Optional @InputFile File snapshotFile
    @Internal File buildDir
    @Input List<CloverSourceSet> testSourceSets
    
    @Internal Set<String> includes
    
    @Override
    void execute(Task task) {
        initIncludes(task)
    }

    void initIncludes(Task task) {
        if (getOptimizeTests() && getSnapshotFile() != null && getSnapshotFile().exists()) {
            log.info 'Optimizing test set.'

            // This cannot use the isolated classloader and I am giving up on it.
            AntBuilder ant = new AntBuilder()
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
            ant.property(name: 'clover.initstring', value: "${getBuildDir()}/${getInitString()}")
            List<File> testSrcDirs = CloverSourceSetUtils.getValidSourceDirs(getTestSourceSets())
            def testset = ant."clover-optimized-testset"(snapshotFile: getSnapshotFile(), debug: true) {
                testSrcDirs.each { testSrcDir ->
                    ant.fileset(dir: testSrcDir)
                }
            }

            // The Clover optimizer operates in terms of source files (*.java, *.groovy), but Gradle's test config operates in terms of classes (*.class)
            includes = testset.collect { fileResource ->
                fileResource.name.replace('.java', '.class').replace('.groovy', '.class')
            }

            log.info 'Finished optimizing test set.'
        }
    }

    @Override
    boolean isSatisfiedBy(FileTreeElement element) {
        if (includes != null && !element.directory) {
            def match = includes.find {
                element.file.path.endsWith(it)
            }
            return match != null
        } else {
            return true
        }
    }
}
