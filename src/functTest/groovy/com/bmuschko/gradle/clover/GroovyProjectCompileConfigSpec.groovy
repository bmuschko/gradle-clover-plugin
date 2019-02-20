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

import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll

class GroovyProjectCompileConfigSpec extends AbstractFunctionalTestBase {

    @Unroll def "Build a Groovy project using compile configuration (with Gradle Version #gradle)"()
    {
        given: "a Groovy project"
        projectName = 'groovy-project-compile-config'
        gradleVersion = gradle

        when: "the Clover report generation task is run"
        BuildResult result = build('clean', 'cloverGenerateReport', '--info')

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

        and: "that the gradle output contains the two files compiled by groovyc"
        with(result.output) {
            contains('Book.groovy')
            contains('BookGroovyTest.groovy')
        }

        where:
        gradle << GRADLE_TEST_VERSIONS
    }
}
