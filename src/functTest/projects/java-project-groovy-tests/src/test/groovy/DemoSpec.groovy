import spock.lang.Specification

class DemoSpec extends Specification {

    def "The Demo Happens"() {
        expect:
        new Demo().toString() == "Hello Demo"
    }

    def "Test for UTF-8 corruption during instrumentation"() {
        expect:
        new Demo().utf8StringIssue92() == "Sprawdź uszkodzenie UTF-8 podczas oprzyrządowania"
    }
}
