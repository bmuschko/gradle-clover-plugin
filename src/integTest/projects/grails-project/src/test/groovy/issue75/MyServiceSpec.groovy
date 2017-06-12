package issue75

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(MyService)
class MyServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test something"() {
        when:
            def result = service.serviceMethod()

        then:
            result == 'hit main service branch'
    }
}
