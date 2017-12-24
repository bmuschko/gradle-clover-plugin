package com.bmuschko.gradle.clover

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class AggregateDatabasesTask extends DefaultTask {
    /**
     * Classpath containing Clover Ant tasks.
     */
    @InputFiles
    FileCollection cloverClasspath

    /**
     * The location to write the Clover coverage database resulting from the merge.
     */
    @Input
    String initString

    @InputFiles
    List<File> cloverDbFiles = new ArrayList<File>()

    @OutputFile
    File getAggregationFile() {
        new File(project.buildDir, getInitString())
    }

    void aggregate(Task testTask) {
        dependsOn testTask
        cloverDbFiles << new File("${aggregationFile.canonicalPath}-${testTask.name}")
    }

    @TaskAction
    void aggregateDatabases() {
        if (existsAtLeastOneCloverDbFile(cloverDbFiles)) {
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)

            ant.'clover-merge'(initString: aggregationFile.canonicalPath) {
                cloverDbFiles.each { cloverDbFile ->
                    if (cloverDbFile.exists()) {
                        ant.cloverDb(initString: cloverDbFile.canonicalPath)
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
    private boolean existsAtLeastOneCloverDbFile(List<File> cloverDbFiles) {
         cloverDbFiles.findAll { cloverDbFile -> cloverDbFile.exists() }.size() > 0
    }
}