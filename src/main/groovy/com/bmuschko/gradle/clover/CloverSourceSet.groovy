package com.bmuschko.gradle.clover

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSetOutput

import java.util.concurrent.Callable

import org.gradle.api.file.FileCollection

import groovy.transform.CompileStatic
import groovy.transform.ToString

@CompileStatic
@ToString
class CloverSourceSet {
    private final SourceSetOutput originalSourceSetOutput

    CloverSourceSet(boolean groovy = false, SourceSetOutput originalSourceSetOutput = null) {
        this.groovy = groovy
        this.originalSourceSetOutput = originalSourceSetOutput
    }

    @Internal Collection<File> srcDirs = new HashSet<File>()

    @InputFiles
    @Optional
    FileCollection getOriginalSourceSetOutputDirectories() {
        return originalSourceSetOutput == null ? null : originalSourceSetOutput.classesDirs.filter { File file -> file.exists() }
    }

    @Internal File classesDir
    void setClassesDir(File classesDir) {
        this.classesDir = classesDir
        this.backupDir = new File("${classesDir}-bak")
    }

    private File backupDir
    @OutputDirectory File getBackupDir() {
        backupDir
    }

    private boolean groovy
    @Input boolean isGroovy() {
        groovy
    }
    void setGroovy(boolean groovy) {
        this.groovy = groovy
    }

    private transient Callable<FileCollection> classpathProvider

    @InputFiles
    FileCollection getCompileClasspath() {
        return classpathProvider.call()
    }
}
