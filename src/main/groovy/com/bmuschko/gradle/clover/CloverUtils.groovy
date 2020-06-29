package com.bmuschko.gradle.clover

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.GroovyPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.util.GradleVersion


class CloverUtils {
    static final String DEFAULT_CLOVER_DATABASE = '.clover/clover.db'
    static final String DEFAULT_JAVA_INCLUDES = '**/*.java'
    static final String DEFAULT_GROOVY_INCLUDES = '**/*.groovy'
    static final String DEFAULT_JAVA_TEST_INCLUDES = '**/*Test.java'
    static final String DEFAULT_GROOVY_TEST_INCLUDES = '**/*Test.groovy'
    static final String DEFAULT_SPOCK_TEST_INCLUDES = '**/*Spec.groovy'

    /**
     * Gets init String that determines location of Clover database.
     *
     * @param cloverPluginConvention Clover plugin convention
     * @return Init String
     */
    @CompileStatic
    static String getInitString(CloverPluginConvention cloverPluginConvention) {
        cloverPluginConvention.initString ?: DEFAULT_CLOVER_DATABASE
    }

    @CompileStatic
    static String getInitString(CloverPluginConvention cloverPluginConvention, Task testTask) {
        "${getInitString(cloverPluginConvention)}-${testTask.name}"
    }

    /**
     * Checks to see if Groovy or Grails plugins got applied to project.
     *
     * @param project Project
     * @return Flag
     */
    @CompileStatic
    static boolean hasGroovyPlugin(Project project) {
        project.plugins.hasPlugin(GroovyPlugin) ||
            project.plugins.hasPlugin('org.grails.grails-core') ||
            project.plugins.hasPlugin('org.grails.grails-plugin') ||
            project.plugins.hasPlugin('org.grails.grails-web')
    }

    @CompileStatic
    static FileCollection getTestRuntimeClasspath(Project project, Test testTask) {
        testTask.classpath.filter { File file -> !file.directory } + project.configurations.getByName(CloverPlugin.CONFIGURATION_NAME)
    }

    static FileCollection getGroovyClasspath(Project project) {
        // We use the test sourceSet to derive the GroovyCompile built-in task name
        // and from there extract the correct GroovyClasspath. This is more closely
        // matched to the built-in Groovy compiler and still supports a build dependency.
        def taskName = project.sourceSets.test.getCompileTaskName('groovy')
        def task = project.tasks.findByName(taskName)
        if (task == null) {
            // Fall back to main source set to get this. We should have this
            // or the test source set using Groovy if this method is called.
            taskName = project.sourceSets.main.getCompileTaskName('groovy')
            task = project.tasks.findByName(taskName)
        }
        if (task == null) {
            return project.configurations.getByName(CloverPlugin.CONFIGURATION_NAME)
        }
        task.getGroovyClasspath() + project.configurations.getByName(CloverPlugin.CONFIGURATION_NAME)
    }

    static String getSourceCompatibility(Project project, CloverPluginConvention cloverPluginConvention) {
        if (cloverPluginConvention.compiler.sourceCompatibility) {
            return cloverPluginConvention.compiler.sourceCompatibility
        }

        return project.sourceCompatibility?.toString()
    }

    static String getTargetCompatibility(Project project, CloverPluginConvention cloverPluginConvention) {
        if (cloverPluginConvention.compiler.targetCompatibility) {
            return cloverPluginConvention.compiler.targetCompatibility
        }

        return project.targetCompatibility?.toString()
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
    static List getIncludes(Project project, CloverPluginConvention cloverPluginConvention) {
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
    static List getTestIncludes(Project project, CloverPluginConvention cloverPluginConvention) {
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
    static List getTestExcludes(Project project, CloverPluginConvention cloverPluginConvention) {
        if (cloverPluginConvention.testExcludes) {
            return cloverPluginConvention.testExcludes
        }

        []
    }

    /**
     * Creates an instance of the specified class, using an ASM-backed class generator.
     *
     * @param clazz the type of object to create
     * @return an instance of the specified type
     */
    @CompileDynamic
    static <T> T createInstance(Project project, Class<T> clazz) {
        if (GradleVersion.version('4.0').compareTo(GradleVersion.current()) < 0) {
            return project.objects.newInstance(clazz)
        }
        // If we are building in Gradle 3.x or older use the old mechanism
        def generator = Class.forName('org.gradle.api.internal.AsmBackedClassGenerator').getConstructor().newInstance()
        return generator.generate(clazz).getConstructor().newInstance()
    }
}
