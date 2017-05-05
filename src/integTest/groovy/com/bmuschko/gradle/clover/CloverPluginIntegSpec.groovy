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

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import spock.lang.Specification
import spock.lang.Unroll

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
        coverage.project.metrics.@methods == '2'
        coverage.project.metrics.@coveredmethods == '1'
        coverage.testproject.metrics.@classes == '1'
        coverage.testproject.metrics.@methods == '1'
        coverage.testproject.metrics.@coveredmethods == '1'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

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

    def "Build a Java project with disabled instrumentation"() {
        given: "a Java project"
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
        coverage.project.metrics.@methods == '1'
        coverage.project.metrics.@coveredmethods == '1'
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
            builder.forTasks(tasks).setStandardOutput(stream).run()
            output = stream.toString()
        }
        finally {
            conn?.close()
        }
    }
}
