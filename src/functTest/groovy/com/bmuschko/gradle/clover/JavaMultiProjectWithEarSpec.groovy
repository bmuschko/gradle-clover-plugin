/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bmuschko.gradle.clover

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import org.apache.commons.io.IOUtils

import spock.lang.Unroll

class JavaMultiProjectWithEarSpec extends AbstractFunctionalTestBase {

    @Unroll def"Build a Java multi-project with instrumented JARs in an EAR (with Gradle Version #gradle)"()
    {
        given: "a Java multi-project with EAR"
        projectName = 'java-multi-project-with-ear'
        gradleVersion = gradle

        when: "the EAR is built with instrumentation"
        build('--init-script', "$initScript", '-PcloverInstrumentedJar', 'clean', 'cloverAggregateReports', 'build')

        then: "the projectDriver JAR file exists"
        def driverJar = new File(buildDir, 'projectDriver/libs/projectDriver.jar')
        driverJar.exists()

        and: "the projectCar JAR file exists"
        def carJar = new File(buildDir, 'projectCar/libs/projectCar.jar')
        carJar.exists()

        and: "the EAR exists"
        def ear = new File(buildDir, 'projectEar/libs/projectEar.ear')
        ear.exists()

        and: "the projectDriver JAR file contains instrumented classes (file marker)"
        zipContains(driverJar, "clover.instrumented")

        and: "the projectCar JAR file contains instrumented classes (file marker)"
        zipContains(carJar, "clover.instrumented")

        and: "the EAR contains all the JAR files"
        zipContains(ear, "projectDriver.jar")
        zipContains(ear, "lib/projectCar.jar")

        and: "the JAR files in the EAR contain instrumented classes (file marker)"
        def driverJarInEar = getFromZip(ear, "projectDriver.jar")
        zipContains(driverJarInEar, "clover.instrumented")

        and: "the JAR files in the EAR contain instrumented classes (file marker)"
        def carJarInEar = getFromZip(ear, "lib/projectCar.jar")
        zipContains(carJarInEar, "clover.instrumented")

        where:
        gradle << GRADLE_TEST_VERSIONS
    }

    /**
     * @returns true if the zip file contains a file with the given name.
     */
    private boolean zipContains(File zip, String fileName) {
        ZipFile zipFile = new ZipFile(zip)
        try {
            return zipFile.getEntry(fileName) != null
        } finally {
            zipFile.close()
        }
    }

    private File getFromZip(File zip, String fileName) {
        ZipFile zipFile = new ZipFile(zip)
        try {
            ZipEntry zipEntry = zipFile.getEntry(fileName)
            assert zipEntry != null

            File newFile = File.createTempFile(zipEntry.name, null)
            FileOutputStream outputStream = new FileOutputStream(newFile)
            try {
                IOUtils.copy(zipFile.getInputStream(zipEntry), outputStream)
            } finally {
                outputStream.close()
            }
            return newFile
        } finally {
            zipFile.close()
        }
    }
}
