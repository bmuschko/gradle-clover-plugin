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

import java.lang.reflect.Constructor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.internal.AsmBackedClassGenerator
import org.gradle.api.internal.ConventionTask
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * <p>A {@link org.gradle.api.Plugin} that provides a task for creating a code coverage report using Clover.</p>
 *
 * @author Benjamin Muschko
 */
class CloverPlugin implements Plugin<Project> {
    private static final Logger log = LoggerFactory.getLogger(CloverPlugin)
    static final String GENERATE_REPORT_TASK_NAME = 'cloverGenerateReport'
    static final String AGGREGATE_REPORTS_TASK_NAME = 'cloverAggregateReports'
    static final String REPORT_GROUP = 'report'
    static final String DEFAULT_JAVA_INCLUDES = '**/*.java'
    static final String DEFAULT_GROOVY_INCLUDES = '**/*.groovy'
    static final String DEFAULT_JAVA_TEST_INCLUDES = '**/*Test.java'
    static final String DEFAULT_GROOVY_TEST_INCLUDES = '**/*Test.groovy'
    static final String DEFAULT_CLOVER_DATABASE = '.clover/clover.db'
    static final String DEFAULT_CLOVER_LICENSE = 'clover.license'

    @Override
    void apply(Project project) {
        project.plugins.apply(JavaPlugin)

        CloverPluginConvention cloverPluginConvention = new CloverPluginConvention()
        project.convention.plugins.clover = cloverPluginConvention

        configureInstrumentationAction(project, cloverPluginConvention)
        configureGenerateCoverageReportTask(project, cloverPluginConvention)
        configureAggregateReportsTask(project, cloverPluginConvention)
    }

    private void configureInstrumentationAction(Project project, CloverPluginConvention cloverPluginConvention) {
        AsmBackedClassGenerator generator = new AsmBackedClassGenerator()
        Class<? extends InstrumentCodeAction> instrumentClass = generator.generate(InstrumentCodeAction)
        Constructor<InstrumentCodeAction> constructor = instrumentClass.getConstructor()

        InstrumentCodeAction instrumentCodeAction = constructor.newInstance()
        instrumentCodeAction.conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('compileGroovy') { hasGroovyPlugin(project) }
        instrumentCodeAction.conventionMapping.map('testRuntimeClasspath') { project.configurations.testRuntime.asFileTree }
        instrumentCodeAction.conventionMapping.map('groovyClasspath') { project.configurations.groovy.asFileTree }
        instrumentCodeAction.conventionMapping.map('classesBackupDir') { getClassesBackupDirectory(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('testClassesBackupDir') { getTestClassesBackupDirectory(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('buildDir') { project.buildDir }
        instrumentCodeAction.conventionMapping.map('classesDir') { project.sourceSets.main.classesDir }
        instrumentCodeAction.conventionMapping.map('testClassesDir') { project.sourceSets.test.classesDir }
        instrumentCodeAction.conventionMapping.map('srcDirs') { getSourceDirectories(project) }
        instrumentCodeAction.conventionMapping.map('testSrcDirs') { getTestSourceDirectories(project) }
        instrumentCodeAction.conventionMapping.map('sourceCompatibility') { project.sourceCompatibility?.toString() }
        instrumentCodeAction.conventionMapping.map('targetCompatibility') { project.targetCompatibility?.toString() }
        instrumentCodeAction.conventionMapping.map('includes') { getIncludes(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('excludes') { cloverPluginConvention.excludes }
        instrumentCodeAction.conventionMapping.map('testIncludes') { getTestIncludes(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('statementContexts') { cloverPluginConvention.contexts.statements }
        instrumentCodeAction.conventionMapping.map('methodContexts') { cloverPluginConvention.contexts.methods }

        project.gradle.taskGraph.whenReady { TaskExecutionGraph graph ->
            def generateReportTask = project.tasks.getByName(GENERATE_REPORT_TASK_NAME)

            // Only invoke instrumentation when Clover report generation task is run
            if(graph.hasTask(generateReportTask)) {
                project.tasks.withType(Test).each { Test test ->
                    test.doFirst instrumentCodeAction
                }
            }
        }
    }

    private void configureGenerateCoverageReportTask(Project project, CloverPluginConvention cloverPluginConvention) {
        project.tasks.withType(GenerateCoverageReportTask).whenTaskAdded { GenerateCoverageReportTask generateCoverageReportTask ->
            generateCoverageReportTask.dependsOn project.tasks.withType(Test)
            generateCoverageReportTask.conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
            generateCoverageReportTask.conventionMapping.map('buildDir') { project.buildDir }
            generateCoverageReportTask.conventionMapping.map('classesDir') { project.sourceSets.main.classesDir }
            generateCoverageReportTask.conventionMapping.map('testClassesDir') { project.sourceSets.test.classesDir }
            generateCoverageReportTask.conventionMapping.map('classesBackupDir') { getClassesBackupDirectory(project, cloverPluginConvention) }
            generateCoverageReportTask.conventionMapping.map('testClassesBackupDir') { getTestClassesBackupDirectory(project, cloverPluginConvention) }
            generateCoverageReportTask.conventionMapping.map('testSrcDirs') { getTestSourceDirectories(project) }
            generateCoverageReportTask.conventionMapping.map('testRuntimeClasspath') { project.configurations.testRuntime.asFileTree }
            generateCoverageReportTask.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            generateCoverageReportTask.conventionMapping.map('targetPercentage') { cloverPluginConvention.targetPercentage }
            generateCoverageReportTask.conventionMapping.map('filter') { cloverPluginConvention.report.filter }
            generateCoverageReportTask.conventionMapping.map('testIncludes') { getTestIncludes(project, cloverPluginConvention) }
            setCloverReportConventionMappings(project, cloverPluginConvention, generateCoverageReportTask)
        }

        GenerateCoverageReportTask generateCoverageReportTask = project.tasks.add(GENERATE_REPORT_TASK_NAME, GenerateCoverageReportTask)
        generateCoverageReportTask.description = 'Generates Clover code coverage report.'
        generateCoverageReportTask.group = REPORT_GROUP
    }

    private void configureAggregateReportsTask(Project project, CloverPluginConvention cloverPluginConvention) {
        project.tasks.withType(AggregateReportsTask).whenTaskAdded { AggregateReportsTask aggregateReportsTask ->
            aggregateReportsTask.conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
            aggregateReportsTask.conventionMapping.map('testRuntimeClasspath') {
                // @todo: Not cool. Needs Clover configuration.
                project.subprojects.first().configurations.testRuntime.asFileTree
            }
            aggregateReportsTask.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            aggregateReportsTask.conventionMapping.map('buildDir') { project.buildDir }
            aggregateReportsTask.conventionMapping.map('subprojectBuildDirs') { project.subprojects.collect { it.buildDir } }
            setCloverReportConventionMappings(project, cloverPluginConvention, aggregateReportsTask)
        }

        project.afterEvaluate {
            // Only add task to root project
            if(project == project.rootProject && project.subprojects.size() > 0) {
                AggregateReportsTask aggregateReportsTask = project.rootProject.tasks.add(AGGREGATE_REPORTS_TASK_NAME, AggregateReportsTask)
                aggregateReportsTask.description = 'Aggregates Clover code coverage reports.'
                aggregateReportsTask.group = REPORT_GROUP
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
    private void setCloverReportConventionMappings(Project project, CloverPluginConvention cloverPluginConvention, ConventionTask task) {
        task.conventionMapping.map('reportsDir') { project.reportsDir }
        task.conventionMapping.map('xml') { cloverPluginConvention.report.xml }
        task.conventionMapping.map('json') { cloverPluginConvention.report.json }
        task.conventionMapping.map('html') { cloverPluginConvention.report.html }
        task.conventionMapping.map('pdf') { cloverPluginConvention.report.pdf }
        task.conventionMapping.map('projectName') { project.name }
    }

    /**
     * Gets init String that determines location of Clover database.
     *
     * @param cloverPluginConvention Clover plugin convention
     * @return Init String
     */
    private String getInitString(CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.initString ?: DEFAULT_CLOVER_DATABASE
    }

    /**
     * Gets classes backup directory.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Classes backup directory
     */
    private File getClassesBackupDirectory(Project project, CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.classesBackupDir ?: new File("${project.sourceSets.main.classesDir}-bak")
    }

    /**
     * Gets test classes backup directory.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Classes backup directory
     */
    private File getTestClassesBackupDirectory(Project project, CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.testClassesBackupDir ?: new File("${project.sourceSets.test.classesDir}-bak")
    }

    /**
     * Gets Clover license file.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return License file
     */
    private File getLicenseFile(Project project, CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.licenseFile ?: new File(project.rootDir, DEFAULT_CLOVER_LICENSE)
    }

    /**
     * Gets source directories. If the Groovy plugin was applied we only its source directories in addition to the
     * Java plugin source directories. We only add directories that actually exist.
     *
     * @param project Project
     * @return Source directories
     */
    private Set<File> getSourceDirectories(Project project) {
        def srcDirs = [] as Set<File>

        if(hasGroovyPlugin(project)) {
            addExistingSourceDirectories(srcDirs, project.sourceSets.main.java.srcDirs)
            addExistingSourceDirectories(srcDirs, project.sourceSets.main.groovy.srcDirs)
        }
        else {
            addExistingSourceDirectories(srcDirs, project.sourceSets.main.java.srcDirs)
        }

        srcDirs
    }

    /**
     * Gets test source directories. If the Groovy plugin was applied we only its test source directories in addition to the
     * Java plugin source directories. We only add directories that actually exist.
     *
     * @param project Project
     * @return Test source directories
     */
    private Set<File> getTestSourceDirectories(Project project) {
        def testSrcDirs = [] as Set<File>

        if(hasGroovyPlugin(project)) {
            addExistingSourceDirectories(testSrcDirs, project.sourceSets.test.java.srcDirs)
            addExistingSourceDirectories(testSrcDirs, project.sourceSets.test.groovy.srcDirs)
        }
        else {
            addExistingSourceDirectories(testSrcDirs, project.sourceSets.test.java.srcDirs)
        }

        testSrcDirs
    }

    /**
     * Adds source directories to target Set only if they actually exist.
     *
     * @param target Target
     * @param source Source
     */
    private void addExistingSourceDirectories(Set<File> target, Set<File> source) {
        source.each {
            if(it.exists()) {
                target << it
            }
            else {
                log.warn "The specified source directory '$it.canonicalPath' does not exist. It won't be included in Clover instrumentation."
            }
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
    private List getTestIncludes(Project project, CloverPluginConvention cloverPluginConvention) {
        if(cloverPluginConvention.testIncludes) {
            return cloverPluginConvention.testIncludes
        }

        if(hasGroovyPlugin(project)) {
            return [DEFAULT_JAVA_TEST_INCLUDES, DEFAULT_GROOVY_TEST_INCLUDES]
        }

        [DEFAULT_JAVA_TEST_INCLUDES]
    }

    /**
     * Checks to see if Groovy plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasGroovyPlugin(Project project) {
        project.plugins.hasPlugin(GroovyPlugin)
    }
}
