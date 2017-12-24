/*
 * Copyright 2012 the original author or authors.
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

import com.bmuschko.gradle.clover.CloverSourceSet

import groovy.transform.CompileStatic

/**
 * Clover source set utilities.
 *
 * @author Benjamin Muschko
 */
@CompileStatic
final class CloverSourceSetUtils {
    private CloverSourceSetUtils() {}

    static List<File> getSourceDirs(Collection<CloverSourceSet> sourceSets) {
        def srcDirs = []

        sourceSets.each { sourceSet ->
            srcDirs.addAll(sourceSet.srcDirs)
        }

        srcDirs
    }

    static boolean existsDirectory(File dir) {
        dir && dir.exists()
    }
}