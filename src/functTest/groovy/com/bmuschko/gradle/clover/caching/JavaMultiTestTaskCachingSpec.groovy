package com.bmuschko.gradle.clover.caching

import spock.lang.Unroll


class JavaMultiTestTaskCachingSpec extends AbstractCachingFunctionalSpec {

    @Unroll def "Build a Java multi-test-task project (with Gradle Version #gradle)"()
    {
        given: "a Java multi-test-task project"
        withProjectTemplate('java-multi-test-task-project')
        gradleVersion = gradle

        when: "the top-level Clover aggregation task is run"
        def result = build('-b', "common.gradle", 'clean', 'cloverGenerateReport')

        then:
        assertTasksExecuted(result, ':compileJava', ':compileTestJava', ':compileCustomTestJava', ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForCustomTest', ':test', ':customTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        when: "the configuration changes"
        file('common.gradle') << """
            clover {
                excludes = ["foo"]
            }
        """
        result = build('-b', "common.gradle", 'clean', 'cloverGenerateReport')

        then: "clover tasks execute again"
        assertTasksCached(result, ':compileJava', ':compileTestJava', ':compileCustomTestJava')
        assertTasksExecuted(result, ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForCustomTest', ':test', ':customTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the aggregated Clover report is generated and is correct"
        assertCoverageIsCorrect(1)

        when: "the sources change"
        addNewCustomTestSourceFile()
        result = build('-b', "common.gradle", 'clean', 'cloverGenerateReport')

        then: "test compile and clover tasks execute again"
        assertTasksCached(result, ':compileJava', ':compileTestJava', ':cloverInstrumentCodeForTest', ':test')
        assertTasksExecuted(result,':compileCustomTestJava', ':cloverInstrumentCodeForCustomTest', ':customTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        when: "the build is executed without any changes"
        result = build('-b', "common.gradle", 'clean', 'cloverGenerateReport')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileJava', ':compileTestJava', ':compileCustomTestJava', ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForCustomTest', ':test', ':customTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the aggregated Clover coverage database is generated"
        cloverDb.exists()

        and: "the aggregated Clover report is generated and is correct"
        assertCoverageIsCorrect(3)

        when: "the build is executed in a relocated directory"
        result = relocateAndBuild('-b', "common.gradle", 'clean', 'cloverGenerateReport')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileJava', ':compileTestJava', ':compileCustomTestJava', ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForCustomTest', ':test', ':customTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover coverage database is present"
        withRelocatedBuild {
            cloverDb.exists()
        }

        and: "the Clover report is present and is correct"
        withRelocatedBuild {
            assertCoverageIsCorrect(3)
        }

        where:
        gradle << GRADLE_TEST_VERSIONS
    }

    boolean assertCoverageIsCorrect(int carStarts) {
        assert cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        def carCoverage = coverage.project.package.file.find { it.@name.text() == 'Car.java' }
        assert carCoverage.line.find { it.@num.text() == "21" }.@count.text() == carStarts as String
        assert carCoverage.line.find { it.@num.text() == "26" }.@count.text() == "1"
        assert cloverHtmlReport.exists()
        assert cloverJsonReport.exists()
        assert cloverPdfReport.exists()
        return true
    }

    File addNewCustomTestSourceFile() {
        def newTestSourceFile = file('src/customTest/java/CarStartTwiceTest.java')
        newTestSourceFile << """
            import org.junit.Test;

            public class CarStartTwiceTest {

                @Test
                public void testStartTwice() {
                    Car car = new Car();
                    car.start();
                    car.start();
                }

            }
        """
        return newTestSourceFile
    }
}
