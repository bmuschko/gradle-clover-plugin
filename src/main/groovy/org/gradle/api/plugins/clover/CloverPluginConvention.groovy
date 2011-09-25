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
package org.gradle.api.plugins.clover

/**
 * Defines Clover plugin convention.
 *
 * @author Benjamin Muschko
 */
class CloverPluginConvention {
    File classesBackupDir
    File licenseFile
    String initString
    String targetPercentage
    List<String> includes
    List<String> excludes
    CloverReportConvention report = new CloverReportConvention()
    CloverContextsConvention contexts = new CloverContextsConvention()

    def clover(Closure closure) {
        closure.delegate = this
        closure()
    }

    def contexts(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = contexts
        closure()
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
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.delegate = report
        closure()
    }
}
