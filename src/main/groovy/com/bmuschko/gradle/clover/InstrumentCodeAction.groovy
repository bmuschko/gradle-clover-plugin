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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional

import com.bmuschko.gradle.clover.internal.AntResourceWorkaround

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * Clover code instrumentation action.
 *
 * @author Benjamin Muschko
 */
@Slf4j
class InstrumentCodeAction implements Action<Task> {
    @Input String initString
    @Input Boolean enabled
    @Input Boolean compileGroovy
    @Classpath FileCollection cloverClasspath
    @Classpath FileCollection instrumentationClasspath
    @Classpath FileCollection groovyClasspath
    @Nested List<CloverSourceSet> sourceSets
    @Nested List<CloverSourceSet> testSourceSets
    @Internal File buildDir
    @Input @Optional String sourceCompatibility
    @Input @Optional String targetCompatibility
    @Input @Optional String executable
    @Input @Optional String encoding
    @Input @Optional List<String> includes
    @Input @Optional List<String> excludes
    @Input @Optional List<String> testIncludes
    @Input @Optional List<String> testExcludes
    @Input @Optional String instrumentLambda
    @Nested Set<CloverContextConvention> statementContexts
    @Nested Set<CloverContextConvention> methodContexts
    @Input boolean debug
    @Internal int flushinterval
    @Input @Optional String flushpolicy
    @Input @Optional String additionalArgs
    @Input @Optional Map additionalGroovycOpts

    @Inject
    IsolatedAntBuilder getAntBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    void execute(Task task) {
        instrumentCode(task)
    }

    private boolean existsAllClassesDir() {
        for (CloverSourceSet sourceSet : getSourceSets()) {
            if (!sourceSet.srcDirs.empty && !sourceSet.classesDir.exists()) {
                return false
            }
        }

        true
    }

    void instrumentCode(Task task) {
        log.info 'Starting to instrument code using Clover.'

        antBuilder.withClasspath(getClasspath().files).execute {
            CloverUtils.injectCloverClasspath(ant.getBuilder(), getCloverClasspath().files)
            CloverUtils.loadCloverlib(ant.getBuilder())

            ant."clover-clean"(initString: "${getBuildDir()}/${getInitString()}")
    
            List<File> srcDirs = CloverSourceSetUtils.getValidSourceDirs(getSourceSets())
            List<File> testSrcDirs = CloverSourceSetUtils.getValidSourceDirs(getTestSourceSets())
    
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
    
            // Compile instrumented classes
            compileClasses(ant)
        }

        log.info 'Finished instrumenting code using Clover.'
    }

    @Internal
    File getCloverDatabaseFile() {
        return new File("${getBuildDir()}/${getInitString()}")
    }

    @Internal
    Map getCloverSetupAttributes() {
        def attributes = [initString: "${cloverDatabaseFile}"]

        if (getSourceCompatibility()) {
            attributes['source'] = getSourceCompatibility()
        }

        if (!getEnabled()) {
            attributes['enabled'] = 'false'
        }

        if (getInstrumentLambda()) {
            attributes['instrumentLambda'] = getInstrumentLambda()
        }

        attributes.flushinterval = getFlushinterval()
        attributes.flushpolicy = getFlushpolicy()

        attributes.encoding = getEncoding()

        attributes
    }

    /**
     * Compiles Java classes. If project has Groovy plugin applied run the joint compiler.
     *
     * @param ant Ant builder
     */
    void compileClasses(def ant) {
        if (getCompileGroovy()) {
             ant.taskdef(name: 'groovyc', classname: 'org.codehaus.groovy.ant.Groovyc')
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
    private FileCollection getClasspath() {
        FileCollection classpath = getCloverClasspath()
        classpath = classpath.plus(getInstrumentationClasspath())
        if (getCompileGroovy()) {
            classpath = classpath.plus(getGroovyClasspath())
        }
        classpath
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
    private void compileSrcFiles(def ant) {
        for(CloverSourceSet sourceSet : getSourceSets()) {
            String classpath = getCompileClasspath(sourceSet)
            if (sourceSet.groovy) {
                compileGroovyAndJava(ant, CloverSourceSetUtils.getValidSourceDirs(sourceSet), sourceSet.instrumentedClassesDir, classpath)
            } else {
                compileJava(ant, CloverSourceSetUtils.getValidSourceDirs(sourceSet), sourceSet.instrumentedClassesDir, classpath)
            }
        }
    }

    /**
     * Compiles test source files.
     *
     * @param ant Ant builder
     */
    @CompileDynamic
    private void compileTestSrcFiles(def ant) {
        def nonTestClasses = getSourceSets().collect { it.classesDir }
        for(CloverSourceSet sourceSet : getTestSourceSets()) {
            String classpath = addClassesDirToClasspath(getCompileClasspath(sourceSet), nonTestClasses)
            if (sourceSet.groovy) {
                compileGroovyAndJava(ant, CloverSourceSetUtils.getValidSourceDirs(sourceSet), sourceSet.instrumentedClassesDir, classpath)
            } else {
                compileJava(ant, CloverSourceSetUtils.getValidSourceDirs(sourceSet), sourceSet.instrumentedClassesDir, classpath)
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
    private void compileGroovyAndJava(def ant, Collection<File> srcDirs, File destDir, String classpath) {
        if (srcDirs.size() > 0) {
            String args = getAdditionalArgs()
            Map groovycAttrs = [destdir: destDir.canonicalPath, classpath: classpath, encoding: getEncoding()] + (getAdditionalGroovycOpts() ?: [:])
            ant.groovyc(groovycAttrs) {
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
    private void compileJava(def ant, Collection<File> srcDirs, File destDir, String classpath) {
        if (srcDirs.size() > 0) {
            String args = getAdditionalArgs()
            ant.javac(destdir: destDir.canonicalPath, source: getSourceCompatibility(), target: getTargetCompatibility(),
                  includeantruntime: false, classpath: classpath, encoding: getEncoding(), executable: getExecutable(), debug: getDebug()) {
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
