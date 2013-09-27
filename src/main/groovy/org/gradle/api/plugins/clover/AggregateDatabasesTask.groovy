package org.gradle.api.plugins.clover

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction

class AggregateDatabasesTask extends DefaultTask {
    FileCollection cloverClasspath
    String initString
    File licenseFile
    List<Task> tasks = new ArrayList<Task>()

    void aggregate(Task task) {
        dependsOn task
        tasks << task
    }

    @TaskAction
    void aggregateDatabases() {
        if (tasks) {
            Task taskWithCloverDb = tasks.find { new File("${project.buildDir.canonicalPath}/${getInitString()}-${it.name}").exists() }
            if (taskWithCloverDb != null) {
			
                ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
                ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
	
                ant.'clover-merge'(initString: "${project.buildDir.canonicalPath}/${getInitString()}") {
                    tasks.each {
                        File file = new File("${project.buildDir.canonicalPath}/${getInitString()}-${it.name}")
                        if (file.exists()) {
                            ant.cloverDb(initString: "${project.buildDir.canonicalPath}/${getInitString()}-${it.name}")
                        }
                    }
                }
            }
        }
    }
}
