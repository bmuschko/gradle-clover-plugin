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
    String initString
    Boolean compileGroovy
    FileCollection testRuntimeClasspath
    FileCollection groovyClasspath
    @OutputDirectory File classesBackupDir
    @OutputDirectory File testClassesBackupDir
    @InputFile File licenseFile
    @InputDirectory File buildDir
    @InputDirectory File classesDir
    @InputDirectory File testClassesDir
    Set<File> srcDirs
    Set<File> testSrcDirs
    String sourceCompatibility
    String targetCompatibility
    List<String> includes
    List<String> excludes
    List<String> testIncludes
    def statementContexts
    def methodContexts

    @Override
    void execute(Task task) {
        instrumentCode()
    }

    void instrumentCode() {
        if(getClassesDir().exists()) {
            LOGGER.info 'Starting to instrument code using Clover.'

            def ant = new AntBuilder()
            ant.taskdef(resource: 'cloverlib.xml', classpath: getTestRuntimeClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
            ant."clover-clean"(initString: "${getBuildDir()}/${getInitString()}")

            ant.'clover-setup'(initString: "${getBuildDir()}/${getInitString()}") {
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

                getTestSrcDirs().each { testSrcDir ->
                    ant.testsources(dir: testSrcDir) {
                        getTestIncludes().each { include ->
                            ant.include(name: include)
                        }
                    }
                }

                // Apply statement and method coverage contexts
                getStatementContexts().each {
                    ant.statementContext(name: it.name, regexp: it.regexp)
                }

                getMethodContexts().each {
                    ant.methodContext(name: it.name, regexp: it.regexp)
                }
            }

            // Move original classes
            ant.move(file: getClassesDir().canonicalPath, tofile: getClassesBackupDir().canonicalPath)
            ant.move(file: getTestClassesDir().canonicalPath, tofile: getTestClassesBackupDir().canonicalPath, failonerror: false)

            // Compile instrumented classes
            getClassesDir().mkdirs()
            getTestClassesDir().mkdirs()
            compileClasses(ant, getSrcDirs(), getClassesDir())

            if(getTestSrcDirs().size() > 0) {
                compileClasses(ant, getTestSrcDirs(), getTestClassesDir(), getClassesDir().canonicalPath)
            }

            // Copy resources
            ant.copy(todir: getClassesDir().canonicalPath) {
                fileset(dir: getClassesBackupDir().canonicalPath, excludes: '**/*.class')
            }
            ant.copy(todir: getTestClassesDir().canonicalPath, failonerror: false) {
                fileset(dir: getTestClassesBackupDir().canonicalPath, excludes: '**/*.class')
            }

            LOGGER.info 'Finished instrumenting code using Clover.'
        }
    }

    /**
     * Compiles Java and Groovy classes.
     *
     * @param ant Ant Builder
     */
    private void compileClasses(AntBuilder ant, Set<File> srcDirs, File destDir, String additionalClasspath = null) {
        if(getCompileGroovy()) {
            // Make sure the Groovy version define in project is used on classpath to avoid using the default Gradle version
            def groovycClasspath = getGroovyClasspath().asPath + System.getProperty('path.separator') + getTestRuntimeClasspath().asPath

            if(additionalClasspath) {
                groovycClasspath += System.getProperty('path.separator') + additionalClasspath
            }

            ant.taskdef(name: 'groovyc', classname: 'org.codehaus.groovy.ant.Groovyc', classpath: getGroovyClasspath().asPath)

            ant.groovyc(destdir: destDir.canonicalPath, classpath: groovycClasspath) {
                srcDirs.each { srcDir ->
                    src(path: srcDir)
                }

                ant.javac(source: getSourceCompatibility(), target: getTargetCompatibility())
            }
        }
        else {
            def classpath = getTestRuntimeClasspath().asPath

            if(additionalClasspath) {
                classpath += System.getProperty('path.separator') + additionalClasspath
            }

            ant.javac(destdir: destDir.canonicalPath, source: getSourceCompatibility(), target: getTargetCompatibility(),
                      classpath: classpath) {
                srcDirs.each { srcDir ->
                    src(path: srcDir)
                }
            }
        }
    }
}
