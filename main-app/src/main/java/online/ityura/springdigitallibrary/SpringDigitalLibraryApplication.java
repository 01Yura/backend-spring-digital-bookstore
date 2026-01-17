package online.ityura.springdigitallibrary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class SpringDigitalLibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringDigitalLibraryApplication.class, args);
    }
}
