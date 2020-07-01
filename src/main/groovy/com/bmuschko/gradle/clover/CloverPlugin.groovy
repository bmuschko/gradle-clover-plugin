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

import org.gradle.api.file.FileTreeElement

import java.util.concurrent.Callable

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import static com.bmuschko.gradle.clover.CloverUtils.*

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
        }
    }

    @CompileStatic
    private boolean testTaskEnabled(Test test, CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.enabled && !((cloverPluginConvention.includeTasks && !(test.name in cloverPluginConvention.includeTasks)) || test.name in cloverPluginConvention.excludeTasks)
    }

    @CompileStatic
    private static String getInstrumentationTaskName(Test testTask) {
        return "cloverInstrumentCodeFor${testTask.name.capitalize()}"
    }

    private void configureActionsForTask(Test test, Project project, CloverPluginConvention cloverPluginConvention, SourceSetsResolver resolver, AggregateDatabasesTask aggregateDatabasesTask) {
        if (testTaskEnabled(test, cloverPluginConvention)) {
            // Add instrumentation task
            def instrumentCodeTask = project.tasks.create(getInstrumentationTaskName(test), CloverInstrumentationTask, cloverPluginConvention, test, resolver)
            instrumentCodeTask.dependsOn(test.testClassesDirs)

            FileCollection instrumentedClassDirs = instrumentCodeTask.instrumentedMainClasses
            FileCollection instrumentedTestClassDirs = instrumentCodeTask.instrumentedTestClasses
            FileCollection originalClassDirs = instrumentCodeTask.originalMainClasses
            FileCollection originalTestClassDirs = instrumentCodeTask.originalTestClasses
            test.ext.originalClasspath = test.classpath
            test.classpath = instrumentedClassDirs + instrumentedTestClassDirs + test.classpath - originalClassDirs - originalTestClassDirs

            test.getConventionMapping().map("testClassesDirs") { instrumentedTestClassDirs }

            // NB: I believe this is a bug in one of the Android plugins used in the
            // user's build who reported this in Issue #111, adding some defensive
            // logic here to avoid adding to a null pointer. In Gradle 4.7 this
            // might change even further and perhaps will disallow assigning a null.
            test.classpath = (test.classpath ?: project.files()).plus(project.configurations.getByName(CONFIGURATION_NAME))
            OptimizeTestSetAction optimizeTestSetAction = createOptimizeTestSetAction(cloverPluginConvention, project, resolver, test)
            test.doFirst optimizeTestSetAction // add first, gets executed second

            test.include optimizeTestSetAction // action is also a file inclusion spec
            test.doLast createCreateSnapshotAction(cloverPluginConvention, project, test)
            if (project.hasProperty('cloverInstrumentedJar')) {

                log.info "Skipping RestoreOriginalClassesAction for {} to generate instrumented JAR", test
                // If we are generating instrumented JAR files make sure the Jar
                // tasks run after the Test tasks so that the instrumented classes
                // get packaged in the archives.
                project.tasks.withType(Jar) { Jar jar ->
                    jar.from instrumentedClassDirs
                    jar.exclude { FileTreeElement element ->
                        originalClassDirs.any { classesDir -> element.file.canonicalPath.startsWith(classesDir.canonicalPath) }
                    }
                }
            }
            aggregateDatabasesTask.aggregate(test)
        }
    }

    private CreateSnapshotAction createCreateSnapshotAction(CloverPluginConvention cloverPluginConvention, Project project, Test testTask) {
        CreateSnapshotAction createSnapshotAction = createInstance(project, CreateSnapshotAction)
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
        OptimizeTestSetAction optimizeTestSetAction = createInstance(project, OptimizeTestSetAction)
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
                map('alwaysReport') { cloverPluginConvention.report.alwaysReport }
                map('includeFailedTestCoverage') { cloverPluginConvention.report.includeFailedTestCoverage }
                map('numThreads') { cloverPluginConvention.report.numThreads }
                map('timeoutInterval') { cloverPluginConvention.report.timeout }
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
                map('alwaysReport') { cloverPluginConvention.report.alwaysReport }
                map('includeFailedTestCoverage') { cloverPluginConvention.report.includeFailedTestCoverage }
                map('numThreads') { cloverPluginConvention.report.numThreads }
                map('timeoutInterval') { cloverPluginConvention.report.timeout }
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

            map('additionalColumns') { cloverPluginConvention.report.columns.jsonColumns }

            cloverPluginConvention.report.historical.with {
                map('historical') { enabled }
                map('historyDir') { getHistoryDir(project, cloverPluginConvention) }
                map('historyIncludes') { historyIncludes }
                map('packageFilter') { packageFilter }
                map('from') { from }
                map('to') { to }
                map('added') { jsonAdded }
                map('movers') { jsonMovers }
            }
        }
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
    class SourceSetsResolver {
        private final Project project
        private final CloverPluginConvention cloverPluginConvention

        SourceSetsResolver(Project project, CloverPluginConvention cloverPluginConvention) {
            this.project = project
            this.cloverPluginConvention = cloverPluginConvention
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
                    classesDir = project.sourceSets.main.java.outputDir
                    instrumentedClassesDir = project.layout.buildDirectory.dir("instrumented/main/java").get().asFile
                    classpathProvider = classpathCallable
                }
                sourceSets << cloverSourceSet
            }

            if (hasGroovyPlugin(project)) {
                CloverSourceSet cloverSourceSet = new CloverSourceSet(true)
                cloverSourceSet.with {
                    srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.main.groovy.srcDirs))
                    classesDir = project.sourceSets.main.groovy.outputDir
                    instrumentedClassesDir = project.layout.buildDirectory.dir("instrumented/main/groovy").get().asFile
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
                    classesDir = project.sourceSets.test.java.outputDir
                    instrumentedClassesDir = project.layout.buildDirectory.dir("instrumented/test/java").get().asFile
                    classpathProvider = classpathCallable
                }
                testSourceSets << cloverSourceSet
            }

            if (hasGroovyPlugin(project)) {
                CloverSourceSet cloverSourceSet = new CloverSourceSet(true)
                cloverSourceSet.with {
                    srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.test.groovy.srcDirs))
                    classesDir = project.sourceSets.test.groovy.outputDir
                    instrumentedClassesDir = project.layout.buildDirectory.dir("instrumented/test/groovy").get().asFile
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
     * Checks to see if Java plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    @CompileStatic
    private boolean hasJavaPlugin(Project project) {
        project.plugins.hasPlugin(JavaPlugin)
    }
}
