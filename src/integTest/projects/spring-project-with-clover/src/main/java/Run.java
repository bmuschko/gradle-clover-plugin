import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import example.Listener;

public class Run {

    public static void main(String... args) {

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        Listener listener = context.getBean(Listener.class);
        listener.listen();
    }

}
