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

import spock.lang.Specification
import spock.lang.Unroll

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
}
