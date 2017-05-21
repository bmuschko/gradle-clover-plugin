/*
 * Copyright 2017 the original author or authors.
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

class FlushPolicySpec extends Specification {
    @Unroll
    def "Convert String policy (#stringName) to Enum type (#enumType)"() {
        given: "A valid String policy name"

        when: "The name is converted"
        FlushPolicy policy = FlushPolicy.valueOf(stringName)

        then: "The type matches the expected value"
        enumType == policy

        where:
        stringName | enumType
        'directed' | FlushPolicy.directed
        'interval' | FlushPolicy.interval
        'threaded' | FlushPolicy.threaded
    }

    def "Invalid flush policy String name fails to convert"() {
        given: "An invalid String policy name"
        String invalid = 'bogus'

        when: "when the conversion is attempted"
        FlushPolicy.valueOf(invalid)

        then: "then right exception is thrown"
        def e = thrown(IllegalArgumentException)
        'No enum constant com.bmuschko.gradle.clover.FlushPolicy.bogus' == e.message
    }
}
