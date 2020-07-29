package com.bmuschko.gradle.clover.caching

import org.gradle.testkit.runner.BuildResult
import spock.lang.Unroll


class JavaProjectWithAdditionalTestsCachingSpec extends AbstractCachingFunctionalSpec {
    @Unroll
    def "correctly caches a Java project with additional tests (with Gradle Version #gradle)"() {
        given: "a Java project with additional tests"
        withProjectTemplate('java-project-with-additional-tests')
        gradleVersion = gradle

        when: "the Clover report generation task is run"
        BuildResult result = build('clean', 'cloverGenerateReport')

        then: "the clover tasks execute"
        assertTasksExecuted(result, ':compileJava', ':compileTestJava', ':compileIntegTestJava', ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForIntegrationTest', ':test', ':integrationTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        then: "the Clover coverage database is generated"
        cloverDb.exists()

        and: "the Clover report is generated and is correct"
        assertCoverageIsCorrect(1)

        when: "there are changes to the clover configuration"
        file('build.gradle') << """
            clover {
                compiler {
                    debug = true
                }
            }
        """
        result = build('clean', 'cloverGenerateReport')

        then: "the clover tasks are executed again"
        assertTasksCached(result, ':compileJava', ':compileTestJava', ':compileIntegTestJava')
        assertTasksExecuted(result, ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForIntegrationTest', ':test', ':integrationTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        when: "there are changes to the sources"
        addNewSourceFile()
        result = build('clean', 'cloverGenerateReport')

        then: "the clover tasks execute"
        assertTasksExecuted(result, ':compileJava', ':compileTestJava', ':compileIntegTestJava', ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForIntegrationTest', ':test', ':integrationTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover report is generated and is correct"
        assertCoverageIsCorrect(2)

        when: "the build is run with no changes"
        result = build('clean', 'cloverGenerateReport')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileJava', ':compileTestJava', ':compileIntegTestJava', ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForIntegrationTest', ':test', ':integrationTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        when: "the build is relocated"
        result = relocateAndBuild('clean', 'cloverGenerateReport')

        then: "the clover tasks are cached"
        assertTasksCached(result, ':compileJava', ':compileTestJava', ':compileIntegTestJava', ':cloverInstrumentCodeForTest', ':cloverInstrumentCodeForIntegrationTest', ':test', ':integrationTest', ':cloverAggregateDatabases', ':cloverGenerateReport')

        and: "the Clover report is generated and is correct"
        assertCoverageIsCorrect(2)

        where:
        gradle << GRADLE_TEST_VERSIONS
    }

    boolean assertCoverageIsCorrect(int classCount) {
        assert cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        assert coverage.project.metrics.@classes == classCount as String
        assert coverage.project.metrics.@methods == '2'
        assert coverage.project.metrics.@coveredmethods == '1'
        assert coverage.testproject.metrics.@files == '2'
        assert coverage.testproject.metrics.@classes == '2'
        assert coverage.testproject.metrics.@methods == '2'
        assert coverage.testproject.metrics.@coveredmethods == '2'
        assert cloverHtmlReport.exists()
        assert cloverJsonReport.exists()
        assert cloverPdfReport.exists()
        return true
    }

    File addNewSourceFile() {
        File newSourceFile = file('src/main/java/Library.java')
        newSourceFile << """
            import java.util.ArrayList;

            public class Library {
                ArrayList<Book> books = new ArrayList<Book>();
            }
        """
    }
}
