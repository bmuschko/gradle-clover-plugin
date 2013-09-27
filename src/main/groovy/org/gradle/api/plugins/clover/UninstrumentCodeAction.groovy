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
package org.gradle.api.plugins.clover

import java.io.File;

import groovy.util.logging.Slf4j
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory

/**
 * Clover code instrumentation action.
 *
 * @author Benjamin Muschko
 */
@Slf4j
class UninstrumentCodeAction implements Action<Task> {
	
	File classesDir
	File testClassesDir
	File classesBackupDir
	File testClassesBackupDir

    @Override
    void execute(Task task) {
		log.info 'Starting to restore original files.'
		
		def ant = new AntBuilder()
        
		if (getClassesBackupDir().exists()) {
			ant.delete(includeEmptyDirs: true) {
	            fileset(dir: getClassesDir().canonicalPath, includes: '**/*')
	        }
			ant.move(file: getClassesBackupDir().canonicalPath, tofile: getClassesDir().canonicalPath, failonerror: true)
		}

		if (getTestClassesBackupDir().exists()) {
			ant.delete(includeEmptyDirs: true) {
	            fileset(dir: getTestClassesDir().canonicalPath, includes: '**/*')
	        }
	        ant.move(file: getTestClassesBackupDir().canonicalPath, tofile: getTestClassesDir().canonicalPath, failonerror: false)
		}
		
		log.info 'Finished restoring original files.'
    }
}
