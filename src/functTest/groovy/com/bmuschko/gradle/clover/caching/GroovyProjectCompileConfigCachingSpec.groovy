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
package com.bmuschko.gradle.clover.caching

import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class GroovyProjectCompileConfigCachingSpec extends AbstractCachingFunctionalSpec {

    @Unroll def "correctly caches build for a Groovy project with configuration changes (with Gradle Version #gradle)"() {
        given: "a Groovy project"
        withProjectTemplate('groovy-project-compile-config')
        gradleVersion = gradle

        when: "the Clover report generation task is run"
        BuildResult result = build('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks execute"
        assertTasksExecuted(result, ':compileGroovy', ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "that the gradle output contains the two files compiled by groovyc"
        with(result.output) {
            contains('Book.groovy')
            contains('BookGroovyTest.groovy')
        }

        when: "there are changes to the configuration"
        file('build.gradle') << """
            clover {
                compiler {
                    debug = true
                }
            }
        """
        result = build('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks execute again"
        assertTasksExecuted(result, ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')
        assertTasksCached(result, ':compileGroovy', ':compileTestGroovy')

        when: "the Clover report generation task is run without changes"
        result = build('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileGroovy', ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover coverage database is present"
        cloverDb.exists()

        and: "the Clover report is present and is correct"
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        cloverXmlReport.exists()
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

        when: "the build is executed in a relocated directory"
        result = relocateAndBuild('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileGroovy', ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover coverage database is present"
        relocatedCloverDb.exists()

        and: "the Clover report is present and is correct"
        def relocatedCoverage = new XmlSlurper().parse(relocatedCloverXmlReport)
        relocatedCloverXmlReport.exists()
        relocatedCoverage.project.metrics.@classes == '1'
        relocatedCoverage.project.metrics.@methods == '2'
        relocatedCoverage.project.metrics.@coveredmethods == '1'
        relocatedCoverage.testproject.metrics.@classes == '2'
        relocatedCoverage.testproject.metrics.@methods == '2'
        relocatedCoverage.testproject.metrics.@coveredmethods == '2'
        relocatedCloverHtmlReport.exists()
        relocatedCloverJsonReport.exists()
        relocatedCloverPdfReport.exists()

        where:
        gradle << GRADLE_TEST_VERSIONS
    }

    @Unroll def "correctly caches build for a Groovy project with source changes (with Gradle Version #gradle)"() {
        given: "a Groovy project"
        withProjectTemplate('groovy-project-compile-config')
        gradleVersion = gradle

        when: "the Clover report generation task is run"
        BuildResult result = build('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks execute"
        assertTasksExecuted(result, ':compileGroovy', ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "that the gradle output contains the two files compiled by groovyc"
        with(result.output) {
            contains('Book.groovy')
            contains('BookGroovyTest.groovy')
        }

        when: "there are changes to the sources"
        addNewTestSourceFile()
        result = build('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks execute again"
        assertTasksExecuted(result, ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')
        assertTasksCached(result, ':compileGroovy')

        when: "the Clover report generation task is run without changes"
        result = build('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileGroovy', ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover coverage database is present"
        cloverDb.exists()

        and: "the Clover report is present and is correct"
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        cloverXmlReport.exists()
        coverage.project.metrics.@classes == '1'
        coverage.project.metrics.@methods == '2'
        coverage.project.metrics.@coveredmethods == '1'
        coverage.testproject.metrics.@classes == '3'
        coverage.testproject.metrics.@methods == '3'
        coverage.testproject.metrics.@coveredmethods == '3'
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        and: "the Clover snapshot is not generated because test optimization is not enabled"
        cloverSnapshot.exists() == false

        when: "the build is executed in a relocated directory"
        result = relocateAndBuild('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileGroovy', ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover coverage database is present"
        relocatedCloverDb.exists()

        and: "the Clover report is present and is correct"
        def relocatedCoverage = new XmlSlurper().parse(relocatedCloverXmlReport)
        relocatedCloverXmlReport.exists()
        relocatedCoverage.project.metrics.@classes == '1'
        relocatedCoverage.project.metrics.@methods == '2'
        relocatedCoverage.project.metrics.@coveredmethods == '1'
        relocatedCoverage.testproject.metrics.@classes == '3'
        relocatedCoverage.testproject.metrics.@methods == '3'
        relocatedCoverage.testproject.metrics.@coveredmethods == '3'
        relocatedCloverHtmlReport.exists()
        relocatedCloverJsonReport.exists()
        relocatedCloverPdfReport.exists()

        where:
        gradle << GRADLE_TEST_VERSIONS
    }

    File addNewTestSourceFile() {
        def newTestSourceFile = file('src/test/groovy/AnotherBookGroovyTest.groovy')
        newTestSourceFile << """
            import org.junit.Test

            public class AnotherBookGroovyTest {

                @Test
                public void testOpenTwice() {
                    Book book = new Book();
                    book.open();
                    book.open();
                }

            }
        """
        return newTestSourceFile
    }
}
