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
 * Defines the Clover Historical convention properties
 *
 * @author Alex Volanis
 */
class CloverReportHistoricalConvention {
    boolean enabled = false
    String historyIncludes = 'clover-*.xml.gz'
    String packageFilter = null
    String from = null
    String to = null

    HistoricalAdded added = null
    List<HistoricalMover> movers = []

    def added(Closure closure) {
        added = new HistoricalAdded()
        ConfigureUtil.configure(closure, added)
    }

    def mover(Closure closure) {
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        HistoricalMover mover = new HistoricalMover()
        closure.delegate = mover
        closure()
        movers << mover
    }
}
