package com.bmuschko.gradle.clover.caching

import com.bmuschko.gradle.clover.AbstractFunctionalTestBase
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder


class AbstractCachingFunctionalSpec extends AbstractFunctionalTestBase {
    @Rule
    TemporaryFolder localCacheTempDir = new TemporaryFolder()

    File initScript
    File localCacheDir
    String accessToken
    boolean relocateBuild

    @Override
    protected File getProjectDir() {
        assert projectName != null
        return relocateBuild ? new File(testBuildDir.getRoot(), "relocated/${projectName}") : new File(testBuildDir.getRoot(), projectName)
    }

    @Override
    protected File getBuildDir() {
        return relocateBuild ? new File(testBuildDir.getRoot(), "relocated/build") : super.getBuildDir()
    }

    def withProjectTemplate(String projectName) {
        this.projectName = projectName
        return withProjectTemplate(super.getProjectDir())
    }

    def withProjectTemplate(File sourceDir) {
        // Create a copy of the project directory so we can change it during tests
        FileUtils.copyDirectory(sourceDir, getProjectDir())
        FileUtils.copyFile(new File(sourceDir.parentFile, 'deps.gradle'), new File(getProjectDir().parentFile, 'deps.gradle'))
    }

    def setup() {
        initScript = localCacheTempDir.newFile('init.gradle')
        localCacheDir = localCacheTempDir.newFolder('local-cache')
        initScript << """
            gradle.settingsEvaluated { settings ->
                settings.buildCache {
                    local {
                        directory = new File('${localCacheDir.absolutePath}/build-cache')
                    }
                }
            }
        """
    }

    @Override
    protected GradleRunner createAndConfigureGradleRunner(String... arguments) {
        def cacheArguments = ['--build-cache', '--init-script', initScript.absolutePath]
        return super.createAndConfigureGradleRunner(arguments + cacheArguments as String[])
    }

    void assertTasksExecuted(BuildResult result, String... taskPaths) {
        assertTasksOutcomeAllMatch(result, TaskOutcome.SUCCESS, taskPaths)
    }

    void assertTasksCached(BuildResult result, String... taskPaths) {
        assertTasksOutcomeAllMatch(result, TaskOutcome.FROM_CACHE, taskPaths)
    }

    void assertTasksUpToDate(BuildResult result, String... taskPaths) {
        assertTasksOutcomeAllMatch(result, TaskOutcome.UP_TO_DATE, taskPaths)
    }

    void assertTasksSkipped(BuildResult result, String... taskPaths) {
        assertTasksOutcomeAllMatch(result, TaskOutcome.SKIPPED, taskPaths)
    }

    void assertTaskOutcomeMatches(BuildResult result, TaskOutcome outcome, String taskPath) {
        assert result.task(taskPath).outcome == outcome
    }

    void assertTasksOutcomeAllMatch(BuildResult result, TaskOutcome outcome, String... taskPaths) {
        taskPaths.each { taskPath ->
            assertTaskOutcomeMatches(result, outcome, taskPath)
        }
    }

    BuildResult relocateAndBuild(String... arguments) {
        def originalProjectDir = getProjectDir()
        withRelocatedBuild {
            withProjectTemplate(originalProjectDir)
            build(arguments)
        }
    }

    protected <T> T withRelocatedBuild(Closure<T> closure) {
        boolean previousState = relocateBuild
        try {
            relocateBuild = true
            closure.call()
        } finally {
            relocateBuild = previousState
        }
    }
}
