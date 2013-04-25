package org.gradle.api.plugins.clover

import org.gradle.api.Action
import org.gradle.api.Task

class RestoreOriginalClassesAction implements Action<Task> {
    File classesDir
    File testClassesDir
    File classesBackupDir
    File testClassesBackupDir


    @Override
    void execute(Task t) {
        def ant = new AntBuilder()
        ant.delete(includeEmptyDirs: true) {
            fileset(dir: getClassesDir().canonicalPath, includes: '**/*')
        }
        ant.delete(includeEmptyDirs: true) {
            fileset(dir: getTestClassesDir().canonicalPath, includes: '**/*')
        }
        ant.move(file: getClassesBackupDir().canonicalPath, tofile: getClassesDir().canonicalPath, failonerror: true)
        ant.move(file: getTestClassesBackupDir().canonicalPath, tofile: getTestClassesDir().canonicalPath, failonerror: false)
    }
}
