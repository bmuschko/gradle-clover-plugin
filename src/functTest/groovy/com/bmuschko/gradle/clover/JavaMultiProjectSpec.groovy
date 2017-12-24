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

import spock.lang.Unroll

class JavaMultiProjectSpec extends AbstractFunctionalTestBase {

    @Unroll def"Build a Java multi-project (with Gradle Version #gradle)"()
    {
        given: "a Java multi-project"
        projectName = 'java-multi-project'
        gradleVersion = gradle

        when: "the top-level Clover aggregation task is run"
        build('--init-script', "$initScript", 'clean', 'cloverAggregateReports')

        then: "the aggregated Clover coverage database is generated"
        cloverDb.exists()

        and: "the aggregated Clover report is generated and is correct"
        cloverXmlReport.exists()
        def coverage = new XmlSlurper().parse(cloverXmlReport)
        coverage.project.package.file.size() == 2
        coverage.project.package.file[0].@name == 'Car.java'
        coverage.project.package.file[1].@name == 'Truck.java'
        !coverage.project.package.file*.@name.contains('Motorbike.java')
        coverage.testproject.package.file.size() == 2
        coverage.testproject.package.file[0].@name == 'CarTest.java'
        coverage.testproject.package.file[1].@name == 'TruckTest.java'
        !coverage.testproject.package.file*.@name.contains('MotorbikeTest.java')
        cloverHtmlReport.exists()
        cloverJsonReport.exists()
        cloverPdfReport.exists()

        and: "the Clover snapshot is not generated because test optimization is not enabled"
        cloverSnapshot.exists() == false

        where:
        gradle << GRADLE_TEST_VERSIONS
    }
}
