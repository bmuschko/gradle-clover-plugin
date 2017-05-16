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

/**
 * License default resolver.
 *
 * @author Benjamin Muschko
 */
class LicenceDefaultResolver implements LicenseResolver {
    @Override
    File resolve(File projectRootDir, String location) {
        File license = new File(projectRootDir, DEFAULT_CLOVER_LICENSE_FILE_NAME)
        if (license.file) {
            return license
        }
        // Do not provide a non-existent file by default, will allow using with
        // OpenClover and retain most of the original behavior.
        return null
    }
}
