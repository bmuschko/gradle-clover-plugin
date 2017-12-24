package com.bmuschko.gradle.clover

import java.util.concurrent.Callable

import org.gradle.api.file.FileCollection

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString
class CloverSourceSet implements Serializable {
    static final long serialVersionUID = 1L

    CloverSourceSet(boolean groovy = false) {
        this.groovy = groovy
    }

    Collection<File> srcDirs = new HashSet<File>()

    File classesDir
    void setClassesDir(File classesDir) {
        this.classesDir = classesDir
        this.backupDir = new File("${classesDir}-bak")
    }

    private File backupDir
    File getBackupDir() {
        backupDir
    }

    private boolean groovy
    boolean isGroovy() {
        groovy
    }
    void setGroovy(boolean groovy) {
        this.groovy = groovy
    }

    private transient Callable<FileCollection> classpathProvider

    FileCollection getCompileClasspath() {
        return classpathProvider.call()
    }
}
