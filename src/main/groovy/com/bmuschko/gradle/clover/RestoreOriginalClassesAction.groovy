package com.bmuschko.gradle.clover

import org.gradle.api.Action
import org.gradle.api.Task

import groovy.transform.CompileStatic

class RestoreOriginalClassesAction implements Action<Task> {
    List<CloverSourceSet> sourceSets
    List<CloverSourceSet> testSourceSets

    @CompileStatic
    @Override
    void execute(Task t) {
        def ant = new AntBuilder()

        deleteAllClassesDirectories(ant, getSourceSets())
        deleteAllClassesDirectories(ant, getTestSourceSets())
        moveAllBackupDirsToClassesDirs(ant, getSourceSets())
        moveAllBackupDirsToClassesDirs(ant, getTestSourceSets())
    }

    @CompileStatic
    private void deleteAllClassesDirectories(AntBuilder ant, List<CloverSourceSet> sourceSets) {
        for(CloverSourceSet sourceSet : sourceSets) {
            if (CloverSourceSetUtils.existsDirectory(sourceSet.classesDir)) {
                deleteClassesDirectory(ant, sourceSet.classesDir)
            }
         }
    }

    private void deleteClassesDirectory(AntBuilder ant, File classesDir) {
         ant.delete(includeEmptyDirs: true) {
            fileset(dir: classesDir.canonicalPath, includes: '**/*')
        }
    }

    @CompileStatic
    private void moveAllBackupDirsToClassesDirs(AntBuilder ant, List<CloverSourceSet> sourceSets) {
        for(CloverSourceSet sourceSet : sourceSets) {
            moveBackupToClassesDir(ant, sourceSet.backupDir, sourceSet.classesDir)
        }
    }

    private void moveBackupToClassesDir(AntBuilder ant, File backupDir, File classesDir) {
        if (CloverSourceSetUtils.existsDirectory(backupDir)) {
            ant.move(file: backupDir.canonicalPath, tofile: classesDir.canonicalPath, failonerror: true)
         }
    }

}
