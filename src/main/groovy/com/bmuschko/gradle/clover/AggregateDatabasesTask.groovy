package com.bmuschko.gradle.clover

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
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

    /**
     * Mandatory Clover license file.
     */
    @InputFile
    File licenseFile

    List<Task> testTasks = new ArrayList<Task>()

    void aggregate(Task testTask) {
        dependsOn testTask
        testTasks << testTask
    }

    @TaskAction
    void aggregateDatabases() {
        File aggregationFile = new File(project.buildDir, getInitString())
        List<File> cloverDbFiles = getTestTaskCloverDbFiles(aggregationFile)

        if(existsAtLeastOneCloverDbFile(cloverDbFiles)) {
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)

            ant.'clover-merge'(initString: aggregationFile.canonicalPath) {
                cloverDbFiles.each { cloverDbFile ->
                    if(cloverDbFile.exists()) {
                        ant.cloverDb(initString: cloverDbFile.canonicalPath)
                    }
                }
            }
        }
    }

    /**
     * Test task Clover database files.
     *
     * @param aggregationFile Aggregation file
     * @return Clover database files
     */
    private List<File> getTestTaskCloverDbFiles(File aggregationFile) {
        testTasks.collect { new File("${aggregationFile.canonicalPath}-${it.name}") }
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