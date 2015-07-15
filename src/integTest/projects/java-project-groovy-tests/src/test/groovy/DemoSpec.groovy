import spock.lang.Specification

class DemoSpec extends Specification {

    def "The Demo Happens"() {
        expect:
        new Demo().toString() == "Hello Demo"
    }
}
