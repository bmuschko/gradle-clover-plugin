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

/**
 * Some integration tests for the Clover plugin.
 *
 * @author Daniel Gredler
 */
class CloverPluginTest extends Specification {

    /* The name of the project currently being tested. */
    String projectName

    /* The standard output from the last time that runTasks() was called. */
    String output

    def "Build a Java project"() {

        given: "a Java project"
            projectName = 'project1'

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
            projectName = 'project2'

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
            projectName = 'project3'
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

        and: "the Clover report wasn't generated"
            cloverReport.exists() == false

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
            projectName = 'project4'

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

    private File getProjectDir() {
        new File('src/test/resources', projectName)
    }

    private File getCloverDb() {
        new File(projectDir, 'build/.clover/clover.db')
    }

    private File getCloverSnapshot() {
        new File(projectDir, '.clover/coverage.db.snapshot')
    }

    private File getCloverReport() {
        new File(projectDir, 'build/reports/clover/clover.xml')
    }

    private void runTasks(String... tasks) {
        ProjectConnection conn = GradleConnector.newConnector().forProjectDirectory(projectDir).connect()
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream()
            conn.newBuild().forTasks(tasks).setStandardOutput(stream).run()
            output = stream.toString()
        } finally {
            conn.close()
        }
    }
}
