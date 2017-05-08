package example;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader=AnnotationConfigContextLoader.class)
public class ListenerIntegrationTest {

    @Configuration
    @ComponentScan("example")
    static class ContextConfiguration {}

    @Autowired
    private Listener listener;

    @Test
    public void testListener() {

        Assert.assertEquals("foo", listener.listen());
    }

}

