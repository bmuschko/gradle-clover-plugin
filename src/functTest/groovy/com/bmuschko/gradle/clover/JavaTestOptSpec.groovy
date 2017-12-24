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

class JavaTestOptSpec extends AbstractFunctionalTestBase {

    @Unroll def"Build a Java project with test optimization enabled (with Gradle Version #gradle)"()
    {
        given: "a Java project with test optimization enabled"
        projectName = 'java-test-opt'
        gradleVersion = gradle
        
        File sourceFile = new File(projectDir, 'src/main/java/Book.java')

        when: "no task has been run yet"

        then: "the cross-build Clover snapshot doesn't exist or can be deleted"
        !cloverSnapshot.exists() || cloverSnapshot.delete()

        when: "the Clover report generation task is run"
        build('clean', 'cloverGenerateReport')

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
        def output = build('clean', 'cloverGenerateReport').output

        then: "no tests were run"
        output.contains('For the current test set, Clover detected no modified source files')
        output.contains('Clover included 0 test classes in this run (total # test classes : 1)')

        when: "the code is modified, and the Clover report generation task is run a third time"
        sourceFile << ' '
        output = build('clean', 'cloverGenerateReport').output

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

        where:
        gradle << GRADLE_TEST_VERSIONS
    }
}
