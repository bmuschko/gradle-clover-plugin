package example;

import org.springframework.stereotype.Component;

@Component
public class BarTalker implements Talker {

    @Override
    public String talk() {

        return "bar";
    }
}
