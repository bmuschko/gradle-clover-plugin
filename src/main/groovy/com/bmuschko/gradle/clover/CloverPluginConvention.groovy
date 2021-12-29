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

import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory

import javax.inject.Inject

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
    boolean enabled = true
    Boolean useClover3 = null
    String targetPercentage
    boolean optimizeTests
    String snapshotFile
    String historyDir
    List<CloverSourceSet> additionalSourceSets = []
    List<CloverSourceSet> additionalTestSourceSets =[]
    List<String> includes
    List<String> excludes
    List<String> testIncludes
    List<String> testExcludes
    final CloverReportConvention report
    final CloverContextsConvention contexts
    final CloverCompilerConvention compiler
    List<String> includeTasks
    List<String> excludeTasks
    String instrumentLambda
    boolean debug = false
    int flushinterval = 1000
    FlushPolicy flushpolicy = FlushPolicy.directed

    private final ObjectFactory objectFactory

    @Inject
    CloverPluginConvention(ObjectFactory objectFactory) {
        this.objectFactory = objectFactory
        report = objectFactory.newInstance(CloverReportConvention)
        contexts = objectFactory.newInstance(CloverContextsConvention)
        compiler = objectFactory.newInstance(CloverCompilerConvention)
    }

    def clover(Action<CloverPluginConvention> action) {
        action.execute(this)
    }

    def contexts(Action<CloverContextsConvention> action) {
        action.execute(contexts)
    }

    def statement(Action<CloverContextConvention> action) {
        CloverContextConvention statementContext = objectFactory.newInstance(CloverContextConvention)
        contexts.statements << statementContext
        action.execute(statementContext)
    }

    def method(Action<CloverMethodContextConvention> action) {
        CloverMethodContextConvention methodContext = objectFactory.newInstance(CloverMethodContextConvention)
        contexts.methods << methodContext
        action.execute(methodContext)
    }

    def additionalSourceSet(Action<CloverSourceSet> action) {
        CloverSourceSet additionalSourceSet = objectFactory.newInstance(CloverSourceSet)
        additionalSourceSets << additionalSourceSet
        action.execute(additionalSourceSet)
    }

    def additionalTestSourceSet(Action<CloverSourceSet> action) {
        CloverSourceSet additionalTestSourceSet = objectFactory.newInstance(CloverSourceSet)
        additionalTestSourceSets << additionalTestSourceSet
        action.execute(additionalTestSourceSet)
    }

    def report(Action<CloverReportConvention> action) {
        action.execute(report)
    }

    def compiler(Action<CloverCompilerConvention> action) {
        action.execute(compiler)
    }
}
