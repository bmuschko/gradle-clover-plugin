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
 * License default resolver tests.
 *
 * @author Benjamin Muschko
 */
class LicenceDefaultResolverTest extends Specification {
    def "Resolve file"() {
        given: "the project root directory and location"
            def projectRootDir = new File('/home/ben')
            def location = 'clover.license'

        when: "getting the license file"
            File file = new LicenceDefaultResolver().resolve(projectRootDir, location)

        then: "the file matches the expected path"
            file.absolutePath == new File('/home/ben/clover.license').absolutePath
    }
}
