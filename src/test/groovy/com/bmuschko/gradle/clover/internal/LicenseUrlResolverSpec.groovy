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
package com.bmuschko.gradle.clover.internal

import spock.lang.Specification

/**
 * License URL resolver tests.
 *
 * @author Benjamin Muschko
 */
class LicenseUrlResolverSpec extends Specification {
    private final File testDir = new File('build/tmp/tests')

    def "Resolve file for existing Clover license file"() {
        setup: "create license file"
            if(testDir.exists()) {
                testDir.deleteDir()
            }

            testDir.mkdirs()
            File existingLicenseFile = new File(testDir, LicenseResolver.DEFAULT_CLOVER_LICENSE_FILE_NAME)
            existingLicenseFile.createNewFile()

        and: "the project root directory and location"
            def location = 'someCloverFile.license'

        when: "getting the license file"
            File file = new LicenseUrlResolver().resolve(testDir, location)

        then: "the file matches the expected path"
            file.path == new File('build/tmp/tests/clover.license').path

        cleanup: "delete license file"
            testDir.deleteDir()
    }

    def "Resolve license from URL"() {
        setup: "create test directory"
            if(testDir.exists()) {
                testDir.deleteDir()
            }

            testDir.mkdirs()

        and: "the project root directory and location"
            def location = 'https://raw.github.com/bmuschko/gradle-clover-plugin/master/clover.license'

        when: "getting the license file"
            File file = new LicenseUrlResolver().resolve(testDir, location)

        then: "the file matches the expected path"
            file.path == new File('build/tmp/tests/clover.license').path

        and: "content matches"
            file.text ==
"""pnNRMjgrANeRKrxwQwbcprhwQQNJAjfnRpSFTlUwumbftm
mi2KH8pKYC0wOhgO4qyk1LFU2Kqr2UzItE2o5lNmhw5rdq
nrMOSsrpPoqPprmoRmpstMORNpQqoppNmnVSvWSvVuOvtS
WTwOxxsuXwxWnMqqmnmqmUUnptvrvvommmmmUUnptvrvvo
mmmmmUU7oXaibW3ilsboWGirdfkUUnmmmm
"""

        cleanup: "delete license file"
            testDir.deleteDir()
    }
}
