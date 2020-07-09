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

    @Unroll def "correctly caches a build for a Groovy project (with Gradle Version #gradle)"() {
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
        assertTasksExecuted(result, ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases')
        assertTasksCached(result, ':compileGroovy', ':compileTestGroovy', ':cloverGenerateReport')

        when: "the Clover report generation task is run without changes"
        result = build('clean', 'cloverGenerateReport', '--info')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileGroovy', ':compileTestGroovy', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
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

        where:
        gradle << GRADLE_TEST_VERSIONS
    }
}
