package example;

import org.springframework.stereotype.Component;

@Component
public class FooTalker implements Talker {

    @Override
    public String talk() {

        return "foo";
    }
}
