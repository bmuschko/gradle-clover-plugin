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

    @Override
    protected File getProjectDir() {
        assert projectName != null
        return new File(testBuildDir.getRoot(), projectName)
    }

    def withProjectTemplate(String projectName) {
        this.projectName = projectName
        // Create a copy of the project directory so we can change it during tests
        FileUtils.copyDirectory(super.getProjectDir(), getProjectDir())
        FileUtils.copyFile(new File(super.getProjectDir().parentFile, 'deps.gradle'), new File(testBuildDir.getRoot(), 'deps.gradle'))
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
        withScanEnabled()
    }

    String getAccessKeyFor(String gradleEnterpriseServer) {
        Properties keysProperties = new Properties()
        File userHome = new File(System.getProperty('user.home'))
        File keysPropertiesFile = new File(userHome, ".gradle/enterprise/keys.properties")
        if (keysPropertiesFile.exists()) {
            keysProperties.load(keysPropertiesFile.newInputStream())
            String accessKey = keysProperties.getProperty(gradleEnterpriseServer)
            return accessKey ? "${gradleEnterpriseServer}=${accessKey}" : null
        }
    }

    void withScanEnabled() {
        def gradleEnterpriseServer = System.getenv('GRADLE_ENTERPRISE_SERVER')
        if (gradleEnterpriseServer) {
            accessToken = getAccessKeyFor(gradleEnterpriseServer)
            if (accessToken == null) {
                println "Access key could not be found for ${gradleEnterpriseServer} - skipping build scan configuration"
                return
            }

            initScript << """
                import com.gradle.enterprise.gradleplugin.GradleEnterprisePlugin
                import com.gradle.scan.plugin.BuildScanPlugin
                import org.gradle.util.GradleVersion

                initscript {
                    def pluginVersion = "3.3.4"

                    repositories {
                        gradlePluginPortal()
                    }
                    dependencies {
                        classpath("com.gradle:gradle-enterprise-gradle-plugin:\${pluginVersion}")
                    }
                }

                def isTopLevelBuild = gradle.getParent() == null

                if (isTopLevelBuild) {
                    def gradleVersion = GradleVersion.current().baseVersion
                    def atLeastGradle5 = gradleVersion >= GradleVersion.version("5.0")
                    def atLeastGradle6 = gradleVersion >= GradleVersion.version("6.0")

                    if (atLeastGradle6) {
                        beforeSettings {
                            if (!it.pluginManager.hasPlugin("com.gradle.enterprise")) {
                                it.pluginManager.apply(GradleEnterprisePlugin)
                            }
                            configureExtension(it.extensions["gradleEnterprise"])
                        }
                    } else if (atLeastGradle5) {
                        rootProject {
                            pluginManager.apply(BuildScanPlugin)
                            configureExtension(extensions["gradleEnterprise"])
                        }
                    }
                }

                void configureExtension(extension) {
                    extension.with {
                        server = "https://${gradleEnterpriseServer}"
                        buildScan {
                            publishAlways()
                            captureTaskInputFiles = true
                        }
                    }
                }
            """
        } else {
            println "withScanEnabled() was called, but GRADLE_ENTERPRISE_SERVER environment variable was not set"
        }
    }

    @Override
    protected GradleRunner createAndConfigureGradleRunner(String... arguments) {
        def cacheArguments = ['--build-cache', '--init-script', initScript.absolutePath]
        GradleRunner runner = super.createAndConfigureGradleRunner(arguments + cacheArguments as String[])
        if (accessToken != null) {
            return runner.withEnvironment([GRADLE_ENTERPRISE_ACCESS_KEY: accessToken]).withDebug(false)
        } else {
            return runner
        }
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
}
