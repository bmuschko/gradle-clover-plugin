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

import org.gradle.util.ConfigureUtil

/**
 * Defines Clover plugin convention.
 *
 * @author Benjamin Muschko
 */
class CloverPluginConvention {
    File classesBackupDir
    File testClassesBackupDir
    String licenseLocation
    String initString
    Boolean enabled = Boolean.TRUE
    String targetPercentage
    boolean optimizeTests
    String snapshotFile
    Set<File> additionalSourceDirs
    Set<File> additionalTestDirs
    List<String> includes
    List<String> excludes
    List<String> testIncludes
    CloverReportConvention report = new CloverReportConvention()
    CloverContextsConvention contexts = new CloverContextsConvention()
    CloverCompilerConvention compiler = new CloverCompilerConvention()
    List<String> includeTasks
    List<String> excludeTasks

    def clover(Closure closure) {
        ConfigureUtil.configure(closure, this)
    }

    def contexts(Closure closure) {
        ConfigureUtil.configure(closure, contexts)
    }

    def statement(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        CloverContextConvention statementContext = new CloverContextConvention()
        closure.delegate = statementContext
        contexts.statements << statementContext
        closure()
    }

    def method(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        CloverContextConvention methodContext = new CloverContextConvention()
        closure.delegate = methodContext
        contexts.methods << methodContext
        closure()
    }

    def report(Closure closure) {
        ConfigureUtil.configure(closure, report)
    }

    def compiler(Closure closure) {
        ConfigureUtil.configure(closure, compiler)
    }
}
