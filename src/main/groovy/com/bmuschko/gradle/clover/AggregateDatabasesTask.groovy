package com.bmuschko.gradle.clover

import javax.inject.Inject

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.project.IsolatedAntBuilder
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

@CacheableTask
class AggregateDatabasesTask extends DefaultTask {
    /**
     * Classpath containing Clover Ant tasks.
     */
    @Classpath
    FileCollection cloverClasspath

    /**
     * The location to write the Clover coverage database resulting from the merge.
     */
    @Input
    String initString

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    FileCollection cloverDbFiles = project.files()

    @OutputFile
    File getAggregationFile() {
        new File(project.buildDir, getInitString())
    }

    void aggregate(Test testTask) {
        dependsOn(testTask)
        cloverDbFiles.from(testTask.ext.cloverDatabaseFile)
    }
    
    @Inject
    IsolatedAntBuilder getAntBuilder() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    void aggregateDatabases() {
        if (existsAtLeastOneCloverDbFile(cloverDbFiles)) {
            antBuilder.withClasspath(getCloverClasspath().files).execute {
                CloverUtils.injectCloverClasspath(ant.getBuilder(), getCloverClasspath().files)
                CloverUtils.loadCloverlib(ant.getBuilder())
                
                ant.'clover-merge'(initString: aggregationFile.canonicalPath) {
                    cloverDbFiles.each { cloverDbFile ->
                        if (cloverDbFile.exists()) {
                            ant.cloverDb(initString: cloverDbFile.canonicalPath)
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if at least one Clover database file exists.
     *
     * @param cloverDbFiles Clover database files
     * @return Flag
     */
    private static boolean existsAtLeastOneCloverDbFile(FileCollection cloverDbFiles) {
         cloverDbFiles.findAll { cloverDbFile -> cloverDbFile.exists() }.size() > 0
    }
}
