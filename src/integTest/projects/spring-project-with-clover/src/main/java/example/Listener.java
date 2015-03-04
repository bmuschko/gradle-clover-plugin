package example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Listener {

    private Talker talker;

    @Autowired
    public void setTalker(Talker fooTalker) {

        this.talker = fooTalker;
    }

    public String listen() {

        return talker.talk();
    }
}
