package com.bmuschko.gradle.clover

import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity

import java.util.concurrent.Callable

import org.gradle.api.file.FileCollection

import groovy.transform.CompileStatic

@CompileStatic
class CloverSourceSet {
    @Internal
    final UUID uuid = UUID.randomUUID()

    String name

    @InputFiles @PathSensitive(PathSensitivity.RELATIVE) Collection<File> srcDirs = new HashSet<File>()

    @Internal File classesDir
    void setClassesDir(File classesDir) {
        this.classesDir = classesDir
    }

    // Workaround for limitation of @Optional (see https://github.com/gradle/gradle/issues/2016)
    @InputDirectory @Optional @PathSensitive(PathSensitivity.RELATIVE)
    File getClassesDirIfExists() {
        return classesDir.exists() ? classesDir : null
    }

    @OutputDirectory File instrumentedClassesDir
    void setInstrumentedClassesDir(File instrumentedClassesDir) {
        this.instrumentedClassesDir = instrumentedClassesDir
    }

    private boolean groovy = false
    @Input boolean isGroovy() {
        groovy
    }
    void setGroovy(boolean groovy) {
        this.groovy = groovy
    }

    private transient Callable<FileCollection> classpathProvider

    @Internal
    Callable<FileCollection> getClasspathProvider() {
        return classpathProvider
    }

    void setClasspathProvider(Callable<FileCollection> classpathProvider) {
        this.classpathProvider = classpathProvider
    }

    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    FileCollection getCompileClasspath() {
        return classpathProvider.call()
    }

    @Internal
    String getName() {
        return name == null ? uuid.toString() : name
    }


    static CloverSourceSet from(ObjectFactory objects, CloverSourceSet other) {
        CloverSourceSet newSourceSet = objects.newInstance(CloverSourceSet)
        newSourceSet.with {
            name = other.name
            groovy = other.groovy
            srcDirs = other.srcDirs
            classesDir = other.classesDir
        }
        return newSourceSet
    }

    @Override
    String toString() {
        return "CloverSourceSet{" +
                "name='" + name + '\'' +
                ", groovy=" + groovy +
                ", srcDirs=" + srcDirs +
                ", classesDir=" + classesDir +
                ", instrumentedClassesDir=" + instrumentedClassesDir +
                '}'
    }
}
