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

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

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
    @Input List<CloverSourceSet> sourceSets
    @Input List<CloverSourceSet> testSourceSets
    @InputDirectory File buildDir
    String sourceCompatibility
    String targetCompatibility
    String executable
    String encoding
    List<String> includes
    List<String> excludes
    List<String> testIncludes
    List<String> testExcludes
    String instrumentLambda
    def statementContexts
    def methodContexts
    boolean debug
    int flushinterval
    String flushpolicy
    String additionalArgs

    @Override
    void execute(Task task) {
        instrumentCode()
    }

    private boolean existsAllClassesDir() {
        for (CloverSourceSet sourceSet : getSourceSets()) {
            if (!sourceSet.srcDirs.empty && !sourceSet.classesDir.exists()) {
                return false
            }
        }

        true
    }

    void instrumentCode() {
        if (existsAllClassesDir()) {
            log.info 'Starting to instrument code using Clover.'

            def ant = new AntBuilder()
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
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

                        getTestExcludes().each { exclude ->
                            ant.exclude(name: exclude)
                        }
                    }
                }

                // Apply statement and method coverage contexts
                getStatementContexts().each {
                    ant.statementContext(name: it.name, regexp: it.regexp)
                }

                getMethodContexts().each {
                    def args = [ name: it.name, regexp: it.regexp ]
                    // Add optional method metrics if provided
                    if (it.maxComplexity != null)
                        args.maxComplexity = it.maxComplexity
                    if (it.maxStatements != null)
                        args.maxStatements = it.maxStatements
                    if (it.maxAggregatedComplexity != null)
                        args.maxAggregatedComplexity = it.maxAggregatedComplexity
                    if (it.maxAggregatedStatements != null)
                        args.maxAggregatedStatements = it.maxAggregatedStatements
                    ant.methodContext(args)
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
    
    private Map getCloverSetupAttributes() {
        def attributes = [initString: "${getBuildDir()}/${getInitString()}"]

        if (!getEnabled()) {
            attributes['enabled'] = 'false'
        }

        if(getInstrumentLambda()) {
            attributes['instrumentLambda'] = getInstrumentLambda()
        }

        attributes.flushinterval = getFlushinterval()
        attributes.flushpolicy = getFlushpolicy()

        attributes.encoding = getEncoding()
        
        attributes
    }

    @CompileStatic
    private void moveOriginalClasses(AntBuilder ant) {
        moveClassesDirsToBackupDirs(ant, getSourceSets())
        moveClassesDirsToBackupDirs(ant, getTestSourceSets())
    }

    private void moveClassesDirsToBackupDirs(AntBuilder ant, List<CloverSourceSet> sourceSets) {
        sourceSets.each { sourceSet ->
            if (CloverSourceSetUtils.existsDirectory(sourceSet.classesDir)) {
                ant.move(file: sourceSet.classesDir.canonicalPath, tofile: sourceSet.backupDir.canonicalPath, failonerror: true)
            }
        }
    }

    @CompileStatic
    private void prepareClassesDirs() {
        createClassesDirs(getSourceSets())
        createClassesDirs(getTestSourceSets())
    }

    @CompileStatic
    private void createClassesDirs(List<CloverSourceSet> sourceSets) {
        sourceSets.each { it.classesDir.mkdirs() }
    }

    @CompileStatic
    private void copyOriginalResources(AntBuilder ant) {
        copyResourceFilesToBackupDirs(ant, getSourceSets())
        copyResourceFilesToBackupDirs(ant, getTestSourceSets())
    }

    private void copyResourceFilesToBackupDirs(AntBuilder ant, List<CloverSourceSet> sourceSets) {
        sourceSets.each { sourceSet ->
            if (CloverSourceSetUtils.existsDirectory(sourceSet.backupDir)) {
                ant.copy(todir: sourceSet.classesDir.canonicalPath, failonerror: true) {
                    fileset(dir: sourceSet.backupDir.canonicalPath, excludes: '**/*.class')
                }
            }
        }
    }

    /**
     * Compiles Java classes. If project has Groovy plugin applied run the joint compiler.
     *
     * @param ant Ant builder
     */
    private void compileClasses(AntBuilder ant) {
        if (getCompileGroovy()) {
            ant.taskdef(name: 'groovyc', classname: 'org.codehaus.groovy.ant.Groovyc', classpath: getGroovycClasspath())
        }
        compileSrcFiles(ant)
        compileTestSrcFiles(ant)
    }

    /**
     * Gets Groovyc classpath.
     *
     * @return Classpath
     */
    @CompileStatic
    private String getGroovycClasspath() {
        getGroovyClasspath().asPath + System.getProperty('path.separator') + getTestRuntimeClasspath().asPath
    }

    /**
     * Gets the compile classpath for the given sourceSet.
     * 
     * @param sourceSet the CloverSourceSet to extract the classpath
     *
     * @return Classpath
     */
    @CompileStatic
    private String getCompileClasspath(CloverSourceSet sourceSet) {
        sourceSet.getCompileClasspath().asPath
    }

    /**
     * Compiles main source files.
     *
     * @param ant Ant builder
     */
    @CompileStatic
    private void compileSrcFiles(AntBuilder ant) {
        for(CloverSourceSet sourceSet : getSourceSets()) {
            String classpath = getCompileClasspath(sourceSet)
            if (sourceSet.groovy) {
                compileGroovyAndJava(ant, sourceSet.srcDirs, sourceSet.classesDir, classpath)
            } else {
                compileJava(ant, sourceSet.srcDirs, sourceSet.classesDir, classpath)
            }
        }
    }

    /**
     * Compiles test source files.
     *
     * @param ant Ant builder
     */
    @CompileStatic
    private void compileTestSrcFiles(AntBuilder ant) {
        def nonTestClasses = getSourceSets().collect { it.classesDir }
        for(CloverSourceSet sourceSet : getTestSourceSets()) {
            String classpath = addClassesDirToClasspath(getCompileClasspath(sourceSet), nonTestClasses)
            if (sourceSet.groovy) {
                compileGroovyAndJava(ant, sourceSet.srcDirs, sourceSet.classesDir, classpath)
            } else {
                compileJava(ant, sourceSet.srcDirs, sourceSet.classesDir, classpath)
            }
        }
    }

    /**
     * Adds classes directory to classpath.
     *
     * @param classpath Classpath
     * @return Classpath
     */
    @CompileStatic
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
    private void compileGroovyAndJava(AntBuilder ant, Collection<File> srcDirs, File destDir, String classpath) {
        if (srcDirs.size() > 0) {
            String args = getAdditionalArgs()
            ant.groovyc(destdir: destDir.canonicalPath, classpath: classpath, encoding: getEncoding()) {
                srcDirs.each { srcDir ->
                    src(path: srcDir)
                }

                ant.javac(source: getSourceCompatibility(), target: getTargetCompatibility(), encoding: getEncoding(),
                          debug: getDebug()) {
                    if (args != null && args.length() > 0) {
                        compilerarg(line: args)
                    }
                }
            }
            addMarkerFile(destDir)
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
    private void compileJava(AntBuilder ant, Collection<File> srcDirs, File destDir, String classpath) {
        if (srcDirs.size() > 0) {
            String args = getAdditionalArgs()
            ant.javac(destdir: destDir.canonicalPath, source: getSourceCompatibility(), target: getTargetCompatibility(),
                  classpath: classpath, encoding: getEncoding(), executable: getExecutable(), debug: getDebug()) {
                srcDirs.each { srcDir ->
                    src(path: srcDir)
                }
                if (args != null && args.length() > 0) {
                    compilerarg(line: args)
                }
            }
            addMarkerFile(destDir)
        }
    }

    /**
     * Adds a marker file to the destination directory.
     */
    @CompileStatic
    private void addMarkerFile(File destDir) {
        File marker = new File(destDir, "clover.instrumented")
        marker << "the classes in this directory are instrumented with clover"
    }

}
