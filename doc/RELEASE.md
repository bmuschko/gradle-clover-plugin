# Introduction
This document describes the release process designed and implemented for `gradle-clover-plugin`. Its main purpose is to explain to developers and maintainers how to prepare and release a new version of this plugin.

# Tools
The release process uses some external libraries and services described in detail below.

## org.ajoberstar.reckon
The [`org.ajoberstar.reckon`](https://github.com/ajoberstar/reckon) Gradle plugin is used to automatically determine the project version. `org.ajoberstar.reckon` is applied in the main [build.gradle](../build.gradle) and configured in [gradle/release.gradle](../gradle/release.gradle). Please refer to the plugin [documentation](https://github.com/ajoberstar/reckon/blob/master/README.md#how-do-i-use-it) for more details.

## gradle-git-publish
The [`gradle-git-publish`](https://github.com/ajoberstar/gradle-git-publish) Gradle plugin is used to publish the documentation to `gh-pages` branch. It is applied and configured in the [gradle/documentation.gradle](../gradle/documentation.gradle) file.

## Travis CI
[Travis CI](https://travis-ci.com) service is used as our current [CI/CD](https://en.wikipedia.org/wiki/CI/CD) server. Build and deploy jobs are configured in [.travis.yml](../.travis.yml) file. Please refer its [documentation](https://docs.travis-ci.com/) for more details.

## Bintray
[Bintray](https://bintray.com) service is used to publish plugin versions. With [BintrayPlugin](https://github.com/bintray/gradle-bintray-plugin) artifacts are uploaded to a remote reposiotry. Plugin configuration is in the [gradle/publishing.gradle](../gradle/publishing.gradle) file.

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
6. After push to the `origin`, Travis detects new tag and triggers a build.
7. Travis [is instructed](../.travis.yml#L17) to execute [release stage](https://docs.travis-ci.com/user/build-stages/) when on Git tag.
8. In this stage [Gradle script](../.travis.yml#L15) assembles plugin binaries (with new version) and uploads them to Bintray (credentials are stored as [secure variables](https://docs.travis-ci.com/user/environment-variables/#Defining-Variables-in-Repository-Settings) in Travis). Also JavaDoc is published to `gh-pages` branch (GitHub username and [access token](https://help.github.com/articles/creating-a-personal-access-token-for-the-command-line/) are also stored as secure variables).

# Useful links
* [Semantic Versioning](http://semver.org/)
* [org.ajoberstar.reckon version inference](https://github.com/ajoberstar/reckon#stage-version-scheme)
* [Travis script deployment](https://docs.travis-ci.com/user/deployment/script/)
