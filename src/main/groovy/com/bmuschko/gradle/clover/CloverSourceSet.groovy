package com.bmuschko.gradle.clover

import java.util.concurrent.Callable

import org.gradle.api.file.FileCollection

class CloverSourceSet implements Serializable {
    static final long serialVersionUID = 7526472295622776147L
    def srcDirs = [] as Set<File>
    File classesDir
    File backupDir
    private transient Callable<FileCollection> classpathProvider

    FileCollection getCompileClasspath() {
        return classpathProvider.call()
    }

    @Override
    String toString() {
        "CloverSourceSet{" +
        "srcDirs=" + srcDirs +
        ", classesDir=" + classesDir +
        ", backupDir=" + backupDir +
        '}'
    }
}
