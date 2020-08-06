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

import spock.lang.Unroll

class JavaMultiProjectCachingSpec extends AbstractCachingFunctionalSpec {

    @Unroll def "correctly caches a Java multi-project build (with Gradle Version #gradle)"() {
        given: "a Java multi-project"
        withProjectTemplate('java-multi-project')
        gradleVersion = gradle

        when: "the top-level Clover aggregation task is run"
        def result = build('--init-script', "$initScript", 'clean', 'cloverAggregateReports')

        then: "the clover tasks execute"
        assertTasksExecuted(result, allTasksFor('project4a'))
        assertTasksExecuted(result, allTasksFor('project4b'))
        assertTasksExecuted(result, ':cloverAggregateReports')

        then: "the aggregated Clover coverage database is generated"
        allCloverDb.exists()

        when: "there are changes to the configuration"
        file('build.gradle') << """
            subprojects {
                clover {
                    excludes = ["foo"]
                }
            }
        """
        result = build('--init-script', "$initScript", 'clean', 'cloverAggregateReports')

        then: "the clover tasks execute again"
        assertTasksExecuted(result, allCloverTasksFor('project4a'))
        assertTasksExecuted(result, allCloverTasksFor('project4b'))
        assertTasksExecuted(result, ':cloverAggregateReports')
        assertTasksCached(result, allCompileTasksFor('project4a'))
        assertTasksCached(result, allCompileTasksFor('project4b'))

        when: "there are changes to the source"
        addNewTestSourceFile()
        result = build('--init-script', "$initScript", 'clean', 'cloverAggregateReports')

        then: "the clover tasks execute again"
        assertTasksExecuted(result, testCompileAndCloverTasksFor('project4a'))
        assertTasksCached(result, compileTaskFor('project4a'))
        assertTasksCached(result, allTasksFor('project4b'))
        assertTasksExecuted(result, ':cloverAggregateReports')

        and: "the aggregated Clover report is generated and is correct"
        assertCoverageIsCorrect()

        and: "the Clover snapshot is not generated because test optimization is not enabled"
        !cloverSnapshot.exists()

        when: "the top-level Clover aggregation task is run without changes"
        result = build('--init-script', "$initScript", 'clean', 'cloverAggregateReports')

        then:
        assertTasksCached(result, allTasksFor('project4a'))
        assertTasksCached(result, allTasksFor('project4b'))
        assertTasksCached(result, ':cloverAggregateReports')

        when: "the top-level Clover aggregation task is run without changes in a relocated directory"
        result = relocateAndBuild('--init-script', "$initScript", 'clean', 'cloverAggregateReports')

        then:
        assertTasksCached(result, allTasksFor('project4a'))
        assertTasksCached(result, allTasksFor('project4b'))
        assertTasksCached(result, ':cloverAggregateReports')

        and: "the aggregated Clover report is generated and is correct"
        withRelocatedBuild {
            assertCoverageIsCorrect()
        }

        where:
        gradle << GRADLE_TEST_VERSIONS
    }

    boolean assertCoverageIsCorrect() {
        assert cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        assert coverage.project.package.file.size() == 2
        assert coverage.project.package.file[0].@name == 'Car.java'
        assert coverage.project.package.file[1].@name == 'Truck.java'
        assert !coverage.project.package.file*.@name.contains('Motorbike.java')
        assert coverage.testproject.package.file.size() == 3
        assert coverage.testproject.package.file.collect { it.@name }.sort() == [ 'TruckTest.java', 'AnotherCarTest.java', 'CarTest.java' ]
        assert !coverage.testproject.package.file*.@name.contains('MotorbikeTest.java')
        assert cloverHtmlReport.exists()
        assert cloverJsonReport.exists()
        assert cloverPdfReport.exists()
        return true
    }

    String[] allTasksFor(String subproject) {
        return allCompileTasksFor(subproject) + allCloverTasksFor(subproject)
    }

    String[] compileTaskFor(String subproject) {
        return [ ":${subproject}:compileJava" ]
    }

    String[] testCompileTaskFor(String subproject) {
        return [ ":${subproject}:compileTestJava" ]
    }

    String[] allCompileTasksFor(String subproject) {
        return compileTaskFor(subproject) + testCompileTaskFor(subproject)
    }

    String[] allCloverTasksFor(String subproject) {
        return [ ":${subproject}:cloverInstrumentCodeForTest", ":${subproject}:test", ":${subproject}:cloverAggregateDatabases", ":${subproject}:cloverGenerateReport" ]
    }

    String[] testCompileAndCloverTasksFor(String subproject) {
        return testCompileTaskFor(subproject) + allCloverTasksFor(subproject)
    }

    File addNewTestSourceFile() {
        return file('project4a/src/test/java/AnotherCarTest.java') << """
            import org.junit.Test;

            public class AnotherCarTest {

                @Test
                public void testStartTwice() {
                    Car car = new Car();
                    car.start();
                    car.start();
                }

            }
        """
    }
}
