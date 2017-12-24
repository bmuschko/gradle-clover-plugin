import spock.lang.Specification

class GroovyBookSpec extends Specification {

    def "The exception thrown has line numbers in the StackTrace"() {
        given:
        def groovyBook = new GroovyBook()

        when:
        groovyBook.open()

        then:
        def e = thrown(Exception)
        e.message == "Testing Debug Stacktraces"
        e.stackTrace[0].lineNumber != -1
    }
}
