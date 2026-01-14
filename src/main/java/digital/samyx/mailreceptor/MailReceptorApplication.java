package digital.samyx.mailreceptor;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

//@EnableScheduling
@SpringBootApplication
public class MailReceptorApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Costa_Rica"));
    }

    public static void main(String[] args) {
        SpringApplication.run(MailReceptorApplication.class, args);
    }

}
