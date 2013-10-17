package org.gradle.api.plugins.clover

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

class AggregateDatabasesTask extends DefaultTask {
    FileCollection cloverClasspath
    String initString
    File licenseFile
    List<Task> testTasks = new ArrayList<Task>()

    void aggregate(Task testTask) {
        dependsOn testTask
        testTasks << testTask
    }

    @TaskAction
    void aggregateDatabases() {
        if (testTasks) {
            ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
            ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)

            File cloverDb = new File("${project.buildDir.canonicalPath}/${getInitString()}")

            if (cloverDb.exists()) {
                ant.'clover-merge'(initString: "${cloverDb.canonicalPath}") {
                    testTasks.each {
                        ant.cloverDb(initString: "${cloverDb.canonicalPath}-${it.name}")
                    }
                }
            } else {
                logger.debug "Unable to find Clover DB file $cloverDb; $project.name may not have any tests."
            }
        }
    }
}
