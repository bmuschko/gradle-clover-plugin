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
package org.gradle.api.plugins.clover

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
            cloverReport.exists()
            def coverage = new XmlSlurper().parse(cloverReport)
            coverage.project.metrics.@classes == '1'
            coverage.project.metrics.@methods == '2'
            coverage.project.metrics.@coveredmethods == '1'
            coverage.testproject.metrics.@classes == '1'
            coverage.testproject.metrics.@methods == '1'
            coverage.testproject.metrics.@coveredmethods == '1'

        and: "the Clover snapshot is not generated because test optimization is not enabled"
            cloverSnapshot.exists() == false
    }

    def "Build a Groovy project"() {

        given: "a Groovy project"
            projectName = 'groovy-project'

        when: "the Clover report generation task is run"
            runTasks('clean', 'cloverGenerateReport')

        then: "the Clover coverage database is generated"
            cloverDb.exists()

        and: "the Clover report is generated and is correct"
            cloverReport.exists()
            def coverage = new XmlSlurper().parse(cloverReport)
            coverage.project.metrics.@classes == '1'
            coverage.project.metrics.@methods == '2'
            coverage.project.metrics.@coveredmethods == '1'
            coverage.testproject.metrics.@classes == '2'
            coverage.testproject.metrics.@methods == '2'
            coverage.testproject.metrics.@coveredmethods == '2'

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
            cloverReport.exists()
            def coverage = new XmlSlurper().parse(cloverReport)
            coverage.project.metrics.@classes == '1'
            coverage.project.metrics.@methods == '2'
            coverage.project.metrics.@coveredmethods == '1'
            coverage.testproject.metrics.@classes == '1'
            coverage.testproject.metrics.@methods == '1'
            coverage.testproject.metrics.@coveredmethods == '1'

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
            cloverReport.exists()
            def coverage2 = new XmlSlurper().parse(cloverReport)
            coverage2.project.metrics.@classes == '1'
            coverage2.project.metrics.@methods == '2'
            coverage2.project.metrics.@coveredmethods == '1'
            coverage2.testproject.metrics.@classes == '1'
            coverage2.testproject.metrics.@methods == '1'
            coverage2.testproject.metrics.@coveredmethods == '1'

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
            cloverReport.exists()
            def coverage = new XmlSlurper().parse(cloverReport)
            coverage.project.package.file[0].@name == 'Car.java'
            coverage.project.package.file[1].@name == 'Truck.java'
            coverage.testproject.package.file[0].@name == 'CarTest.java'
            coverage.testproject.package.file[1].@name == 'TruckTest.java'

        and: "the Clover snapshot is not generated because test optimization is not enabled"
            cloverSnapshot.exists() == false
    }

    @Unroll
    def "Build a Java multi-test-task project #scenario"() {
        given: "a Java multi-test-task project"
        projectName = 'java-multi-test-task-project'

        when: "the top-level Clover aggregation task is run"
        runTasks(['clean', 'cloverGenerateReport']) {
            withArguments '-b', "${buildFile}.gradle"
        }

        then: "the aggregated Clover coverage database is generated"
        cloverDb.exists()

        and: "the aggregated Clover report is generated and is correct"
        cloverReport.exists()
        def coverage = new XmlSlurper().parse(cloverReport)
        def carCoverage = coverage.project.package.file.find { it.@name.text() == 'Car.java' }
        carCoverage.line.find { it.@num.text() == "21" }.@count.text() == startMethodCoverage
        carCoverage.line.find { it.@num.text() == "26" }.@count.text() == stopMethodCoverage

        where:
        scenario        | buildFile | startMethodCoverage | stopMethodCoverage
        'for all tasks' | 'common'  | '1'                 | '1'
        'only for test' | 'onlyFor' | '1'                 | '0'
        'except test'   | 'except'  | '0'                 | '1'
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

    private File getCloverReport() {
        new File(projectDir, 'build/reports/clover/clover.xml')
    }

    private void runTasks(List<String> tasks, Closure builderConfigurationClosure) {
        ProjectConnection conn = GradleConnector.newConnector().forProjectDirectory(projectDir).connect()

        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream()
            def builder = conn.newBuild()
            if (builderConfigurationClosure) {
                builderConfigurationClosure.delegate = builder
                builderConfigurationClosure.resolveStrategy = Closure.DELEGATE_FIRST
                builderConfigurationClosure.call()
            }
            builder.forTasks(*tasks).setStandardOutput(stream).run()
            output = stream.toString()
        }
        finally {
            conn.close()
        }
    }

    private void runTasks(String... tasks) {
        runTasks(tasks as List, null)
    }
}
