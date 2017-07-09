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

import groovy.transform.CompileStatic

/**
 * License resolver factory.
 *
 * @author Benjamin Muschko
 */
@CompileStatic
@Singleton
class LicenseResolverFactory {
    LicenseResolver getResolver(String location) {
        if(!location || location == '') {
            return new LicenceDefaultResolver()
        }

        if(location.indexOf('://') != -1) {
            return new LicenseUrlResolver()
        }
        else {
            return new LicenceFileResolver()
        }
    }
}
