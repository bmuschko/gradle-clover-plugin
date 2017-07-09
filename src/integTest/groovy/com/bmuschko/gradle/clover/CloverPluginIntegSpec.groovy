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

import org.apache.commons.io.IOUtils
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import spock.lang.Specification
import spock.lang.Unroll

import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Some integration tests for the Clover plugin.
 *
 * @author Daniel Gredler
 */
class CloverPluginIntegSpec extends Specification {
    /* The name of the project currently being tested. */
    String projectName

    /* The standard output from the last time that runTasks() was called. */
    String output

    def "Build a Java project"() {
        given: "a Java project"
        projectName = 'java-project'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.metrics.@classes == '1'
        coverage.project.metrics.@methods == '3'
        coverage.project.metrics.@coveredmethods == '2'
        coverage.testproject.metrics.@files == '1'
        coverage.testproject.metrics.@classes == '1'
        coverage.testproject.metrics.@methods == '2'
        coverage.testproject.metrics.@coveredmethods == '2'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()
        cloverHistoricalHtmlReport.exists()
        cloverHistoricalPdfReport.exists()

        and: "the Clover snapshot is not generated because test optimization is not enabled"
        cloverSnapshot.exists() == false

        when: "the Clover report generation task is run a second time without cleaning but the reports deleted"
        cloverXmlReport.delete()
        cloverHtmlReport.delete()
        cloverJsonReport.delete()
        cloverPdfReport.delete()
        runTasks('cloverGenerateReport')

        then: "the Clover reports are generated again"
        cloverXmlReport.exists()
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()
    }

    def "Build a Java project with method filters"() {
        given: "a Java project with method filters"
        projectName = 'java-project-with-method-filters'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.metrics.@classes == '1'
        coverage.project.metrics.@methods == '2'
        coverage.project.metrics.@coveredmethods == '1'
        coverage.testproject.metrics.@files == '1'
        coverage.testproject.metrics.@classes == '1'
        coverage.testproject.metrics.@methods == '1'
        coverage.testproject.metrics.@coveredmethods == '1'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()
    }

    def "Build a Java project with additional tests"() {
        given: "a Java project with additional tests"
        projectName = 'java-project-with-additional-tests'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.metrics.@classes == '1'
        coverage.project.metrics.@methods == '2'
        coverage.project.metrics.@coveredmethods == '1'
        coverage.testproject.metrics.@files == '2'
        coverage.testproject.metrics.@classes == '2'
        coverage.testproject.metrics.@methods == '2'
        coverage.testproject.metrics.@coveredmethods == '2'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()
    }

    def "Build a Java project with lambdas"() {
        given: "a Java project with lambdas"
        projectName = 'java-project-lambdas'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
    }

    def "Build a Java and Groovy project with debug mode enabled"() {
        given: "a Java and Groovy project"
        projectName = 'java-groovy-project-debug-enabled'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
    }

    def "Build a Java Spring project with debug mode enabled"() {
        given: "a Java Spring project"
        projectName = 'spring-project-with-clover'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
    }

    def "Build a Java project with disabled instrumentation"() {
        given: "a Java project with disabled instrumentation"
        projectName = 'java-project-disabled-instrumentation'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        !cloverDb.exists()

        and: "the Clover report is not generated"
        !cloverXmlReport.exists()
        !cloverHtmlReport.exists()
        !cloverJsonReport.exists()
        !cloverPdfReport.exists()

        and: "the Clover snapshot is not generated"
        !cloverSnapshot.exists()
    }

    def "Build a Java project with Groovy Tests"() {
        given: "a Java plus Groovy project"
        projectName = 'java-project-groovy-tests'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.metrics.@classes == '1'
        coverage.project.metrics.@methods == '2'
        coverage.project.metrics.@coveredmethods == '2'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        and: "the Clover snapshot is not generated because test optimization is not enabled"
        cloverSnapshot.exists() == false
    }

    def "Build a Groovy project using compile configuration"() {
        given: "a Groovy project"
        projectName = 'groovy-project-compile-config'

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.metrics.@classes == '1'
        coverage.project.metrics.@methods == '2'
        coverage.project.metrics.@coveredmethods == '1'
        coverage.testproject.metrics.@classes == '2'
        coverage.testproject.metrics.@methods == '2'
        coverage.testproject.metrics.@coveredmethods == '2'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        and: "the Clover snapshot is not generated because test optimization is not enabled"
        cloverSnapshot.exists() == false
    }

    def "Build a Java project with test optimization enabled"() {
        given: "a Java project with test optimization enabled"
        projectName = 'java-test-opt'
        File sourceFile = new File(projectDir, 'src/main/java/Book.java')

        when: "no task has been run yet"

        then: "the cross-build Clover snapshot doesn't exist or can be deleted"
        !cloverSnapshot.exists() || cloverSnapshot.delete()

        when: "the Clover report generation task is run"
        runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.metrics.@classes == '1'
        coverage.project.metrics.@methods == '2'
        coverage.project.metrics.@coveredmethods == '1'
        coverage.testproject.metrics.@classes == '1'
        coverage.testproject.metrics.@methods == '1'
        coverage.testproject.metrics.@coveredmethods == '1'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        and: "the Clover snapshot is generated"
        cloverSnapshot.exists()

        when: "no code is modified, and the Clover report generation task is run a second time"
        runTasks('clean', 'cloverGenerateReport')

        then: "no tests were run"
        output.contains('For the current test set, Clover detected no modified source files')
        output.contains('Clover included 0 test classes in this run (total # test classes : 1)')

        when: "the code is modified, and the Clover report generation task is run a third time"
        sourceFile << ' '
        runTasks('clean', 'cloverGenerateReport')

        then: "Clover detected one change and exactly one test was run"
        output.contains('Test BookTest.testOpen was affected by changed source: true')
        output.contains('Clover included 1 test class in this run (total # test classes : 1)')

        and: "the Clover report was generated and is correct"
        cloverXmlReport.exists()
        def coverage2 = new XmlSlurper().parse(cloverXmlReport)
        coverage2.project.metrics.@classes == '1'
        coverage2.project.metrics.@methods == '2'
        coverage2.project.metrics.@coveredmethods == '1'
        coverage2.testproject.metrics.@classes == '1'
        coverage2.testproject.metrics.@methods == '1'
        coverage2.testproject.metrics.@coveredmethods == '1'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        cleanup: "undo the source modification"
        sourceFile.text = sourceFile.text.trim()
    }

    def "Build a Java project with test optimization enabled and invalid useClover3 override"() {
        given: "a Java project with test optimization enabled"
        projectName = 'java-test-opt'
        File sourceFile = new File(projectDir, 'src/main/java/Book.java')
        List<String> args = [ '-b', 'invalid.gradle' ]

        when: "no task has been run yet"

        then: "the cross-build Clover snapshot doesn't exist or can be deleted"
        !cloverSnapshot.exists() || cloverSnapshot.delete()

        when: "the Clover report generation task is run"
        runTasks(args, 'clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover snapshot is generated"
        cloverSnapshot.exists()

        when: "no code is modified, and the Clover report generation task is run a second time"
        runTasks(args, 'clean', 'cloverGenerateReport')

        then: "Build fails with org.gradle.tooling.BuildException"
        thrown(org.gradle.tooling.BuildException)

        and: "Output has mention of cloverjunitlib.xml not found"
        output.contains('Could not load definitions from resource cloverjunitlib.xml. It could not be found.')

        cleanup: "undo the source modification"
        sourceFile.text = sourceFile.text.trim()
    }

    def "Build a Java multi-project"() {
        given: "a Java multi-project"
        projectName = 'java-multi-project'

        when: "the top-level Clover aggregation task is run"
        runTasks('clean', 'cloverAggregateReports')

        then: "the aggregated Clover coverage database is generated"
        cloverDb.exists()

        and: "the aggregated Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.package.file.size() == 2
        coverage.project.package.file[0].@name == 'Car.java'
        coverage.project.package.file[1].@name == 'Truck.java'
        !coverage.project.package.file*.@name.contains('Motorbike.java')
        coverage.testproject.package.file.size() == 2
        coverage.testproject.package.file[0].@name == 'CarTest.java'
        coverage.testproject.package.file[1].@name == 'TruckTest.java'
        !coverage.testproject.package.file*.@name.contains('MotorbikeTest.java')
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        and: "the Clover snapshot is not generated because test optimization is not enabled"
        cloverSnapshot.exists() == false
    }

    @Unroll
    def "Build a Java multi-test-task project #scenario"() {
        given: "a Java multi-test-task project"
        projectName = 'java-multi-test-task-project'

        when: "the top-level Clover aggregation task is run"
        runTasks(['-b', "${buildFile}.gradle"], 'clean', 'cloverGenerateReport')

        then: "the aggregated Clover coverage database is generated"
        cloverDb.exists()

        and: "the aggregated Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        def carCoverage = coverage.project.package.file.find { it.@name.text() == 'Car.java' }
        carCoverage.line.find { it.@num.text() == "21" }.@count.text() == startMethodCoverage
        carCoverage.line.find { it.@num.text() == "26" }.@count.text() == stopMethodCoverage
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        where:
        scenario        | buildFile | startMethodCoverage | stopMethodCoverage
        'for all tasks' | 'common'  | '1'                 | '1'
        'only for test' | 'include' | '1'                 | '0'
        'except test'   | 'exclude' | '0'                 | '1'
    }

    def "Build a Java multi-project with instrumented JARs in an EAR"() {
        given: "a Java multi-project with EAR"
            projectName = 'java-multi-project-with-ear'

        when: "the EAR is built with instrumentation"
            runTasks(["-PcloverInstrumentedJar"], 'clean', 'cloverAggregateReports', 'build')

        then: "the projectDriver JAR file exists"
            def driverJar = new File(projectDir, 'projectDriver/build/libs/projectDriver.jar')
            driverJar.exists()

        and: "the projectCar JAR file exists"
            def carJar = new File(projectDir, 'projectCar/build/libs/projectCar.jar')
            carJar.exists()

        and: "the EAR exists"
            def ear = new File(projectDir, 'projectEar/build/libs/projectEar.ear')
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
    }

    def "Build a Grails3 project with unit tests and Clover coverage report"() {
        given: "a Grails3 project with unit tests"
            projectName = 'grails-project'
    
        when: "the Clover report generation task is run"
            runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
            cloverDb.exists()

        and: "the Clover report is generated and is correct"
            cloverXmlReport.exists()
            def coverage = new XmlSlurper().parse(cloverXmlReport)
            coverage.project.metrics.@classes == '5'
            coverage.project.metrics.@methods == '6'
            coverage.project.metrics.@coveredmethods == '3'
            coverage.testproject.metrics.@classes == '2'
            coverage.testproject.metrics.@methods == '6'
            coverage.testproject.metrics.@coveredmethods == '6'
            cloverHtmlReport.exists()
            cloverJsonReport.exists()
            cloverPdfReport.exists()
    
        and: "the Clover snapshot is not generated because test optimization is not enabled"
            cloverSnapshot.exists() == false
    }
    
    private File getProjectDir() {
        new File('src/integTest/projects', projectName)
    }

    private File getCloverDb() {
        new File(projectDir, 'build/.clover/clover.db')
    }

    private File getCloverSnapshot() {
        new File(projectDir, '.clover/coverage.db.snapshot-test')
    }

    private File getCloverXmlReport() {
        new File(getReportsDir(), 'clover.xml')
    }

    private File getCloverHtmlReport() {
        new File(getReportsDir(), 'html')
    }

    private File getCloverJsonReport() {
        new File(getReportsDir(), 'json')
    }

    private File getCloverPdfReport() {
        new File(getReportsDir(), 'clover.pdf')
    }

    private File getCloverHistoricalHtmlReport() {
        new File(getReportsDir(), 'html/historical.html')
    }

    private File getCloverHistoricalPdfReport() {
        new File(getReportsDir(), 'historical.pdf')
    }

    private File getReportsDir() {
        new File(projectDir, 'build/reports/clover')
    }

    void runTasks(List<String> arguments = [], String... tasks) {
        ProjectConnection conn

        try {
            GradleConnector gradleConnector = GradleConnector.newConnector().forProjectDirectory(projectDir)
            conn = gradleConnector.connect()

            ByteArrayOutputStream stream = new ByteArrayOutputStream()
            def builder = conn.newBuild()
            if (arguments) {
                builder.withArguments(*arguments)
            }
            try {
                builder.forTasks(tasks).setStandardOutput(stream).run()
            } finally {
                output = stream.toString()
            }
        }
        finally {
            conn?.close()
        }
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
