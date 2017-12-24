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

class JavaMultiTestTaskProjectSpec extends AbstractFunctionalTestBase {

    @Unroll def"Build a Java multi-test-task project #scenario"()
    {
        given: "a Java multi-test-task project"
        projectName = 'java-multi-test-task-project'
        gradleVersion = CURRENT_GRADLE

        when: "the top-level Clover aggregation task is run"
        build('--init-script', "$initScript", '-b', "${buildFile}.gradle", 'clean', 'cloverGenerateReport')

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
}
