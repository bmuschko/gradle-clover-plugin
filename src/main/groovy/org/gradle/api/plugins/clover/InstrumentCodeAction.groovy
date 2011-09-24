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

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Clover code instrumentation action.
 *
 * @author Benjamin Muschko
 */
class InstrumentCodeAction implements Action<Task> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InstrumentCodeAction)
    Boolean compileGroovy
    FileCollection classpath
    @OutputDirectory File classesBackupDir
    @InputFile File licenseFile
    @InputDirectory File classesDir
    Set<File> srcDirs
    String sourceCompatibility
    String targetCompatibility
    List<String> includes
    List<String> excludes

    @Override
    void execute(Task task) {
        instrumentCode()
    }

    void instrumentCode() {
        if(getClassesDir().exists()) {
            LOGGER.info 'Starting to instrument code using Clover.'

            def ant = new AntBuilder()
            ant.taskdef(resource: 'cloverlib.xml', classpath: getClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
            ant."clover-clean"()

            ant.'clover-setup'() {
                getSrcDirs().each { srcDir ->
                    ant.fileset(dir: srcDir) {
                        getIncludes().each { include ->
                            ant.include(name: include)
                        }

                        getExcludes().each { exclude ->
                            ant.exclude(name: exclude)
                        }
                    }
                }
            }

            // Move original classes
            ant.move(file: getClassesDir().canonicalPath, tofile: getClassesBackupDir().canonicalPath)

            // Compile instrumented classes
            getClassesDir().mkdirs()

            if(getCompileGroovy()) {
                ant.groovyc(destdir: getClassesDir().canonicalPath, classpath: getClasspath().asPath) {
                    compileJava(ant)
                }
            }
            else {
                compileJava(ant)
            }

            // Copy resources
            ant.copy(todir: getClassesDir().canonicalPath) {
                fileset(dir: getClassesBackupDir().canonicalPath, excludes: '**/*.class')
            }

            LOGGER.info 'Finished instrumenting code using Clover.'
        }
    }

    /**
     * Compiles Java sources.
     */
    private void compileJava(AntBuilder ant) {
        ant.javac(destdir: getClassesDir().canonicalPath, source: getSourceCompatibility(), target: getTargetCompatibility(),
                  includeAntRuntime: false, classpath: getClasspath().asPath) {
            getSrcDirs().each { srcDir ->
                src(path: srcDir)
            }
        }
    }
}
