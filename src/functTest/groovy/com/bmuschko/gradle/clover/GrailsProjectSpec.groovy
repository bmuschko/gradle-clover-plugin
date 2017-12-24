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

import spock.lang.Unroll

class GrailsProjectSpec extends AbstractFunctionalTestBase {

    @Unroll def"Build a Grails3 project with unit tests and Clover coverage report (with Gradle Version #gradle)"()
    {
        given: "a Grails3 project with unit tests"
        projectName = 'grails-project'
        gradleVersion = gradle

        when: "the Clover report generation task is run"
        build('clean', 'cloverGenerateReport')

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

        where:
        gradle << GRADLE_TEST_VERSIONS
    }
}
