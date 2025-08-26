package batalskyi.technical.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class OrderApp {

  public static void main(String[] args) {
    SpringApplication.run(OrderApp.class, args);
  }

}
