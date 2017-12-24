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

import java.io.File

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

protected abstract class AbstractFunctionalTestBase extends Specification {
    // Change this as the current version is updated for each release
    protected static final CURRENT_GRADLE = '4.4.1'
    // Add new versions as needed when making a new release
    protected static GRADLE_TEST_VERSIONS = ['2.14.1', '3.5.1', CURRENT_GRADLE]

    @Rule
    TemporaryFolder testBuildDir = new TemporaryFolder()

    /* The name of the project currently being tested. */
    protected String projectName
    protected String gradleVersion = CURRENT_GRADLE

    protected File getProjectDir() {
        new File('src/functTest/projects', projectName)
    }

    protected File getBuildDir() {
        new File(testBuildDir.getRoot(), 'build')
    }

    protected File getCloverDb() {
        new File(buildDir, '.clover/clover.db')
    }

    protected File getCloverSnapshot() {
        new File(projectDir, '.clover/coverage.db.snapshot-test')
    }

    protected File getCloverXmlReport() {
        new File(getReportsDir(), 'clover.xml')
    }

    protected File getCloverHtmlReport() {
        new File(getReportsDir(), 'html')
    }

    protected File getCloverJsonReport() {
        new File(getReportsDir(), 'json')
    }

    protected File getCloverPdfReport() {
        new File(getReportsDir(), 'clover.pdf')
    }

    protected File getCloverHistoricalHtmlReport() {
        new File(getReportsDir(), 'html/historical.html')
    }

    protected File getCloverHistoricalPdfReport() {
        new File(getReportsDir(), 'historical.pdf')
    }

    protected File getReportsDir() {
        new File(buildDir, 'reports/clover')
    }

    protected File getInitScript() {
        new File(testBuildDir.getRoot(), 'injectClasspath.gradle')
    }

    protected BuildResult build(String... arguments) {
        createAndConfigureGradleRunner(arguments).build()
    }

    protected BuildResult buildAndFail(String... arguments) {
        createAndConfigureGradleRunner(arguments).buildAndFail()
    }

    private GradleRunner createAndConfigureGradleRunner(String... arguments) {
        // Provide the buildDir as a property to process in deps.gradle
        def args = ['--stacktrace', "-PtestBuildDir=$buildDir".toString()]
        if (arguments) {
            args.addAll(arguments)
        }
        def runner = GradleRunner.create().withGradleVersion(gradleVersion).withProjectDir(projectDir).withArguments(args).withPluginClasspath()
        createClasspathInjectionScript(runner)
        runner
    }

    private void createClasspathInjectionScript(GradleRunner runner) {
        def classpathString = runner.getPluginClasspath()
            .collect { it.absolutePath.replace('\\', '\\\\') } // escape backslashes in Windows paths
            .collect { "'$it'" }
            .join(", ")

        initScript << """
initscript {
    rootProject {
        buildscript {
            dependencies {
                classpath files($classpathString)
            }
        }
    }
}"""

    }
}
