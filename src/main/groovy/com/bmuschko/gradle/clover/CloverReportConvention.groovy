/*
 * Copyright 2011, 2017 the original author or authors.
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
 * Defines Clover report convention.
 *
 * @author Benjamin Muschko
 */
class CloverReportConvention {
    Boolean xml = true
    Boolean json = false
    Boolean html = false
    Boolean pdf = false
    String filter
    String testResultsDir
    String testResultsInclude = 'TEST-*.xml'
    Boolean alwaysReport = false
    Boolean includeFailedTestCoverage = false
    Integer numThreads = 2
    String timeout = ''

    final CloverReportHistoricalConvention historical
    final CloverReportColumnsConvention columns

    @Inject
    CloverReportConvention(ObjectFactory objectFactory) {
        historical = objectFactory.newInstance(CloverReportHistoricalConvention)
        columns = new CloverReportColumnsConvention()
    }

    def historical(Action<? extends CloverReportHistoricalConvention> action) {
        action.execute(historical)
    }

    def columns(Action<? extends CloverReportColumnsConvention> action) {
        action.execute(columns)
    }
}
