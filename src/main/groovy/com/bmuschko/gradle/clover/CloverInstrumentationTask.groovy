package com.bmuschko.gradle.clover

import static com.bmuschko.gradle.clover.CloverUtils.*

import javax.inject.Inject

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

import com.bmuschko.gradle.clover.CloverPlugin.SourceSetsResolver

@CacheableTask
class CloverInstrumentationTask extends DefaultTask {
    @Nested
    final InstrumentCodeAction instrumentCodeAction

    @Internal
    final CloverPluginConvention cloverPluginConvention

    @Internal
    final Test testTask

    @Internal
    final SourceSetsResolver resolver
    
    @Inject
    CloverInstrumentationTask(CloverPluginConvention cloverPluginConvention, Test testTask, SourceSetsResolver resolver) {
        this.instrumentCodeAction = project.objects.newInstance(InstrumentCodeAction)
        this.cloverPluginConvention = cloverPluginConvention
        this.testTask = testTask
        this.resolver = resolver

        this.dependsOn(testTask.testClassesDirs)

        instrumentCodeAction.conventionMapping.with {
            map('initString') { getInitString(cloverPluginConvention, testTask) }
            map('enabled') { cloverPluginConvention.enabled }
            map('compileGroovy') { hasGroovyPlugin(project) }
            map('cloverClasspath') { project.configurations.getByName(CloverPlugin.CONFIGURATION_NAME).asFileTree }
            map('instrumentationClasspath') { getInstrumentationClasspath(project, testTask).asFileTree }
            map('groovyClasspath') { getGroovyClasspath(project) }
            map('buildDir') { project.buildDir }
            map('sourceSets') { resolver.getSourceSets(testTask) }
            map('testSourceSets') { resolver.getTestSourceSets(testTask) }
            map('sourceCompatibility') { getSourceCompatibility(project, cloverPluginConvention) }
            map('targetCompatibility') { getTargetCompatibility(project, cloverPluginConvention) }
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
            map('additionalGroovycOpts') { cloverPluginConvention.compiler.additionalGroovycOpts }
        }
    }

    @TaskAction
    void instrumentCode() {
        instrumentCodeAction.execute(this)
    }

    @Internal
    FileCollection getInstrumentedMainClasses() {
        return project.files({ instrumentCodeAction.sourceSets.collect { it.instrumentedClassesDir } }) { builtBy this }
    }

    @Internal
    FileCollection getInstrumentedTestClasses() {
        return project.files({ instrumentCodeAction.testSourceSets.collect { it.instrumentedClassesDir } }) { builtBy this }
    }

    @Internal
    FileCollection getOriginalMainClasses() {
        return project.files { instrumentCodeAction.sourceSets.collect { it.classesDir } }
    }

    @Internal
    FileCollection getOriginalTestClasses() {
        return project.files { instrumentCodeAction.testSourceSets.collect { it.classesDir } }
    }

    @OutputFile
    File getCloverDatabaseFile() {
        return instrumentCodeAction.cloverDatabaseFile
    }
}
