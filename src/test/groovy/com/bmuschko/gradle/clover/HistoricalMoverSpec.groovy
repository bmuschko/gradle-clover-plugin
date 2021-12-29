package com.bmuschko.gradle.clover

import spock.lang.Specification

class HistoricalMoverSpec extends Specification {
    def 'Validate JSON serialized form is correct'() {
        given: 'A configured object'
        HistoricalMover mover = new HistoricalMover()
        mover.threshold = 1
        mover.range = 1
        mover.interval = 'foo'

        when: 'Converted to JSON'
        String json = mover.toJson()

        then: 'Valid JSON is returned'
        json == '{"threshold":1,"range":1,"interval":"foo"}'
    }

    def 'Validate JSON deserialized is correct'() {
        given: 'A JSON form'
        String json = '{"threshold":1,"range":1,"interval":"foo"}'

        when: 'Deserialized to object'
        def mover = HistoricalMover.fromJson(json)

        then: 'A valid object is returned'
        mover.threshold == 1
        mover.range == 1
        mover.interval == 'foo'
    }
}
