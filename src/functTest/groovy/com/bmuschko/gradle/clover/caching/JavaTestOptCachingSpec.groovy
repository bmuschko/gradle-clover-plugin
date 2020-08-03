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

import com.bmuschko.gradle.clover.AbstractFunctionalTestBase
import spock.lang.Unroll

class JavaTestOptCachingSpec extends AbstractCachingFunctionalSpec {

    @Unroll def "Build a Java project with test optimization enabled (with Gradle Version #gradle)"()
    {
        given: "a Java project with test optimization enabled"
        withProjectTemplate('java-test-opt')
        gradleVersion = gradle

        File sourceFile = new File(projectDir, 'src/main/java/Book.java')

        when: "no task has been run yet"

        then: "the cross-build Clover snapshot doesn't exist or can be deleted"
        !cloverSnapshot.exists() || cloverSnapshot.delete()

        when: "the Clover report generation task is run"
        def buildResult = build('clean', 'cloverGenerateReport')

        then: "the Clover tasks execute"
        assertTasksExecuted(buildResult, ':compileJava', ':compileTestJava', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        assertCoverageIsCorrect()

        and: "the Clover snapshot is generated"
        cloverSnapshot.exists()

        when: "no code is modified, and the Clover report generation task is run a second time"
        buildResult = build('clean', 'cloverGenerateReport')

        then: "no tests were run"
        assertTasksCached(buildResult, ':compileJava', ':compileTestJava', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        when: "the code is modified, and the Clover report generation task is run a third time"
        sourceFile << ' '
        buildResult = build('clean', 'cloverGenerateReport')

        then: "the clover tasks execute again"
        assertTasksCached(buildResult, ':compileTestJava')
        assertTasksExecuted(buildResult, ':compileJava', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "Clover detected one change and exactly one test was run"
        buildResult.output.contains('Test BookTest.testOpen was affected by changed source: true')
        buildResult.output.contains('Clover included 1 test class in this run (total # test classes : 1)')

        and: "the Clover report was generated and is correct"
        assertCoverageIsCorrect()

        when: "the build is relocated"
        buildResult = relocateAndBuild('clean', 'cloverGenerateReport')

        then: "no tests were run"
        assertTasksCached(buildResult, ':compileJava', ':compileTestJava', ':cloverInstrumentCodeForTest', ':test', ':cloverAggregateDatabases', ':cloverGenerateReport')

        where:
        gradle << GRADLE_TEST_VERSIONS
    }

    boolean assertCoverageIsCorrect() {
        assert cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        assert coverage.project.metrics.@classes == '1'
        assert coverage.project.metrics.@methods == '2'
        assert coverage.project.metrics.@coveredmethods == '1'
        assert coverage.testproject.metrics.@classes == '1'
        assert coverage.testproject.metrics.@methods == '1'
        assert coverage.testproject.metrics.@coveredmethods == '1'
        assert cloverHtmlReport.exists()
        assert cloverJsonReport.exists()
        assert cloverPdfReport.exists()
        return true
    }
}
