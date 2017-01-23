package com.bmuschko.gradle.clover

class CloverSourceSet implements Serializable {
    static final long serialVersionUID = 7526472295622776147L
    def srcDirs = [] as Set<File>
    File classesDir
    File backupDir

    @Override
    String toString() {
        "CloverSourceSet{" +
        "srcDirs=" + srcDirs +
        ", classesDir=" + classesDir +
        ", backupDir=" + backupDir +
        '}'
    }
}
