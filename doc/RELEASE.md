# Introduction
This document describes the release process designed and implemented for `gradle-clover-plugin`. Its main purpose is to explain to developers and maintainers how to prepare and release a new version of this plugin.

# Tools
The release process uses some external libraries and services described in detail below.

## org.ajoberstar.reckon
The [`org.ajoberstar.reckon`](https://github.com/ajoberstar/reckon) Gradle plugin is used to automatically determine the project version. `org.ajoberstar.reckon` is applied in the main [build.gradle](../build.gradle) and configured in [gradle/release.gradle](../gradle/release.gradle). Please refer to the plugin [documentation](https://github.com/ajoberstar/reckon/blob/master/README.md#how-do-i-use-it) for more details.

## gradle-git-publish
The [`gradle-git-publish`](https://github.com/ajoberstar/gradle-git-publish) Gradle plugin is used to publish the documentation to `gh-pages` branch. It is applied and configured in the [gradle/documentation.gradle](../gradle/documentation.gradle) file.

## Github Actions
[Github Actions](https://github.com/bmuschko/gradle-clover-plugin/actions) service is used as our current [CI/CD](https://en.wikipedia.org/wiki/CI/CD) server. Build and release jobs are configured in [workflows](../.github/workflows) file. Please refer its [documentation](https://github.com/actions/starter-workflows#readme) for more details.

# Workflow
The release process is automated to some extent. The following steps describe the workflow.
1. Developer updates `RELEASE_NOTES.md` with new planned version.
2. Developer commits all changes in local working copy.
3. Developer triggers new version release using the following command:
```
./gradlew reckonTagPush -Preckon.stage=final -Preckon.scope=[SCOPE]
```
 where `[SCOPE]` can be one of: `major`, `minor` or `patch`, and determines which part of the version string `<major>.<minor>.<patch>`   will be increased.

4. Gradle executes a build on developer's machine which calculates new version string, creates new tag with it and pushes to the `origin`.
5. When Gradle build is finished, developer's work is done and the rest of the release process is automated.
6. After push to the `origin`, Github Actions detects new tag and triggers a build.
7. Github Action [is instructed](../.github/workflows/linux-build-release.yml#L56) to execute Release/Publish when on Git tag.

# Useful links
* [Semantic Versioning](http://semver.org/)
* [org.ajoberstar.reckon version inference](https://github.com/ajoberstar/reckon#stage-version-scheme)
* [Github Starter Workflows](https://github.com/actions/starter-workflows#readme)
