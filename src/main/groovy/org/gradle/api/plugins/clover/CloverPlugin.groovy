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

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.internal.AsmBackedClassGenerator
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.clover.internal.LicenseResolverFactory
import org.gradle.api.tasks.SourceSet
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
	static final String AGGREGATE_REPORTS_WITH_HISTORY_TASK_NAME = 'cloverAggregateReportsWithHistory'
    static final String REPORT_GROUP = 'report'
    static final String CLOVER_GROUP = 'clover'
    static final String DEFAULT_JAVA_INCLUDES = '**/*.java'
    static final String DEFAULT_GROOVY_INCLUDES = '**/*.groovy'
    static final String DEFAULT_JAVA_TEST_INCLUDES = '**/*Test.java'
    static final String DEFAULT_GROOVY_TEST_INCLUDES = '**/*Test.groovy'
    static final String DEFAULT_CLOVER_DATABASE = '.clover/clover.db'
    static final String DEFAULT_CLOVER_SNAPSHOT = '.clover/coverage.db.snapshot'
	static final String DEFAULT_CLOVER_HISTORY_DIR = '.clover/history'

    @Override
    void apply(Project project) {
		// TODO XCA use create()
        project.configurations.add(CONFIGURATION_NAME).setVisible(false).setTransitive(true)
                .setDescription('The Clover library to be used for this project.')

        CloverPluginConvention cloverPluginConvention = new CloverPluginConvention()
        project.convention.plugins.clover = cloverPluginConvention

        AggregateDatabasesTask aggregateDatabasesTask = configureAggregateDatabasesTask(project, cloverPluginConvention)
        configureActions(project, cloverPluginConvention, aggregateDatabasesTask)
        configureGenerateCoverageReportTask(project, cloverPluginConvention, aggregateDatabasesTask)
        configureAggregateReportsTask(project, cloverPluginConvention)
        configureAggregateReportsWithHistoryTask(project, cloverPluginConvention)
    }

    private AggregateDatabasesTask configureAggregateDatabasesTask(Project project, CloverPluginConvention cloverPluginConvention) {
        project.tasks.withType(AggregateDatabasesTask).whenTaskAdded { AggregateDatabasesTask aggregateDatabasesTask ->
            aggregateDatabasesTask.conventionMapping.with {
                map('initString') { getInitString(cloverPluginConvention) }
                map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
                map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            }
        }

		// TODO use create()
        AggregateDatabasesTask aggregateDatabasesTask = project.tasks.add(AGGREGATE_DATABASES_TASK_NAME, AggregateDatabasesTask)
        aggregateDatabasesTask.description = 'Aggregates Clover code coverage databases for the project.'
        aggregateDatabasesTask.group = CLOVER_GROUP
        aggregateDatabasesTask
    }

    private void configureActions(Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        project.tasks.withType(Test) { Test test ->
            project.afterEvaluate {
                if (cloverPluginConvention.includeTasks) {
                    if (test.name in cloverPluginConvention.includeTasks) {
                        configureActionsForTask(test, project, cloverPluginConvention, aggregateDatabasesTask)
                    }
                } else if (!(test.name in cloverPluginConvention.excludeTasks)) {
                    configureActionsForTask(test, project, cloverPluginConvention, aggregateDatabasesTask)
                }
            }
        }
		
		// wait for the task graph to be ready, otherwise the restoreOriginalClassesAction will be executed 
		// before building the jar, resulting in a jar that contains uninstrumented classes
		// see forum entry at: http://forums.gradle.org/gradle/topics/jar_dolast_modifies_my_jar_file
		
		project.gradle.taskGraph.whenReady {
			
			if (project.hasProperty('cloverInstrumentedJar')) {
				try {
					Task jar = project.tasks.getByName("jar")
					jar.doFirst createInstrumentCodeActionForJar(cloverPluginConvention, project, jar)
					jar.doLast createRestoreOriginalClassesAction(cloverPluginConvention, project, jar)
					aggregateDatabasesTask.aggregate(jar)
				} catch (UnknownTaskException e) {
					// can be ignored because not every project contains a task called "jar"
				}
			}
		}
    }

    private void configureActionsForTask(Test test, Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        test.classpath += project.configurations.getByName(CONFIGURATION_NAME).asFileTree
        OptimizeTestSetAction optimizeTestSetAction = createOptimizeTestSetAction(cloverPluginConvention, project, test)
        test.doFirst optimizeTestSetAction // add first, gets executed second
        test.doFirst createInstrumentCodeActionForTest(cloverPluginConvention, project, test) // add second, gets executed first
        test.include optimizeTestSetAction // action is also a file inclusion spec
        test.doLast createCreateSnapshotAction(cloverPluginConvention, project, test)
        test.doLast createRestoreOriginalClassesAction(cloverPluginConvention, project, test)
        aggregateDatabasesTask.aggregate(test)
    }

    private RestoreOriginalClassesAction createRestoreOriginalClassesAction(CloverPluginConvention cloverPluginConvention, Project project, Task task) {
        RestoreOriginalClassesAction restoreOriginalClassesAction = createInstance(RestoreOriginalClassesAction)
        restoreOriginalClassesAction.conventionMapping.map('classesBackupDir') { getClassesBackupDirectory(project, cloverPluginConvention) }
        restoreOriginalClassesAction.conventionMapping.map('testClassesBackupDir') { getTestClassesBackupDirectory(project, cloverPluginConvention) }
        restoreOriginalClassesAction.conventionMapping.map('classesDir') { project.sourceSets.main.output.classesDir }
        restoreOriginalClassesAction.conventionMapping.map('testClassesDir') { task.hasProperty("testClassesDir") ? task.testClassesDir : null }
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
        optimizeTestSetAction.conventionMapping.map('snapshotFile') { getSnapshotFile(project, cloverPluginConvention, false, testTask) }
        optimizeTestSetAction.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
        optimizeTestSetAction.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
        optimizeTestSetAction.conventionMapping.map('testSrcDirs') { getTestSourceDirectories(project, cloverPluginConvention, testTask) }
        optimizeTestSetAction.conventionMapping.map('buildDir') { project.buildDir }
        optimizeTestSetAction
    }

    private InstrumentCodeAction createInstrumentCodeActionForTest(CloverPluginConvention cloverPluginConvention, Project project, Test testTask) {
		// instrument test classes only on a test action
		boolean instrumentTestClasses = true;
		
		// note: when mixing -PcloverInstrumentedJar and cloverReportTask, make sure the "jar" task is executed before "test"
		
		// keep the clover.db if there exists already one from building an instrumented jar
		// but generate a clover.db if there was nothing to be jared (i.e. an empty classes dir in an integration test project)
		def generateReportTask = project.tasks.getByName(GENERATE_REPORT_TASK_NAME)
		def isCloverInstrumentedJar = project.hasProperty('cloverInstrumentedJar')
		boolean performClean = (!isCloverInstrumentedJar || isMainSourceSetEmpty(project))
		
		project.logger.debug ("Create clover instrumentation in "+testTask+", with performClean="+performClean)
		return createInstrumentCodeAction(cloverPluginConvention, project, testTask, performClean, instrumentTestClasses)
    }

	private InstrumentCodeAction createInstrumentCodeActionForJar(CloverPluginConvention cloverPluginConvention, Project project, Task jarTask) {
		// instrument test classes only on a test action, not on jar
		boolean instrumentTestClasses = false
		
		// clean the clover.db before instrumenting for jar
		boolean performClean = true
		
		return createInstrumentCodeAction(cloverPluginConvention, project, jarTask, performClean, instrumentTestClasses)
	}
		
	 private InstrumentCodeAction createInstrumentCodeAction(CloverPluginConvention cloverPluginConvention, Project project, Task task, boolean performClean, boolean instrumentTestClasses) {
        InstrumentCodeAction instrumentCodeAction = createInstance(InstrumentCodeAction)
        instrumentCodeAction.conventionMapping.map('initString') { getInitString(cloverPluginConvention, task) }
        instrumentCodeAction.conventionMapping.map('compileGroovy') { hasGroovyPlugin(project) }
        instrumentCodeAction.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
		instrumentCodeAction.conventionMapping.map('testCompileClasspath') {getTestCompileClasspath(project, cloverPluginConvention).asFileTree }
		instrumentCodeAction.conventionMapping.map('compileClasspath') { getCompileClasspath(project, cloverPluginConvention).asFileTree }
        instrumentCodeAction.conventionMapping.map('groovyClasspath') { getGroovyClasspath(project) }
        instrumentCodeAction.conventionMapping.map('classesBackupDir') { getClassesBackupDirectory(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('testClassesBackupDir') { getTestClassesBackupDirectory(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('buildDir') { project.buildDir }
        instrumentCodeAction.conventionMapping.map('classesDir') { project.sourceSets.main.output.classesDir }
        instrumentCodeAction.conventionMapping.map('testClassesDir') { task.hasProperty("testClassesDir") ? task.testClassesDir : null }
        instrumentCodeAction.conventionMapping.map('srcDirs') { getSourceDirectories(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('testSrcDirs') { getTestSourceDirectories(project, cloverPluginConvention, task) }
        instrumentCodeAction.conventionMapping.map('sourceCompatibility') { project.sourceCompatibility?.toString() }
        instrumentCodeAction.conventionMapping.map('targetCompatibility') { project.targetCompatibility?.toString() }
        instrumentCodeAction.conventionMapping.map('includes') { getIncludes(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('excludes') { cloverPluginConvention.excludes }
        instrumentCodeAction.conventionMapping.map('testIncludes') { getTestIncludes(project, cloverPluginConvention) }
        instrumentCodeAction.conventionMapping.map('statementContexts') { cloverPluginConvention.contexts.statements }
        instrumentCodeAction.conventionMapping.map('methodContexts') { cloverPluginConvention.contexts.methods }
		instrumentCodeAction.performClean = performClean
		instrumentCodeAction.instrumentTestClasses = instrumentTestClasses
        instrumentCodeAction
    }

    private void configureGenerateCoverageReportTask(Project project, CloverPluginConvention cloverPluginConvention, AggregateDatabasesTask aggregateDatabasesTask) {
        project.tasks.withType(GenerateCoverageReportTask).whenTaskAdded { GenerateCoverageReportTask generateCoverageReportTask ->
            generateCoverageReportTask.dependsOn aggregateDatabasesTask
            generateCoverageReportTask.conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
            generateCoverageReportTask.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            generateCoverageReportTask.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            generateCoverageReportTask.conventionMapping.map('targetPercentage') { cloverPluginConvention.targetPercentage }
            generateCoverageReportTask.conventionMapping.map('filter') { cloverPluginConvention.report.filter }
            setCloverReportConventionMappings(project, cloverPluginConvention, generateCoverageReportTask)
        }

		// TODO  XCA use create()
        GenerateCoverageReportTask generateCoverageReportTask = project.tasks.add(GENERATE_REPORT_TASK_NAME, GenerateCoverageReportTask)
        generateCoverageReportTask.description = 'Generates Clover code coverage report.'
        generateCoverageReportTask.group = REPORT_GROUP
    }

    private void configureAggregateReportsTask(Project project, CloverPluginConvention cloverPluginConvention) {
        project.tasks.withType(AggregateReportsTask).whenTaskAdded { AggregateReportsTask aggregateReportsTask ->
            aggregateReportsTask.conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
            aggregateReportsTask.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            aggregateReportsTask.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            aggregateReportsTask.conventionMapping.map('subprojectBuildDirs') { project.subprojects.collect { it.buildDir } }
            setCloverReportConventionMappings(project, cloverPluginConvention, aggregateReportsTask)
        }

        // Only add task to root project
        if(project == project.rootProject && project.subprojects.size() > 0) {
			// TODO  XCA use create()
            AggregateReportsTask aggregateReportsTask = project.rootProject.tasks.add(AGGREGATE_REPORTS_TASK_NAME, AggregateReportsTask)
            aggregateReportsTask.description = 'Aggregates Clover code coverage reports.'
            aggregateReportsTask.group = REPORT_GROUP
            project.allprojects*.tasks*.withType(GenerateCoverageReportTask) {
                aggregateReportsTask.dependsOn it
            }
        }
    }
	
	private void configureAggregateReportsWithHistoryTask(Project project, CloverPluginConvention cloverPluginConvention) {
		project.tasks.withType(AggregateReportsWithHistoryTask).whenTaskAdded { AggregateReportsWithHistoryTask aggregateReportsWithHistoryTask ->
            aggregateReportsWithHistoryTask.conventionMapping.map('initString') { getInitString(cloverPluginConvention) }
            aggregateReportsWithHistoryTask.conventionMapping.map('cloverClasspath') { project.configurations.getByName(CONFIGURATION_NAME).asFileTree }
            aggregateReportsWithHistoryTask.conventionMapping.map('licenseFile') { getLicenseFile(project, cloverPluginConvention) }
            aggregateReportsWithHistoryTask.conventionMapping.map('subprojectBuildDirs') { project.subprojects.collect { it.buildDir } }
			aggregateReportsWithHistoryTask.conventionMapping.map('historyDir') { getHistoryDir(cloverPluginConvention)}
			setCloverReportConventionMappings(project, cloverPluginConvention, aggregateReportsWithHistoryTask)
		}
		
        // Only add task to root project
        if(project == project.rootProject && project.subprojects.size() > 0) {
			// TODO  XCA use create()
            AggregateReportsWithHistoryTask aggregateReportsWithHistoryTask = project.rootProject.tasks.add(AGGREGATE_REPORTS_WITH_HISTORY_TASK_NAME, AggregateReportsWithHistoryTask)
            aggregateReportsWithHistoryTask.description = 'Aggregates Clover code coverage reports including historical metrics.'
            aggregateReportsWithHistoryTask.group = REPORT_GROUP
            project.allprojects*.tasks*.withType(GenerateCoverageReportTask) {
                aggregateReportsWithHistoryTask.dependsOn it
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
        task.conventionMapping.map('reportsDir') { new File(project.buildDir, 'reports') }
        task.conventionMapping.map('xml') { cloverPluginConvention.report.xml }
        task.conventionMapping.map('json') { cloverPluginConvention.report.json }
        task.conventionMapping.map('html') { cloverPluginConvention.report.html }
        task.conventionMapping.map('pdf') { cloverPluginConvention.report.pdf }
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

    private String getInitString(CloverPluginConvention cloverPluginConvention, Task task) {
        "${getInitString(cloverPluginConvention)}-${task.name}"
    }
	
	
	/**
	 * Gets historyDir File that determines location of Clover history database.
	 *
	 * @param cloverPluginConvention Clover plugin convention
	 * @return historyDir
	 */
	private File getHistoryDir(CloverPluginConvention cloverPluginConvention) {
		cloverPluginConvention.historyDir ?:  new File(DEFAULT_CLOVER_HISTORY_DIR)
	}

    /**
     * Gets classes backup directory.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Classes backup directory
     */
    private File getClassesBackupDirectory(Project project, CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.classesBackupDir ?: new File("${project.sourceSets.main.output.classesDir}-bak")
    }

    /**
     * Gets test classes backup directory.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Classes backup directory
     */
    private File getTestClassesBackupDirectory(Project project, CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.testClassesBackupDir ?: new File("${project.sourceSets.test.output.classesDir}-bak")
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
     * Gets source directories. If the Groovy plugin was applied we only its source directories in addition to the
     * Java plugin source directories. We only add directories that actually exist.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Source directories
     */
    private Set<File> getSourceDirectories(Project project, CloverPluginConvention cloverPluginConvention) {
        def srcDirs = [] as Set<File>

		if (!hasSourceSets(project)) {
    		// Do nothing ( by intention! )
		} else if (hasGroovyPlugin(project)) {
            addExistingSourceDirectories(srcDirs, project.sourceSets.main.java.srcDirs)
            addExistingSourceDirectories(srcDirs, project.sourceSets.main.groovy.srcDirs)
        }
        else {
            addExistingSourceDirectories(srcDirs, project.sourceSets.main.java.srcDirs)
        }

        if(cloverPluginConvention.additionalSourceDirs) {
            addExistingSourceDirectories(srcDirs, cloverPluginConvention.additionalSourceDirs)
        }

        srcDirs
    }

    /**
     * Gets test source directories. If the Groovy plugin was applied we only its test source directories in addition to the
     * Java plugin source directories. We only add directories that actually exist.
     *
     * @param project Project
     * @param cloverPluginConvention Clover plugin convention
     * @return Test source directories
     */
    private Set<File> getTestSourceDirectories(Project project, CloverPluginConvention cloverPluginConvention, Task task) {
        def testSrcDirs = [] as Set<File>
		
		if (!task instanceof Test) {
			return testSrcDirs
		}

        //default test task
        if (task.testSrcDirs as Set<File> == project.sourceSets.test.java.srcDirs) {
			if (!hasSourceSets(project)) {
				// Do nothing ( by intention! )
			} else if(hasGroovyPlugin(project)) {
                addExistingSourceDirectories(testSrcDirs, project.sourceSets.test.java.srcDirs)
                addExistingSourceDirectories(testSrcDirs, project.sourceSets.test.groovy.srcDirs)
            }
            else if(hasJavaPlugin(project)) {
                addExistingSourceDirectories(testSrcDirs, project.sourceSets.test.java.srcDirs)
            }
        } else {
            addExistingSourceDirectories(testSrcDirs, task.testSrcDirs as Set)
        }

        if(cloverPluginConvention.additionalTestDirs) {
            addExistingSourceDirectories(testSrcDirs, cloverPluginConvention.additionalTestDirs)
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
     * Checks to see if Java plugin got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    private boolean hasJavaPlugin(Project project) {
        project.plugins.hasPlugin(JavaPlugin)
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
	
	/**
	 * Returns true if the project has any source sets defined
	 * @param project the project to check
	 * @return see above
	 */
	private boolean hasSourceSets(Project project) {
		return project.getProperties().containsKey("sourceSets")
	}
	
	/**
	 * Gets testCompile classpath which consists of the existing testCompile configuration FileTree, the Clover
	 * configuration FileTree and the classpath FileTree from plusConfigurations.
	 *
	 * @param project Project
	 * @return File collection
	 */
	private FileCollection getTestCompileClasspath(Project project, CloverPluginConvention cloverPluginConvention) {
		project.configurations.testCompile.asFileTree + project.configurations.getByName(CONFIGURATION_NAME).asFileTree + getPlusClasspaths(project, cloverPluginConvention)
	}

    private FileCollection getGroovyClasspath(Project project) {
        if(project.configurations.findByName('groovy')?.files.size() > 0) {
            return project.configurations.groovy.asFileTree
        }

        project.configurations.compile.asFileTree
    }
	
	/**
	 * Gets compile classpath which consists of the existing compile configuration FileTree, the Clover
	 * configuration FileTree and the classpath FileTree from plusConfigurations.
	 *
	 * @param project Project
	 * @return File collection
	 */
	private FileCollection getCompileClasspath(Project project,  CloverPluginConvention cloverPluginConvention) {
		project.configurations.compile.asFileTree + project.configurations.getByName(CONFIGURATION_NAME).asFileTree + getPlusClasspaths(project, cloverPluginConvention)
	}
	
	private FileTree getPlusClasspaths(Project project,  CloverPluginConvention cloverPluginConvention) {
		FileTree plusClasspaths = project.files().asFileTree
		cloverPluginConvention.classpath.plusConfigurations.each {
			plusClasspaths += it.asFileTree
		}
		return plusClasspaths.asFileTree
	}

	private boolean isMainSourceSetEmpty(Project project) {
		if (!project.hasProperty("sourceSets")) {
			return true
		}
		SourceSet main = project.sourceSets.main
		if (main == null) {
			return true
		}
		File file = main.allSource.find { File file ->
			!file.path.contains("resources")
		}
		if (file == null) {
			return true;
		}
		else return false;
	}
}