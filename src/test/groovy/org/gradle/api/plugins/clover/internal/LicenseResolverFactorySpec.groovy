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
package org.gradle.api.plugins.clover.internal

import spock.lang.Specification

/**
 * License resolver factory tests.
 *
 * @author Benjamin Muschko
 */
class LicenseResolverFactorySpec extends Specification {
    def "Get resolver for empty location"() {
        given: "a empty location"
            def location = ''

        when: "trying to get the license resolver"
            LicenseResolver resolver = LicenseResolverFactory.instance.getResolver(location)

        then: "the retrieved resolver is the default implementation"
            resolver instanceof LicenceDefaultResolver
    }

    def "Get resolver for null location"() {
        given: "a null location"
            def location = null

        when: "trying to get the license resolver"
            LicenseResolver resolver = LicenseResolverFactory.instance.getResolver(location)

        then: "the retrieved resolver is the default implementation"
            resolver instanceof LicenceDefaultResolver
    }

    def "Get resolver for file URL location"() {
        given: "a file URL location"
            def location = 'file:///home/ben/test/clover.license'

        when: "trying to get the license resolver"
            LicenseResolver resolver = LicenseResolverFactory.instance.getResolver(location)

        then: "the retrieved resolver is the URL implementation"
            resolver instanceof LicenseUrlResolver
    }

    def "Get resolver for HTTP URL location"() {
        given: "a HTTP URL location"
            def location = 'http://internal.license.server/clover/clover.license'

        when: "trying to get the license resolver"
            LicenseResolver resolver = LicenseResolverFactory.instance.getResolver(location)

        then: "the retrieved resolver is the URL implementation"
            resolver instanceof LicenseUrlResolver
    }

    def "Get resolver for HTTPS URL location"() {
        given: "a HTTPS URL location"
            def location = 'https://internal.license.server/clover/clover.license'

        when: "trying to get the license resolver"
            LicenseResolver resolver = LicenseResolverFactory.instance.getResolver(location)

        then: "the retrieved resolver is the URL implementation"
            resolver instanceof LicenseUrlResolver
    }

    def "Get resolver for *NIX file location"() {
        given: "a file location"
            def location = '/home/ben/clover/clover.license'

        when: "trying to get the license resolver"
            LicenseResolver resolver = LicenseResolverFactory.instance.getResolver(location)

        then: "the retrieved resolver is the file implementation"
            resolver instanceof LicenceFileResolver
    }

    def "Get resolver for Windows file location"() {
        given: "a file location"
            def location = 'C:\\Home\\Ben\\clover\\clover.license'

        when: "trying to get the license resolver"
            LicenseResolver resolver = LicenseResolverFactory.instance.getResolver(location)

        then: "the retrieved resolver is the file implementation"
            resolver instanceof LicenceFileResolver
    }
}
