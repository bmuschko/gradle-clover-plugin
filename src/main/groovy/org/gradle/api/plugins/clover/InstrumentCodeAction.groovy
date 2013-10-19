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
class InstrumentCodeAction implements Action<Task> {
    String initString
    Boolean compileGroovy
    FileCollection cloverClasspath
	FileCollection testCompileClasspath
	FileCollection compileClasspath
    FileCollection groovyClasspath
    @OutputDirectory File classesBackupDir
    @OutputDirectory File testClassesBackupDir
    @InputFile File licenseFile
    @InputDirectory File buildDir
    @InputDirectory File classesDir
    @InputDirectory File testClassesDir
    Set<File> srcDirs
    Set<File> testSrcDirs
    String sourceCompatibility
    String targetCompatibility
    List<String> includes
    List<String> excludes
    List<String> testIncludes
    def statementContexts
    def methodContexts
	Boolean instrumentTestClasses = true
	Boolean performClean = true

    @Override
    void execute(Task task) {
        instrumentCode()
    }

    void instrumentCode() {
        log.info 'Starting to instrument code using Clover.'

        def ant = new AntBuilder()
        ant.taskdef(resource: 'cloverlib.xml', classpath: getCloverClasspath().asPath)
        ant.property(name: 'clover.license.path', value: getLicenseFile().canonicalPath)
		
		if (performClean) {
			ant."clover-clean"(initString: "${getBuildDir()}/${getInitString()}")
		}

        ant.'clover-setup'(initString: "${getBuildDir()}/${getInitString()}") {
			
			if (instrumentClasses) {
				getSrcDirs().each { srcDir ->
					ant.fileset(dir: srcDir) {
						getIncludes().each { include ->
							ant.include(name: include)
						}

						getExcludes().each { exclude ->
							ant.exclude(name: exclude)
						}
					}
				}
			}
			if (instrumentTestClasses) {
                getTestSrcDirs().each { testSrcDir ->
                    ant.fileset(dir: testSrcDir) {
                        getTestIncludes().each { include ->
                            ant.include(name: include)
                        }
                    }
                }
			}

            // Apply statement and method coverage contexts
            getStatementContexts().each {
                ant.statementContext(name: it.name, regexp: it.regexp)
            }

            getMethodContexts().each {
                ant.methodContext(name: it.name, regexp: it.regexp)
            }
        }

        // Move original classes
		if (instrumentClasses) {
            ant.move(file: getClassesDir().canonicalPath, tofile: getClassesBackupDir().canonicalPath, failonerror: true)
			getClassesDir().mkdirs()
		}
		
		if (instrumentTestClasses) {
			ant.move(file: getTestClassesDir().canonicalPath, tofile: getTestClassesBackupDir().canonicalPath, failonerror: false)
			getTestClassesDir().mkdirs()
		}

        // Compile instrumented classes
        compileClasses(ant)

        // Copy resources
		if (instrumentClasses) {
            ant.copy(todir: getClassesDir().canonicalPath, failonerror: true) {
                fileset(dir: getClassesBackupDir().canonicalPath, excludes: '**/*.class')
            }
		}
		if (instrumentTestClasses) {
            ant.copy(todir: getTestClassesDir().canonicalPath, failonerror: false) {
                fileset(dir: getTestClassesBackupDir().canonicalPath, excludes: '**/*.class')
            }
		}

        log.info 'Finished instrumenting code using Clover.'
    }
	
	public boolean getInstrumentClasses() {
		return getClassesDir().exists()
	}

    /**
     * Compiles Java classes. If project has Groovy plugin applied run the joint compiler.
     *
     * @param ant Ant builder
     */
    private void compileClasses(AntBuilder ant) {
        if(getCompileGroovy()) {
            ant.taskdef(name: 'groovyc', classname: 'org.codehaus.groovy.ant.Groovyc', classpath: getGroovyClasspath().asPath)

			if (instrumentClasses) {
				log.debug 'Instrumenting groovy and java src/main files'
				compileGroovyAndJavaSrcFiles(ant)
			}

            if (instrumentTestClasses && getTestSrcDirs().size() > 0) {
            	log.debug 'Instrumenting groovy and java src/test files'
                compileGroovyAndJavaTestSrcFiles(ant)
            }
        }
        else {
        	if (instrumentClasses) {
        		log.debug 'Instrumenting java src/main files'
				compileJavaSrcFiles(ant)
        	}

            if(instrumentTestClasses && getTestSrcDirs().size() > 0) {
				log.debug 'Instrumenting java src/test files'
                compileJavaTestSrcFiles(ant)
            }
        }
		addMarkerFile(getClassesDir())
    }

    /**
     * Gets Groovyc classpath (compile). Make sure the Groovy version defined in project is used on classpath first to avoid using the
     * default version bundled with Gradle.
     *
     * @return Classpath
     */
    private String getGroovyCompileClasspath() {
        getGroovyClasspath().asPath + System.getProperty('path.separator') + getCompileClasspath().asPath
    }
	
	/**
	 * Gets Groovyc classpath (testCompile). Make sure the Groovy version defined in project is used on classpath first to avoid using the
	 * default version bundled with Gradle.
	 *
	 * @return Classpath
	 */
	private String getGroovyTestCompileClasspath() {
		getGroovyClasspath().asPath + System.getProperty('path.separator') + getTestCompileClasspath().asPath
	}

	/**
	 * Gets Javac classpath (compile).
	 *
	 * @return Classpath
	 */
	private String getJavaCompileClasspath() {
		getCompileClasspath().asPath
	}
	
	/**
	 * Gets Javac classpath (testCompile)
	 *
	 * @return Classpath
	 */
	private String getJavaTestCompileClasspath() {
		getTestCompileClasspath().asPath
	}

    /**
     * Compiles Groovy and Java source files.
     *
     * @param ant Ant builder
     */
    private void compileGroovyAndJavaSrcFiles(AntBuilder ant) {
        compileGroovyAndJava(ant, getSrcDirs(), getClassesDir(), getGroovyCompileClasspath())
    }

    /**
     * Compiles Java source files.
     *
     * @param ant Ant builder
     */
    private void compileJavaSrcFiles(AntBuilder ant) {
        compileJava(ant, getSrcDirs(), getClassesDir(), getJavaCompileClasspath())
    }

    /**
     * Compiles Groovy and Java test source files.
     *
     * @param ant Ant builder
     */
    private void compileGroovyAndJavaTestSrcFiles(AntBuilder ant) {
        String classpath = addClassesDirToClasspath(getGroovyTestCompileClasspath())
        compileGroovyAndJava(ant, getTestSrcDirs(), getTestClassesDir(), classpath)
    }

    /**
     * Compiles Java test source files.
     *
     * @param ant Ant builder
     */
    private void compileJavaTestSrcFiles(AntBuilder ant) {
        String classpath = addClassesDirToClasspath(getJavaTestCompileClasspath())
        compileJava(ant, getTestSrcDirs(), getTestClassesDir(), classpath)
    }

    /**
     * Adds classes directory to classpath.
     *
     * @param classpath Classpath
     * @return Classpath
     */
    private String addClassesDirToClasspath(String classpath) {
        classpath + System.getProperty('path.separator') + getClassesDir().canonicalPath
    }

    /**
     * Compiles given Groovy and Java source files to destination directory.
     *
     * @param ant Ant builder
     * @param srcDirs Source directories
     * @param destDir Destination directory
     * @param classpath Classpath
     */
    private void compileGroovyAndJava(AntBuilder ant, Set<File> srcDirs, File destDir, String classpath) {
        ant.groovyc(destdir: destDir.canonicalPath, classpath: classpath) {
            srcDirs.each { srcDir ->
                src(path: srcDir)
            }

            ant.javac(source: getSourceCompatibility(), target: getTargetCompatibility())
        }
    }

    /**
     * Compiles given Java source files to destination directory.
     *
     * @param ant Ant builder
     * @param srcDirs Source directories
     * @param destDir Destination directory
     * @param classpath Classpath
     */
    private void compileJava(AntBuilder ant, Set<File> srcDirs, File destDir, String classpath) {
        ant.javac(destdir: destDir.canonicalPath, source: getSourceCompatibility(), target: getTargetCompatibility(),
                  classpath: classpath) {
            srcDirs.each { srcDir ->
                src(path: srcDir)
            }
        }
    }
	
	/**
	 * Adds a marker file to the destination directory.
	 */
	private void addMarkerFile(File destDir) {
		if (destDir.exists()) {
			File marker = new File(destDir.canonicalPath + "/clover.instrumented")
			log.debug 'Starting to write a marker file to: '+marker.canonicalPath
			PrintWriter writer = new PrintWriter(marker)
			writer.println("the classes in this directory are instrumented with clover")
			writer.close()
		} else {
			log.debug 'Do not write a marker file because directory does not exist: '+destDir.canonicalPath
		}
	}
}
