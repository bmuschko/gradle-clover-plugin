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
import org.gradle.api.tasks.OutputDirectory

/**
 * Clover code instrumentation action.
 *
 * @author Benjamin Muschko
 */
@Slf4j
class InstrumentCodeAction implements Action<Task> {
    String initString
    Boolean enabled
    Boolean compileGroovy
    FileCollection cloverClasspath
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
    String executable
    String encoding
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
            log.info 'Starting to instrument code using Clover.'

            def ant = new AntBuilder()
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
            ant."clover-clean"(initString: "${getBuildDir()}/${getInitString()}")

            ant.'clover-setup'(getCloverSetupAttributes()) {
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
                    ant.fileset(dir: testSrcDir) {
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
            ant.move(file: getClassesDir().canonicalPath, tofile: getClassesBackupDir().canonicalPath, failonerror: true)
            ant.move(file: getTestClassesDir().canonicalPath, tofile: getTestClassesBackupDir().canonicalPath, failonerror: false)

            // Compile instrumented classes
            getClassesDir().mkdirs()
            getTestClassesDir().mkdirs()
            compileClasses(ant)

            // Copy resources
            ant.copy(todir: getClassesDir().canonicalPath, failonerror: true) {
                fileset(dir: getClassesBackupDir().canonicalPath, excludes: '**/*.class')
            }
            ant.copy(todir: getTestClassesDir().canonicalPath, failonerror: false) {
                fileset(dir: getTestClassesBackupDir().canonicalPath, excludes: '**/*.class')
            }

            log.info 'Finished instrumenting code using Clover.'
        }
    }

    private Map getCloverSetupAttributes() {
        def attributes = [initString: "${getBuildDir()}/${getInitString()}"]

        if(!getEnabled()) {
            attributes['enabled'] = false
        }

        attributes
    }

    /**
     * Compiles Java classes. If project has Groovy plugin applied run the joint compiler.
     *
     * @param ant Ant builder
     */
    private void compileClasses(AntBuilder ant) {
        if(getCompileGroovy()) {
            ant.taskdef(name: 'groovyc', classname: 'org.codehaus.groovy.ant.Groovyc', classpath: getGroovycClasspath())
            compileGroovyAndJavaSrcFiles(ant)

            if(getTestSrcDirs().size() > 0) {
                compileGroovyAndJavaTestSrcFiles(ant)
            }
        }
        else {
            compileJavaSrcFiles(ant)

            if(getTestSrcDirs().size() > 0) {
                compileJavaTestSrcFiles(ant)
            }
        }
    }

    /**
     * Gets Groovyc classpath. Make sure the Groovy version defined in project is used on classpath first to avoid using the
     * default version bundled with Gradle.
     *
     * @return Classpath
     */
    private String getGroovycClasspath() {
        getGroovyClasspath().asPath + System.getProperty('path.separator') + getTestRuntimeClasspath().asPath
    }

    /**
     * Gets Javac classpath.
     *
     * @return Classpath
     */
    private String getJavacClasspath() {
        getTestRuntimeClasspath().asPath
    }

    /**
     * Compiles Groovy and Java source files.
     *
     * @param ant Ant builder
     */
    private void compileGroovyAndJavaSrcFiles(AntBuilder ant) {
        compileGroovyAndJava(ant, getSrcDirs(), getClassesDir(), getGroovycClasspath())
    }

    /**
     * Compiles Java source files.
     *
     * @param ant Ant builder
     */
    private void compileJavaSrcFiles(AntBuilder ant) {
        compileJava(ant, getSrcDirs(), getClassesDir(), getJavacClasspath())
    }

    /**
     * Compiles Groovy and Java test source files.
     *
     * @param ant Ant builder
     */
    private void compileGroovyAndJavaTestSrcFiles(AntBuilder ant) {
        String classpath = addClassesDirToClasspath(getGroovycClasspath())
        compileGroovyAndJava(ant, getTestSrcDirs(), getTestClassesDir(), classpath)
    }

    /**
     * Compiles Java test source files.
     *
     * @param ant Ant builder
     */
    private void compileJavaTestSrcFiles(AntBuilder ant) {
        String classpath = addClassesDirToClasspath(getJavacClasspath())
        compileJava(ant, getTestSrcDirs(), getTestClassesDir(), classpath)
    }

    /**
     * Adds classes directory to classpath.
     *
     * @param classpath Classpath
     * @return Classpath
     */
    private String addClassesDirToClasspath(String classpath) {
        classpath + System.getProperty('path.separator') + getClassesDir().canonicalPath
    }

    /**
     * Compiles given Groovy and Java source files to destination directory.
     *
     * @param ant Ant builder
     * @param srcDirs Source directories
     * @param destDir Destination directory
     * @param classpath Classpath
     */
    private void compileGroovyAndJava(AntBuilder ant, Set<File> srcDirs, File destDir, String classpath) {
        ant.groovyc(destdir: destDir.canonicalPath, classpath: classpath) {
            srcDirs.each { srcDir ->
                src(path: srcDir)
            }

            ant.javac(source: getSourceCompatibility(), target: getTargetCompatibility(), encoding: getEncoding(),
                      executable: getExecutable())
        }
    }

    /**
     * Compiles given Java source files to destination directory.
     *
     * @param ant Ant builder
     * @param srcDirs Source directories
     * @param destDir Destination directory
     * @param classpath Classpath
     */
    private void compileJava(AntBuilder ant, Set<File> srcDirs, File destDir, String classpath) {
        ant.javac(destdir: destDir.canonicalPath, source: getSourceCompatibility(), target: getTargetCompatibility(),
                  classpath: classpath, encoding: getEncoding(), executable: getExecutable()) {
            srcDirs.each { srcDir ->
                src(path: srcDir)
            }
        }
    }
}
