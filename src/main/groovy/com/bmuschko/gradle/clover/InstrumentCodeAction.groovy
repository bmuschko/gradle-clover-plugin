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
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile

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
    @Input Set<CloverSourceSet> sourceSets
    @Input Set<CloverSourceSet> testSourceSets
    @InputFile File licenseFile
    @InputDirectory File buildDir
    String sourceCompatibility
    String targetCompatibility
    String executable
    String encoding
    List<String> includes
    List<String> excludes
    List<String> testIncludes
    String instrumentLambda
    def statementContexts
    def methodContexts
    boolean debug

    @Override
    void execute(Task task) {
        instrumentCode()
    }

    private boolean existsAllClassesDir() {
        for(CloverSourceSet sourceSet : getSourceSets()) {
            if(!sourceSet.classesDir.exists()) {
                return false
            }
        }

        true
    }

    void instrumentCode() {
        if(existsAllClassesDir()) {
            log.info 'Starting to instrument code using Clover.'

            def ant = new AntBuilder()
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
            ant."clover-clean"(initString: "${getBuildDir()}/${getInitString()}")

            List<File> srcDirs = CloverSourceSetUtils.getSourceDirs(getSourceSets())
            List<File> testSrcDirs = CloverSourceSetUtils.getSourceDirs(getTestSourceSets())

            ant.'clover-setup'(getCloverSetupAttributes()) {
                srcDirs.each { srcDir ->
                    ant.fileset(dir: srcDir) {
                        getIncludes().each { include ->
                            ant.include(name: include)
                        }

                        getExcludes().each { exclude ->
                            ant.exclude(name: exclude)
                        }
                    }
                }

                testSrcDirs.each { testSrcDir ->
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
                    ant.methodContext(
                        name:   it.name,
                        regexp: it.regexp,
                        maxComplexity: it.maxComplexity,
                        maxAggregatedComplexity: it.maxAggregatedComplexity,
                        maxAggregatedStatements: it.maxAggregatedStatements
                    )
                }
            }

            // Move original classes
            moveOriginalClasses(ant)
            prepareClassesDirs()

            // Compile instrumented classes
            compileClasses(ant)

            // Copy resources
            copyOriginalResources(ant)

            log.info 'Finished instrumenting code using Clover.'
        }
    }
 
    private void moveOriginalClasses(AntBuilder ant) {
        moveClassesDirsToBackupDirs(ant, getSourceSets())
        moveClassesDirsToBackupDirs(ant, getTestSourceSets())
    }
    
    private void moveClassesDirsToBackupDirs(AntBuilder ant, Set<CloverSourceSet> sourceSets) {
        sourceSets.each { sourceSet ->
            if(CloverSourceSetUtils.existsDirectory(sourceSet.classesDir)) {
                ant.move(file: sourceSet.classesDir.canonicalPath, tofile: sourceSet.backupDir.canonicalPath, failonerror: true)
            }
        }
    }

    private void prepareClassesDirs() {
        createClassesDirs(getSourceSets())
        createClassesDirs(getTestSourceSets())
    }

    private void createClassesDirs(Set<CloverSourceSet> sourceSets) {
        sourceSets.collect { it.classesDir }.each { it.mkdirs() }
    }
    
    private void copyOriginalResources(AntBuilder ant) {
        copyResourceFilesToBackupDirs(ant, getSourceSets())
        copyResourceFilesToBackupDirs(ant, getTestSourceSets())
    }
    
    private void copyResourceFilesToBackupDirs(AntBuilder ant, Set<CloverSourceSet> sourceSets) {
        sourceSets.each { sourceSet ->
            if(CloverSourceSetUtils.existsDirectory(sourceSet.backupDir)) {
                ant.copy(todir: sourceSet.classesDir.canonicalPath, failonerror: true) {
                    fileset(dir: sourceSet.backupDir.canonicalPath, excludes: '**/*.class')
                }
            }
        }
    }

    private Map getCloverSetupAttributes() {
        def attributes = [initString: "${getBuildDir()}/${getInitString()}"]

        if(!getEnabled()) {
            attributes['enabled'] = false
        }

        if(getInstrumentLambda()) {
            attributes['instrumentLambda'] = getInstrumentLambda()
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

            if(getTestSourceSets().size() > 0) {
                compileGroovyAndJavaTestSrcFiles(ant)
            }
        }
        else {
            compileJavaSrcFiles(ant)

            if(getTestSourceSets().size() > 0) {
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
        for(CloverSourceSet sourceSet : getSourceSets()) {
            compileGroovyAndJava(ant, sourceSet.srcDirs, sourceSet.classesDir, getGroovycClasspath())
        }
    }

    /**
     * Compiles Java source files.
     *
     * @param ant Ant builder
     */
    private void compileJavaSrcFiles(AntBuilder ant) {
        for(CloverSourceSet sourceSet : getSourceSets()) {
            compileJava(ant, sourceSet.srcDirs, sourceSet.classesDir, getJavacClasspath())
        }
    }

    /**
     * Compiles Groovy and Java test source files.
     *
     * @param ant Ant builder
     */
    private void compileGroovyAndJavaTestSrcFiles(AntBuilder ant) {
        for(CloverSourceSet sourceSet : getTestSourceSets()) {
            String classpath = addClassesDirToClasspath(getGroovycClasspath(), getSourceSets().collect { it.classesDir })
            compileGroovyAndJava(ant, sourceSet.srcDirs, sourceSet.classesDir, classpath)
        }
    }

    /**
     * Compiles Java test source files.
     *
     * @param ant Ant builder
     */
    private void compileJavaTestSrcFiles(AntBuilder ant) {
        for(CloverSourceSet sourceSet : getTestSourceSets()) {
            String classpath = addClassesDirToClasspath(getJavacClasspath(), getSourceSets().collect { it.classesDir })
            compileJava(ant, sourceSet.srcDirs, sourceSet.classesDir, classpath)
        }
    }

    /**
     * Adds classes directory to classpath.
     *
     * @param classpath Classpath
     * @return Classpath
     */
    private String addClassesDirToClasspath(String classpath, List<File> classesDirs) {
        StringBuilder fullClasspath = new StringBuilder()
        fullClasspath << classpath

        classesDirs.each { classesDir ->
            fullClasspath << System.getProperty('path.separator')
            fullClasspath << classesDir.canonicalPath
        }

        fullClasspath
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
        if(srcDirs.size() > 0) {
            ant.groovyc(destdir: destDir.canonicalPath, classpath: classpath) {
                srcDirs.each { srcDir ->
                    src(path: srcDir)
                }
 
            ant.javac(source: getSourceCompatibility(), target: getTargetCompatibility(), encoding: getEncoding(),
                      debug: getDebug())
            }
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
        if(srcDirs.size() > 0) {
            ant.javac(destdir: destDir.canonicalPath, source: getSourceCompatibility(), target: getTargetCompatibility(),
                  classpath: classpath, encoding: getEncoding(), executable: getExecutable(), debug: getDebug()) {
                srcDirs.each { srcDir ->
                    src(path: srcDir)
                }
            }
        }
    }

}
