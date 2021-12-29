package com.bmuschko.gradle.clover

import spock.lang.Specification

class HistoricalAddedSpec extends Specification {
    def 'Validate JSON serialized form is correct'() {
        given: 'A configured object'
        HistoricalAdded added = new HistoricalAdded()
        added.interval = 'foo'
        added.range = 1

        when: 'Converted to JSON'
        String json = added.toJson()

        then: 'Valid JSON is returned'
        json == '{"range":1,"interval":"foo"}'
    }

    def 'Validate JSON deserialized is correct'() {
        given: 'A JSON form'
        String json = '{"range":1,"interval":"foo"}'

        when: 'Deserialized to object'
        def added = HistoricalAdded.fromJson(json)

        then: 'A valid object is returned'
        added.range == 1
        added.interval == 'foo'
    }
}
