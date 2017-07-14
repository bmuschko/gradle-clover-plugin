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

import org.gradle.api.InvalidUserDataException

import spock.lang.Specification

class CloverPluginConventionSpec extends Specification {
    def "FlushPolicy assignment conversion works"() {
        given: "A new CloverPluginConvention instance"
        def convention = new CloverPluginConvention()

        when: "and configuration closure with a valid flushpolicy and flushinterval"
        convention.clover {
            flushinterval = 500
            flushpolicy = 'threaded'
        }

        then: "then flushpolicy and flushinterval are assigned correctly"
        convention.flushinterval == 500
        convention.flushpolicy == FlushPolicy.threaded
    }

    def "Invalid FlushPolicy assignment fails as expected"() {
        given: "A new CloverPluginConvention instance"
        def convention = new CloverPluginConvention()

        when: "and configuration closure with a bad flushpolicy"
        convention.clover {
            flushpolicy = 'bogus'
        }

        then: "then right exception is thrown"
        def e = thrown(org.gradle.internal.typeconversion.TypeConversionException)
        e.message =~ /^Cannot convert string value 'bogus' to an enum value/
    }

    def "Historical report convention can be configured"() {
        given: "A new CloverPluginConvention instance"
        def convention = new CloverPluginConvention()

        when: "configuration closure with historical report enabled"
        convention.clover {
            report {
                historical {
                    enabled = true
                    added {
                        range = 15
                        interval = '5 days'
                    }
                    mover {
                        threshold = 5
                        range = 10
                        interval = '2 days'
                    }
                    mover {
                        threshold = 5
                        range = 5
                        interval = '2 weeks'
                    }
                    mover {
                        threshold = 5
                        range = 10
                        interval = '6 months'
                    }
                }
            }
        }

        then: "all selections are reflected"
        convention.report.historical.enabled == true
        convention.report.historical.historyIncludes == 'clover-*.xml.gz'
        convention.report.historical.packageFilter == null
        convention.report.historical.from == null
        convention.report.historical.to == null

        convention.report.historical.added.range == 15
        convention.report.historical.added.interval == '5 days'

        convention.report.historical.movers.size() == 3
        convention.report.historical.movers[0].interval == '2 days'
        convention.report.historical.movers[1].interval == '2 weeks'
        convention.report.historical.movers[2].interval == '6 months'
    }
    
    def "Additional columns for clover report can be specified"() {
        given: "A new CloverPluginConvention instance"
        def convention = new CloverPluginConvention()

        when: "configuration closure with report columns is added"
        convention.clover {
            report {
                columns {
                    coveredMethods format: 'longbar', min: '75'
                    coveredStatements format: '%'
                    coveredBranches format: 'raw'
                    totalPercentageCovered format: '%', scope: 'package'
                }
            }
        }
        
        then: "all columns are reflected in the collection"
        convention.report.columns.columns.size() == 4
        with(convention.report.columns.columns.find { it.column == 'coveredMethods' }) {
            attributes.size() == 2
            attributes.format == 'longbar'
            attributes.min == '75'
        }
        with(convention.report.columns.columns.find { it.column == 'coveredStatements' }) {
            attributes.size() == 1
            attributes.format == '%'
        }
        with(convention.report.columns.columns.find { it.column == 'coveredBranches' }) {
            attributes.size() == 1
            attributes.format == 'raw'
        }
        with(convention.report.columns.columns.find { it.column == 'totalPercentageCovered' }) {
            attributes.size() == 2
            attributes.format == '%'
            attributes.scope == 'package'
        }
    }
    
    def "Invalid columns added to report are rejected with useful message"() {
        given: "A new CloverPluginConvention instance"
        def convention = new CloverPluginConvention()

        when: "configuration closure with report columns is added"
        convention.clover {
            report {
                columns {
                    expression format: 'raw'
                }
            }
        }
        
        then: "A proper exception is thrown"
        def e = thrown(InvalidUserDataException)
        e.message == "Unsupported column name 'expression' for Clover report"
    }
}
