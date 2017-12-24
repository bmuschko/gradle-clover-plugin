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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.AsmBackedClassGenerator
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

import java.lang.reflect.Constructor
import java.util.concurrent.Callable

/**
 * <p>A {@link org.gradle.api.Plugin} that provides a task for creating a code coverage report using Clover.</p>
 *
 * @author Benjamin Muschko
 */
@Slf4j
class CloverPlugin implements Plugin<Project> {
    static final String CONFIGURATION_NAME = 'clover'
    static final String GENERATE_REPORT_TASK_NAME = 'cloverGenerateReport'
    static final String AGGREGATE_REPORTS_TASK_NAME = 'cloverAggregateReports'
    static final String AGGREGATE_DATABASES_TASK_NAME = 'cloverAggregateDatabases'
    static final String REPORT_GROUP = 'report'
    static final String CLOVER_GROUP = 'clover'
    static final String DEFAULT_JAVA_INCLUDES = '**/*.java'
    static final String DEFAULT_GROOVY_INCLUDES = '**/*.groovy'
    static final String DEFAULT_JAVA_TEST_INCLUDES = '**/*Test.java'
    static final String DEFAULT_GROOVY_TEST_INCLUDES = '**/*Test.groovy'
    static final String DEFAULT_SPOCK_TEST_INCLUDES = '**/*Spec.groovy'
    static final String DEFAULT_CLOVER_DATABASE = '.clover/clover.db'
    static final String DEFAULT_CLOVER_SNAPSHOT = '.clover/coverage.db.snapshot'
    static final String DEFAULT_CLOVER_HISTORY_DIR = '.clover/historypoints'

    @CompileStatic
    @Override
    void apply(Project project) {
        project.configurations.create(CONFIGURATION_NAME).setVisible(false).setTransitive(true)
                .setDescription('The Clover library to be used for this project.')

        CloverPluginConvention cloverPluginConvention = new CloverPluginConvention()
        project.convention.plugins.clover = cloverPluginConvention

        AggregateDatabasesTask aggregateDatabasesTask = configureAggregateDatabasesTask(project, cloverPluginConvention)
        configureActions(project, cloverPluginConvention, aggregateDatabasesTask)
        configureGenerateCoverageReportTask(project, cloverPluginConvention, aggregateDatabasesTask)
        configureAggregateReportsTask(project, cloverPluginConvention)
    }

    private AggregateDatabasesTask configureAggregateDatabasesTask(Project project, CloverPluginConvention cloverPluginConvention) {
        project.tasks.withType(AggregateDatabasesTask) {
            conventionMapping.with {
                map('initString') { getInitString(cloverPluginConvention) }
                map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            }
        }

        AggregateDatabasesTask aggregateDatabasesTask = project.tasks.create(AGGREGATE_DATABASES_TASK_NAME, AggregateDatabasesTask)
        aggregateDatabasesTask.description = 'Aggregates Clover code coverage databases for the project.'
        aggregateDatabasesTask.group = CLOVER_GROUP
        aggregateDatabasesTask
    }

    @CompileStatic
    private void configureActions(Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        SourceSetsResolver resolver = new SourceSetsResolver(project, cloverPluginConvention)

        project.tasks.withType(Test) { Test test ->
            // If it is too late for afterEvaluate configure now
            if (project.state.executed) {
                configureActionsForTask(test, project, cloverPluginConvention, resolver, aggregateDatabasesTask)
            } else {
                project.afterEvaluate {
                    configureActionsForTask(test, project, cloverPluginConvention, resolver, aggregateDatabasesTask)
                }
            }
            // If we are generating instrumented JAR files make sure the Jar
            // tasks run after the Test tasks so that the instrumented classes
            // get packaged in the archives.
            if (project.hasProperty('cloverInstrumentedJar')) {
                project.tasks.withType(Jar) { Jar jar ->
                    jar.mustRunAfter test
                }
            }
        }
    }

    @CompileStatic
    private boolean testTaskEnabled(Test test, CloverPluginConvention cloverPluginConvention) {
        !((cloverPluginConvention.includeTasks && !(test.name in cloverPluginConvention.includeTasks)) || test.name in cloverPluginConvention.excludeTasks)
    }

    @CompileStatic
    private void configureActionsForTask(Test test, Project project, CloverPluginConvention cloverPluginConvention, SourceSetsResolver resolver, AggregateDatabasesTask aggregateDatabasesTask) {
        if (testTaskEnabled(test, cloverPluginConvention)) {
            test.classpath += project.configurations.getByName(CONFIGURATION_NAME).asFileTree
            OptimizeTestSetAction optimizeTestSetAction = createOptimizeTestSetAction(cloverPluginConvention, project, resolver, test)
            test.doFirst optimizeTestSetAction // add first, gets executed second
            test.doFirst createInstrumentCodeAction(cloverPluginConvention, project, resolver, test) // add second, gets executed first
            test.include optimizeTestSetAction // action is also a file inclusion spec
            test.doLast createCreateSnapshotAction(cloverPluginConvention, project, test)
            if (project.hasProperty('cloverInstrumentedJar')) {
                log.info "Skipping RestoreOriginalClassesAction for {} to generate instrumented JAR", test
            } else {
                test.doLast createRestoreOriginalClassesAction(resolver, test)
            }
            aggregateDatabasesTask.aggregate(test)
        }
    }

    private RestoreOriginalClassesAction createRestoreOriginalClassesAction(SourceSetsResolver resolver, Test testTask) {
        RestoreOriginalClassesAction restoreOriginalClassesAction = createInstance(RestoreOriginalClassesAction)
        restoreOriginalClassesAction.conventionMapping.with {
            map('sourceSets') { resolver.getSourceSets() }
            map('testSourceSets') { resolver.getTestSourceSets() }
        }
        restoreOriginalClassesAction
    }

    private CreateSnapshotAction createCreateSnapshotAction(CloverPluginConvention cloverPluginConvention, Project project, Test testTask) {
        CreateSnapshotAction createSnapshotAction = createInstance(CreateSnapshotAction)
        createSnapshotAction.conventionMapping.with {
            map('initString') { getInitString(cloverPluginConvention, testTask) }
            map('optimizeTests') { cloverPluginConvention.optimizeTests }
            map('snapshotFile') { getSnapshotFile(project, cloverPluginConvention, true, testTask) }
            map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            map('buildDir') { project.buildDir }
        }
        createSnapshotAction
    }

    private OptimizeTestSetAction createOptimizeTestSetAction(CloverPluginConvention cloverPluginConvention, Project project, SourceSetsResolver resolver, Test testTask) {
        OptimizeTestSetAction optimizeTestSetAction = createInstance(OptimizeTestSetAction)
        optimizeTestSetAction.conventionMapping.with {
            map('initString') { getInitString(cloverPluginConvention, testTask) }
            map('optimizeTests') { cloverPluginConvention.optimizeTests }
            map('snapshotFile') { getSnapshotFile(project, cloverPluginConvention, false, testTask) }
            map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            map('testSourceSets') { resolver.getTestSourceSets() }
            map('buildDir') { project.buildDir }
        }
        optimizeTestSetAction
    }

    private InstrumentCodeAction createInstrumentCodeAction(CloverPluginConvention cloverPluginConvention, Project project, SourceSetsResolver resolver, Test testTask) {
        InstrumentCodeAction instrumentCodeAction = createInstance(InstrumentCodeAction)
        instrumentCodeAction.conventionMapping.with {
            map('initString') { getInitString(cloverPluginConvention, testTask) }
            map('enabled') { cloverPluginConvention.enabled }
            map('compileGroovy') { hasGroovyPlugin(project) }
            map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            map('testRuntimeClasspath') { getTestRuntimeClasspath(project, testTask).asFileTree }
            map('groovyClasspath') { getGroovyClasspath(project) }
            map('buildDir') { project.buildDir }
            map('sourceSets') { resolver.getSourceSets() }
            map('testSourceSets') { resolver.getTestSourceSets() }
            map('sourceCompatibility') { project.sourceCompatibility?.toString() }
            map('targetCompatibility') { project.targetCompatibility?.toString() }
            map('includes') { getIncludes(project, cloverPluginConvention) }
            map('excludes') { cloverPluginConvention.excludes }
            map('testIncludes') { getTestIncludes(project, cloverPluginConvention) }
            map('testExcludes') { getTestExcludes(project, cloverPluginConvention) }
            map('statementContexts') { cloverPluginConvention.contexts.statements }
            map('methodContexts') { cloverPluginConvention.contexts.methods }
            map('executable') { cloverPluginConvention.compiler.executable?.absolutePath }
            map('encoding') { cloverPluginConvention.compiler.encoding }
            map('instrumentLambda') { cloverPluginConvention.instrumentLambda }
            map('debug') { cloverPluginConvention.compiler.debug }
            map('flushinterval') { cloverPluginConvention.flushinterval }
            map('flushpolicy') { cloverPluginConvention.flushpolicy.name() }
            map('additionalArgs') { cloverPluginConvention.compiler.additionalArgs }
        }
        instrumentCodeAction
    }

    private void configureGenerateCoverageReportTask(Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        project.tasks.withType(GenerateCoverageReportTask) { GenerateCoverageReportTask generateCoverageReportTask ->
            dependsOn aggregateDatabasesTask
            conventionMapping.with {
                map('initString') { getInitString(cloverPluginConvention) }
                map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
                map('targetPercentage') { cloverPluginConvention.targetPercentage }
                map('filter') { cloverPluginConvention.report.filter }
                map('testResultsDir') { cloverPluginConvention.report.testResultsDir }
                map('testResultsInclude') { cloverPluginConvention.report.testResultsInclude }
            }
            setCloverReportConventionMappings(project, cloverPluginConvention, generateCoverageReportTask)
        }

        GenerateCoverageReportTask generateCoverageReportTask = project.tasks.create(GENERATE_REPORT_TASK_NAME, GenerateCoverageReportTask)
        generateCoverageReportTask.description = 'Generates Clover code coverage report.'
        generateCoverageReportTask.group = REPORT_GROUP
    }

    private void configureAggregateReportsTask(Project project, CloverPluginConvention cloverPluginConvention) {
        project.tasks.withType(AggregateReportsTask) { AggregateReportsTask aggregateReportsTask ->
            conventionMapping.with {
                map('initString') { getInitString(cloverPluginConvention) }
                map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
                map('subprojectBuildDirs') { project.subprojects.collect { it.buildDir } }
                map('filter') { cloverPluginConvention.report.filter }
                map('testResultsDir') { cloverPluginConvention.report.testResultsDir }
                map('testResultsInclude') { cloverPluginConvention.report.testResultsInclude }
            }
            setCloverReportConventionMappings(project, cloverPluginConvention, aggregateReportsTask)
        }

        // Only add task to root project
        if(project == project.rootProject && project.subprojects.size() > 0) {
            AggregateReportsTask aggregateReportsTask = project.rootProject.tasks.create(AGGREGATE_REPORTS_TASK_NAME, AggregateReportsTask)
            aggregateReportsTask.description = 'Aggregates Clover code coverage reports.'
            aggregateReportsTask.group = REPORT_GROUP
            project.allprojects*.tasks*.withType(GenerateCoverageReportTask) {
                aggregateReportsTask.dependsOn it
            }
        }
    }

    /**
     * Sets Clover report convention mappings.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @param task Task
     */
    private void setCloverReportConventionMappings(Project project, CloverPluginConvention cloverPluginConvention, Task task) {
        task.conventionMapping.with {
            map('reportsDir') { new File(project.buildDir, 'reports') }
            map('xml') { cloverPluginConvention.report.xml }
            map('json') { cloverPluginConvention.report.json }
            map('html') { cloverPluginConvention.report.html }
            map('pdf') { cloverPluginConvention.report.pdf }
            
            map('additionalColumns') { cloverPluginConvention.report.columns.getColumns() }

            cloverPluginConvention.report.historical.with {
                map('historical') { enabled }
                map('historyDir') { getHistoryDir(project, cloverPluginConvention) }
                map('historyIncludes') { historyIncludes }
                map('packageFilter') { packageFilter }
                map('from') { from }
                map('to') { to }
                map('added') { added }
                map('movers') { movers }
            }
        }
    }

    /**
     * Creates an instance of the specified class, using an ASM-backed class generator.
     *
     * @param clazz the type of object to create
     * @return an instance of the specified type
     */
    @CompileStatic
    private createInstance(Class clazz) {
        AsmBackedClassGenerator generator = new AsmBackedClassGenerator()
        Class instrumentClass = generator.generate(clazz)
        Constructor constructor = instrumentClass.getConstructor()
        return constructor.newInstance()
    }

    /**
     * Gets init String that determines location of Clover database.
     *
     * @param cloverPluginConvention Clover plugin convention
     * @return Init String
     */
    @CompileStatic
    private String getInitString(CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.initString ?: DEFAULT_CLOVER_DATABASE
    }

    @CompileStatic
    private String getInitString(CloverPluginConvention cloverPluginConvention, Test testTask) {
        "${getInitString(cloverPluginConvention)}-${testTask.name}"
    }

    /**
     * Gets the Clover snapshot file location.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @param force if true, return the snapshot file even if it doesn't exist; if false, don't return the snapshot file if it doesn't exist
     * @return the Clover snapshot file location
     */
    @CompileStatic
    private File getSnapshotFile(Project project, CloverPluginConvention cloverPluginConvention, boolean force, Test testTask) {
        File file = cloverPluginConvention.snapshotFile != null && cloverPluginConvention.snapshotFile != '' ?
            project.file("${cloverPluginConvention.snapshotFile}-${testTask.name}") :
            project.file("${DEFAULT_CLOVER_SNAPSHOT}-${testTask.name}")
        return file.exists() || force ? file : null
    }

    /**
     * Gets the Clover history directory location.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return the Clover history directory location
     */
    @CompileStatic
    private File getHistoryDir(Project project, CloverPluginConvention cloverPluginConvention) {
        File file = cloverPluginConvention.historyDir != null && cloverPluginConvention.historyDir != '' ?
            project.file(cloverPluginConvention.historyDir) :
            project.file(DEFAULT_CLOVER_HISTORY_DIR)
        return file
    }

    /**
     * This construct avoids multiple evaluations of the source sets collections.
     * Without this each convention call to get the source sets will repeat the
     * computation giving back the same results.
     */
    @CompileStatic
    private class SourceSetsResolver {
        private final Project project
        private final CloverPluginConvention cloverPluginConvention
        private final boolean gradleLessThan4

        SourceSetsResolver(Project project, CloverPluginConvention cloverPluginConvention) {
            this.project = project
            this.cloverPluginConvention = cloverPluginConvention
            gradleLessThan4 = project.gradle.gradleVersion.split('\\.')[0].toInteger() < 4
        }

        List<CloverSourceSet> sourceSets = null
        @CompileDynamic
        synchronized List<CloverSourceSet> getSourceSets() {
            if (sourceSets != null) {
                return sourceSets
            }

            sourceSets = new ArrayList<CloverSourceSet>()
            Callable<FileCollection> classpathCallable = new Callable<FileCollection>() {
                FileCollection call() {
                    project.sourceSets.main.getCompileClasspath() + project.configurations.getByName(CONFIGURATION_NAME)
                }
            }

            if (hasJavaPlugin(project)) {
                CloverSourceSet cloverSourceSet = new CloverSourceSet(false)
                cloverSourceSet.with {
                    srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.main.java.srcDirs))
                    if (gradleLessThan4) {
                        classesDir = project.sourceSets.main.output.classesDir
                    } else {
                        classesDir = project.sourceSets.main.java.outputDir
                    }
                    classpathProvider = classpathCallable
                }
                sourceSets << cloverSourceSet
            }

            if (hasGroovyPlugin(project)) {
                CloverSourceSet cloverSourceSet = new CloverSourceSet(true)
                cloverSourceSet.with {
                    srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.main.groovy.srcDirs))
                    if (gradleLessThan4) {
                        classesDir = project.sourceSets.main.output.classesDir
                    } else {
                        classesDir = project.sourceSets.main.groovy.outputDir
                    }
                    classpathProvider = classpathCallable
                }
                sourceSets << cloverSourceSet
            }

            if (cloverPluginConvention.additionalSourceSets) {
                cloverPluginConvention.additionalSourceSets.each { additionalSourceSet ->
                    additionalSourceSet.groovy = hasGroovySource(project, additionalSourceSet.srcDirs)
                    additionalSourceSet.classpathProvider = classpathCallable
                    sourceSets << additionalSourceSet
                }
            }

            sourceSets
        }

        List<CloverSourceSet> testSourceSets = null
        @CompileDynamic
        synchronized List<CloverSourceSet> getTestSourceSets() {
            if (testSourceSets != null) {
                return testSourceSets
            }

            testSourceSets = new ArrayList<CloverSourceSet>()
            Callable<FileCollection> classpathCallable = new Callable<FileCollection>() {
                FileCollection call() {
                    project.sourceSets.test.getCompileClasspath() + project.configurations.getByName(CONFIGURATION_NAME)
                }
            }

            if (hasJavaPlugin(project)) {
                CloverSourceSet cloverSourceSet = new CloverSourceSet(false)
                cloverSourceSet.with {
                    srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.test.java.srcDirs))
                    if (gradleLessThan4) {
                        classesDir = project.sourceSets.test.output.classesDir
                    } else {
                        classesDir = project.sourceSets.test.java.outputDir
                    }
                    classpathProvider = classpathCallable
                }
                testSourceSets << cloverSourceSet
            }

            if (hasGroovyPlugin(project)) {
                CloverSourceSet cloverSourceSet = new CloverSourceSet(true)
                cloverSourceSet.with {
                    srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.test.groovy.srcDirs))
                    if (gradleLessThan4) {
                        classesDir = project.sourceSets.test.output.classesDir
                    } else {
                        classesDir = project.sourceSets.test.groovy.outputDir
                    }
                    classpathProvider = classpathCallable
                }
                testSourceSets << cloverSourceSet
            }

            if (cloverPluginConvention.additionalTestSourceSets) {
                cloverPluginConvention.additionalTestSourceSets.each { additionalTestSourceSet ->
                    additionalTestSourceSet.groovy = hasGroovySource(project, additionalTestSourceSet.srcDirs)
                    additionalTestSourceSet.classpathProvider = classpathCallable
                    testSourceSets << additionalTestSourceSet
                }
            }

            testSourceSets
        }

        @CompileStatic
        private boolean hasGroovySource(Project project, Collection<File> dirs) {
            for (File dir : dirs) {
                if (!project.fileTree(dir: dir, includes: ['**/*.groovy']).getFiles().isEmpty()) {
                    return true
                }
            }
            return false
        }

        @CompileStatic
        private Set<File> filterNonExistentDirectories(Set<File> dirs) {
            dirs.findAll { it.exists() }
        }
    }

    /**
     * Gets includes for compilation. Uses includes if set as convention property. Otherwise, use default includes. The
     * default includes are determined by the fact if Groovy plugin was applied to project or not.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Includes
     */
    @CompileStatic
    private List getIncludes(Project project, CloverPluginConvention cloverPluginConvention) {
        if(cloverPluginConvention.includes) {
            return cloverPluginConvention.includes
        }

        if(hasGroovyPlugin(project)) {
            return [DEFAULT_JAVA_INCLUDES, DEFAULT_GROOVY_INCLUDES]
        }

        [DEFAULT_JAVA_INCLUDES]
    }

    /**
     * Gets test includes for compilation. Uses includes if set as convention property. Otherwise, use default includes. The
     * default includes are determined by the fact if Groovy plugin was applied to project or not.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Test includes
     */
    @CompileStatic
    private List getTestIncludes(Project project, CloverPluginConvention cloverPluginConvention) {
        if(cloverPluginConvention.testIncludes) {
            return cloverPluginConvention.testIncludes
        }

        if(hasGroovyPlugin(project)) {
            return [DEFAULT_JAVA_TEST_INCLUDES, DEFAULT_GROOVY_TEST_INCLUDES, DEFAULT_SPOCK_TEST_INCLUDES]
        }

        [DEFAULT_JAVA_TEST_INCLUDES]
    }

    /**
     * Gets test patterns excluded from instrumentation. The default is empty list - no excludes.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Test excludes
     */
    @CompileStatic
    private List getTestExcludes(Project project, CloverPluginConvention cloverPluginConvention) {
        if (cloverPluginConvention.testExcludes) {
            return cloverPluginConvention.testExcludes
        }

        []
    }

    /**
     * Checks to see if Java plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    @CompileStatic
    private boolean hasJavaPlugin(Project project) {
        project.plugins.hasPlugin(JavaPlugin)
    }

    /**
     * Checks to see if Groovy or Grails plugins got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    @CompileStatic
    private boolean hasGroovyPlugin(Project project) {
        project.plugins.hasPlugin(GroovyPlugin) ||
		project.plugins.hasPlugin('org.grails.grails-core') ||
		project.plugins.hasPlugin('org.grails.grails-plugin') ||
		project.plugins.hasPlugin('org.grails.grails-web')
    }

    @CompileStatic
    private FileCollection getTestRuntimeClasspath(Project project, Test testTask) {
        testTask.classpath.filter { File file -> !file.directory } + project.configurations.getByName(CONFIGURATION_NAME)
    }

    private FileCollection getGroovyClasspath(Project project) {
        // We use the test sourceSet to derive the GroovyCompile built-in task name
        // and from there extract the correct GroovyClasspath. This is more closely
        // matched to the built-in Groovy compiler and still supports a build dependency.
        def taskName = project.sourceSets.test.getCompileTaskName('groovy')
        def task = project.tasks.findByName(taskName)
        if (task == null) {
            // Fall back to main source set to get this. We should have this
            // or the test source set using Groovy if this method is called.
            taskName = project.sourceSets.main.getCompileTaskName('groovy')
            task = project.tasks.getByName(taskName)
        }
        task.getGroovyClasspath() + project.configurations.getByName(CONFIGURATION_NAME)
    }
}
