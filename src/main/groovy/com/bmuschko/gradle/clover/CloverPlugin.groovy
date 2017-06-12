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

import com.bmuschko.gradle.clover.internal.LicenseResolverFactory
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
    static final String DEFAULT_CLOVER_DATABASE = '.clover/clover.db'
    static final String DEFAULT_CLOVER_SNAPSHOT = '.clover/coverage.db.snapshot'
    static final String DEFAULT_CLOVER_HISTORY_DIR = '.clover/historypoints'

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
                map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
                map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            }
        }

        AggregateDatabasesTask aggregateDatabasesTask = project.tasks.create(AGGREGATE_DATABASES_TASK_NAME, AggregateDatabasesTask)
        aggregateDatabasesTask.description = 'Aggregates Clover code coverage databases for the project.'
        aggregateDatabasesTask.group = CLOVER_GROUP
        aggregateDatabasesTask
    }

    private void configureActions(Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        project.tasks.withType(Test) { Test test ->
            // If it is too late for afterEvaluate configure now
            if (project.state.executed) {
                configureActionsForTask(test, project, cloverPluginConvention, aggregateDatabasesTask)
            } else {
                project.afterEvaluate {
                    configureActionsForTask(test, project, cloverPluginConvention, aggregateDatabasesTask)
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

    private boolean testTaskEnabled(Test test, CloverPluginConvention cloverPluginConvention) {
        !((cloverPluginConvention.includeTasks && !(test.name in cloverPluginConvention.includeTasks)) || test.name in cloverPluginConvention.excludeTasks)
    }

    private void configureActionsForTask(Test test, Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        if (testTaskEnabled(test, cloverPluginConvention)) {
            test.classpath += project.configurations.getByName(CONFIGURATION_NAME).asFileTree
            OptimizeTestSetAction optimizeTestSetAction = createOptimizeTestSetAction(cloverPluginConvention, project, test)
            test.doFirst optimizeTestSetAction // add first, gets executed second
            test.doFirst createInstrumentCodeAction(cloverPluginConvention, project, test) // add second, gets executed first
            test.include optimizeTestSetAction // action is also a file inclusion spec
            test.doLast createCreateSnapshotAction(cloverPluginConvention, project, test)
            if (project.hasProperty('cloverInstrumentedJar')) {
                log.info "Skipping RestoreOriginalClassesAction for {} to generate instrumented JAR", test
            } else {
                test.doLast createRestoreOriginalClassesAction(cloverPluginConvention, project, test)
            }
            aggregateDatabasesTask.aggregate(test)
        }
    }

    private RestoreOriginalClassesAction createRestoreOriginalClassesAction(CloverPluginConvention cloverPluginConvention, Project project, Test testTask) {
        RestoreOriginalClassesAction restoreOriginalClassesAction = createInstance(RestoreOriginalClassesAction)
        restoreOriginalClassesAction.conventionMapping.map('sourceSets') { getSourceSets(project, cloverPluginConvention) }
        restoreOriginalClassesAction.conventionMapping.map('testSourceSets') { getTestSourceSets(project, cloverPluginConvention) }

        restoreOriginalClassesAction
    }

    private CreateSnapshotAction createCreateSnapshotAction(CloverPluginConvention cloverPluginConvention, Project project, Test testTask) {
        CreateSnapshotAction createSnapshotAction = createInstance(CreateSnapshotAction)
        createSnapshotAction.conventionMapping.map('initString') { getInitString(cloverPluginConvention, testTask) }
        createSnapshotAction.conventionMapping.map('optimizeTests') { cloverPluginConvention.optimizeTests }
        createSnapshotAction.conventionMapping.map('snapshotFile') { getSnapshotFile(project, cloverPluginConvention, true, testTask) }
        createSnapshotAction.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
        createSnapshotAction.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
        createSnapshotAction.conventionMapping.map('buildDir') { project.buildDir }
        createSnapshotAction
    }

    private OptimizeTestSetAction createOptimizeTestSetAction(CloverPluginConvention cloverPluginConvention, Project project, Test testTask) {
        OptimizeTestSetAction optimizeTestSetAction = createInstance(OptimizeTestSetAction)
        optimizeTestSetAction.conventionMapping.map('initString') { getInitString(cloverPluginConvention, testTask) }
        optimizeTestSetAction.conventionMapping.map('optimizeTests') { cloverPluginConvention.optimizeTests }
        optimizeTestSetAction.conventionMapping.map('useClover3') { useClover3(project, cloverPluginConvention) }
        optimizeTestSetAction.conventionMapping.map('snapshotFile') { getSnapshotFile(project, cloverPluginConvention, false, testTask) }
        optimizeTestSetAction.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
        optimizeTestSetAction.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
        optimizeTestSetAction.conventionMapping.map('testSourceSets') { getTestSourceSets(project, cloverPluginConvention) }
        optimizeTestSetAction.conventionMapping.map('buildDir') { project.buildDir }
        optimizeTestSetAction
    }

    private InstrumentCodeAction createInstrumentCodeAction(CloverPluginConvention cloverPluginConvention, Project project, Test testTask) {
        InstrumentCodeAction instrumentCodeAction = createInstance(InstrumentCodeAction)
        instrumentCodeAction.conventionMapping.map('initString') { getInitString(cloverPluginConvention, testTask) }
        instrumentCodeAction.conventionMapping.map('enabled') { cloverPluginConvention.enabled }
        instrumentCodeAction.conventionMapping.map('compileGroovy') { hasGroovyPlugin(project) }
        instrumentCodeAction.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
        instrumentCodeAction.conventionMapping.map('testRuntimeClasspath') { getTestRuntimeClasspath(project, testTask).asFileTree }
        instrumentCodeAction.conventionMapping.map('groovyClasspath') { getGroovyClasspath(project) }
        instrumentCodeAction.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('buildDir') { project.buildDir }
        instrumentCodeAction.conventionMapping.map('sourceSets') { getSourceSets(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('testSourceSets') { getTestSourceSets(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('sourceCompatibility') { project.sourceCompatibility?.toString() }
        instrumentCodeAction.conventionMapping.map('targetCompatibility') { project.targetCompatibility?.toString() }
        instrumentCodeAction.conventionMapping.map('includes') { getIncludes(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('excludes') { cloverPluginConvention.excludes }
        instrumentCodeAction.conventionMapping.map('testIncludes') { getTestIncludes(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('testExcludes') { getTestExcludes(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('statementContexts') { cloverPluginConvention.contexts.statements }
        instrumentCodeAction.conventionMapping.map('methodContexts') { cloverPluginConvention.contexts.methods }
        instrumentCodeAction.conventionMapping.map('executable') { cloverPluginConvention.compiler.executable?.absolutePath }
        instrumentCodeAction.conventionMapping.map('encoding') { cloverPluginConvention.compiler.encoding }
        instrumentCodeAction.conventionMapping.map('instrumentLambda') { cloverPluginConvention.instrumentLambda }
        instrumentCodeAction.conventionMapping.map('debug') { cloverPluginConvention.compiler.debug }
        instrumentCodeAction.conventionMapping.map('flushinterval') { cloverPluginConvention.flushinterval }
        instrumentCodeAction.conventionMapping.map('flushpolicy') { cloverPluginConvention.flushpolicy.name() }
        instrumentCodeAction
    }

    private void configureGenerateCoverageReportTask(Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        project.tasks.withType(GenerateCoverageReportTask) { GenerateCoverageReportTask generateCoverageReportTask ->
            dependsOn aggregateDatabasesTask
            conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
            conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            conventionMapping.map('targetPercentage') { cloverPluginConvention.targetPercentage }
            conventionMapping.map('filter') { cloverPluginConvention.report.filter }
            conventionMapping.map('testResultsDir') { cloverPluginConvention.report.testResultsDir }
            conventionMapping.map('testResultsInclude') { cloverPluginConvention.report.testResultsInclude }
            setCloverReportConventionMappings(project, cloverPluginConvention, generateCoverageReportTask)
        }

        GenerateCoverageReportTask generateCoverageReportTask = project.tasks.create(GENERATE_REPORT_TASK_NAME, GenerateCoverageReportTask)
        generateCoverageReportTask.description = 'Generates Clover code coverage report.'
        generateCoverageReportTask.group = REPORT_GROUP
    }

    private void configureAggregateReportsTask(Project project, CloverPluginConvention cloverPluginConvention) {
        project.tasks.withType(AggregateReportsTask) { AggregateReportsTask aggregateReportsTask ->
            conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
            conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            conventionMapping.map('subprojectBuildDirs') { project.subprojects.collect { it.buildDir } }
            conventionMapping.map('filter') { cloverPluginConvention.report.filter }
            conventionMapping.map('testResultsDir') { cloverPluginConvention.report.testResultsDir }
            conventionMapping.map('testResultsInclude') { cloverPluginConvention.report.testResultsInclude }
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
    private String getInitString(CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.initString ?: DEFAULT_CLOVER_DATABASE
    }

    private String getInitString(CloverPluginConvention cloverPluginConvention, Test testTask) {
        "${getInitString(cloverPluginConvention)}-${testTask.name}"
    }

    /**
     * Gets Clover license file.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return License file
     */
    private File getLicenseFile(Project project, CloverPluginConvention cloverPluginConvention) {
        LicenseResolverFactory.instance.getResolver(cloverPluginConvention.licenseLocation).resolve(project.rootDir, cloverPluginConvention.licenseLocation)
    }

    private boolean useClover3(Project project, CloverPluginConvention cloverPluginConvention) {
        if (cloverPluginConvention.useClover3 != null) {
            log.info "Using user supplied preference for useClover3: {}", cloverPluginConvention.useClover3
            return cloverPluginConvention.useClover3.toBoolean()
        }

        def artifacts = project.configurations.getByName(CONFIGURATION_NAME).resolvedConfiguration.resolvedArtifacts
        assert artifacts.size() == 1

        def result = artifacts.iterator().next().moduleVersion.id.version.startsWith('3.')
        log.info "Using detected version to assign useClover3: {}", result
        return result
    }

    /**
     * Gets the Clover snapshot file location.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @param force if true, return the snapshot file even if it doesn't exist; if false, don't return the snapshot file if it doesn't exist
     * @return the Clover snapshot file location
     */
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
    private File getHistoryDir(Project project, CloverPluginConvention cloverPluginConvention) {
        File file = cloverPluginConvention.historyDir != null && cloverPluginConvention.historyDir != '' ?
            project.file(cloverPluginConvention.historyDir) :
            project.file(DEFAULT_CLOVER_HISTORY_DIR)
        return file
    }

    private Set<CloverSourceSet> getSourceSets(Project project, CloverPluginConvention cloverPluginConvention) {
        def sourceSets = []

         if(hasGroovyPlugin(project)) {
            CloverSourceSet cloverSourceSet = new CloverSourceSet()
            cloverSourceSet.srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.main.java.srcDirs))
            cloverSourceSet.srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.main.groovy.srcDirs))
            cloverSourceSet.classesDir = project.sourceSets.main.output.classesDir
            cloverSourceSet.backupDir = cloverPluginConvention.classesBackupDir ?: new File("${project.sourceSets.main.output.classesDir}-bak")
            sourceSets << cloverSourceSet
         }
        else if(hasJavaPlugin(project)) {
            CloverSourceSet cloverSourceSet = new CloverSourceSet()
            cloverSourceSet.srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.main.java.srcDirs))
            cloverSourceSet.classesDir = project.sourceSets.main.output.classesDir
            cloverSourceSet.backupDir = cloverPluginConvention.classesBackupDir ?: new File("${project.sourceSets.main.output.classesDir}-bak")
            sourceSets << cloverSourceSet
         }

        if(cloverPluginConvention.additionalSourceSets) {
            cloverPluginConvention.additionalSourceSets.each { additionalSourceSet ->
                additionalSourceSet.backupDir = cloverPluginConvention.classesBackupDir ?: new File("${additionalSourceSet.classesDir}-bak")
                sourceSets << additionalSourceSet
            }
         }

        sourceSets
     }

    private Set<CloverSourceSet> getTestSourceSets(Project project, CloverPluginConvention cloverPluginConvention) {
        def testSourceSets = []

         if(hasGroovyPlugin(project)) {
            CloverSourceSet cloverSourceSet = new CloverSourceSet()
            cloverSourceSet.srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.test.java.srcDirs))
            cloverSourceSet.srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.test.groovy.srcDirs))
            cloverSourceSet.classesDir = project.sourceSets.test.output.classesDir
            cloverSourceSet.backupDir = cloverPluginConvention.testClassesBackupDir ?: new File("${project.sourceSets.test.output.classesDir}-bak")
            testSourceSets << cloverSourceSet
         }
         else if(hasJavaPlugin(project)) {
            CloverSourceSet cloverSourceSet = new CloverSourceSet()
            cloverSourceSet.srcDirs.addAll(filterNonExistentDirectories(project.sourceSets.test.java.srcDirs))
            cloverSourceSet.classesDir = project.sourceSets.test.output.classesDir
            cloverSourceSet.backupDir = cloverPluginConvention.testClassesBackupDir ?: new File("${project.sourceSets.test.output.classesDir}-bak")
            testSourceSets << cloverSourceSet
         }

        if(cloverPluginConvention.additionalTestSourceSets) {
            cloverPluginConvention.additionalTestSourceSets.each { additionalTestSourceSet ->
                additionalTestSourceSet.backupDir = cloverPluginConvention.classesBackupDir ?: new File("${additionalTestSourceSet.classesDir}-bak")
                testSourceSets << additionalTestSourceSet
            }
         }

        testSourceSets
     }

    private Set<File> filterNonExistentDirectories(Set<File> dirs) {
        dirs.findAll { it.exists() }
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
                log.info "The specified source directory '$it.canonicalPath' does not exist. It won't be included in Clover instrumentation."
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
     * Gets test patterns excluded from instrumentation. The default is empty list - no excludes.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Test excludes
     */
    private List getTestExcludes(Project project, CloverPluginConvention cloverPluginConvention) {
        if(cloverPluginConvention.testExcludes) {
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
    private boolean hasJavaPlugin(Project project) {
        project.plugins.hasPlugin(JavaPlugin)
    }

    /**
     * Checks to see if Groovy or Grails plugins got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasGroovyPlugin(Project project) {
        project.plugins.hasPlugin(GroovyPlugin) ||
		project.plugins.hasPlugin('org.grails.grails-core') ||
		project.plugins.hasPlugin('org.grails.grails-plugin') ||
		project.plugins.hasPlugin('org.grails.grails-web')
    }

    private FileCollection getTestRuntimeClasspath(Project project, Test testTask) {
        testTask.classpath.filter { !it.directory } + project.configurations.getByName(CONFIGURATION_NAME)
    }

    private FileCollection getGroovyClasspath(Project project) {
        project.configurations.compile.asFileTree
    }
}