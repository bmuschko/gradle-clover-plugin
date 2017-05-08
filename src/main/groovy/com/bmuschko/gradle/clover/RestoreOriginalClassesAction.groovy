package com.bmuschko.gradle.clover

import org.gradle.api.Action
import org.gradle.api.Task

class RestoreOriginalClassesAction implements Action<Task> {
    Set<CloverSourceSet> sourceSets
    Set<CloverSourceSet> testSourceSets


    @Override
    void execute(Task t) {
        def ant = new AntBuilder()

        deleteAllClassesDirectories(ant, getSourceSets())
        deleteAllClassesDirectories(ant, getTestSourceSets())
        moveAllBackupDirsToClassesDirs(ant, getSourceSets())
        moveAllBackupDirsToClassesDirs(ant, getTestSourceSets())
    }

    private void deleteAllClassesDirectories(AntBuilder ant, Set<CloverSourceSet> sourceSets) {
        for(CloverSourceSet sourceSet : sourceSets) {
            deleteClassesDirectory(ant, sourceSet.classesDir)
         }
    }

    private void deleteClassesDirectory(AntBuilder ant, File classesDir) {
         ant.delete(includeEmptyDirs: true) {
            fileset(dir: classesDir.canonicalPath, includes: '**/*')
        }
    }

    private void moveAllBackupDirsToClassesDirs(AntBuilder ant, Set<CloverSourceSet> sourceSets) {
        for(CloverSourceSet sourceSet : sourceSets) {
            moveBackupToClassesDir(ant, sourceSet.backupDir, sourceSet.classesDir)
        }
    }

    private void moveBackupToClassesDir(AntBuilder ant, File backupDir, File classesDir) {
        if(CloverSourceSetUtils.existsDirectory(backupDir)) {
            ant.move(file: backupDir.canonicalPath, tofile: classesDir.canonicalPath, failonerror: true)
         }
    }

}
