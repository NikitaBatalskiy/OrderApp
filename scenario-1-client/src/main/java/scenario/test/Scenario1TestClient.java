package scenario.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@SpringBootApplication
@RequiredArgsConstructor
public class Scenario1TestClient implements CommandLineRunner {

  @Value("${order.service.url}")
  private String baseUrl;
  private final RestTemplate restTemplate = new RestTemplate();

  public static void main(String[] args) {
    SpringApplication.run(Scenario1TestClient.class, args);
    Runtime.getRuntime().halt(0);
  }

  @Override
  public void run(String... args) {
    log.info("=== Automation Scenario 1 run: N+1 equal orders ===");

    var randomInt = new Random().nextInt(1, 1000);
    var supplierId = createTestClient("Supplier Test1", "supplier" + randomInt + "@test.com");
    var consumerId = createTestClient("Consumer Test1", "consumer" + randomInt + "@test.com");

    testDuplicateOrdersScenario(supplierId, consumerId);

    log.info("=== Scenario 1 completed ===");
  }

  private void testDuplicateOrdersScenario(Long supplierId, Long consumerId) {
    log.info("Testing the creation of 20 identical orders in parallel...");

    var executor = Executors.newFixedThreadPool(25);
    var successCount = new AtomicInteger(0);
    var errorCount = new AtomicInteger(0);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
      final int requestNum = i + 1;

      var future = CompletableFuture.runAsync(() -> {
        try {
          createOrder(supplierId, consumerId);
          successCount.incrementAndGet();
          log.info("Order #{} successfully created", requestNum);
        } catch (Exception e) {
          errorCount.incrementAndGet();
          log.info("Order #{} rejected: {}", requestNum, e.getMessage());
        }
      }, executor);

      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();

    log.info("=== SCENARIO 1 RESULTS ===");
    log.info("Successfully created orders: {}", successCount.get());
    log.info("Rejected orders: {}", errorCount.get());

    if (successCount.get() == 1 && errorCount.get() == 19) {
      log.info("✅ SCENARIO 1 SUCCESSFUL: only 1 out of 20 orders was created");
    } else {
      log.error("❌ SCENARIO 1 FAILED: 1 successful order expected and received {}",
          successCount.get());
    }

    checkClientProfits(supplierId, consumerId);
  }

  private Long createTestClient(String name, String email) {
    try {
      var headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      var requestBody = String.format("""
          {
              "name": "%s",
              "email": "%s",
              "address": "Test Address",
              "active": "true"
          }
          """, name, email);

      var request = new HttpEntity<>(requestBody, headers);
      var response = restTemplate.postForEntity(
          baseUrl + "/clients/createClient", request, Map.class);

      log.info("Test client created: {}", name);
      return Long.valueOf(Objects.requireNonNull(response.getBody()).get("id").toString());

    } catch (Exception e) {
      log.error("Error creating client: {}", e.getMessage());
      throw new RuntimeException("Failed to create test client", e);
    }
  }

  private void createOrder(Long supplierId, Long consumerId) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    var requestBody = String.format("""
        {
            "title": "Test Order Duplicate",
            "supplierId": %d,
            "consumerId": %d,
            "price": %s
        }
        """, supplierId, consumerId, BigDecimal.ONE);

    var request = new HttpEntity<>(requestBody, headers);

    try {
      restTemplate.postForEntity(
          baseUrl + "/orders/createOrder", request, String.class);
    } catch (HttpClientErrorException e) {
      throw new RuntimeException("Order rejected: " + e.getResponseBodyAsString());
    }
  }

  private void checkClientProfits(Long supplierId, Long consumerId) {
    try {
      var supplierProfit = getClientProfit(supplierId);
      var consumerProfit = getClientProfit(consumerId);

      log.info("Supplier's final profit (ID={}): {}", supplierId, supplierProfit);
      log.info("Consumer's final profit (ID={}): {}", consumerId, consumerProfit);

      if (supplierProfit.compareTo(BigDecimal.ONE) == 0 &&
          consumerProfit.compareTo(BigDecimal.ONE.negate()) == 0) {
        log.info("✅ Clients' profits are correct");
      } else {
        log.warn("⚠️ Clients' profits are incorrect");
      }

    } catch (Exception e) {
      log.error("Profit check error: {}", e.getMessage());
    }
  }

  private BigDecimal getClientProfit(Long clientId) {
    try {
      var response = restTemplate.getForEntity(
          baseUrl + "/clients/getProfit/" + clientId, String.class);
      return new BigDecimal(Objects.requireNonNull(response.getBody()));
    } catch (Exception e) {
      log.error("Error getting client profit {}: {}", clientId, e.getMessage());
      return BigDecimal.ZERO;
    }
  }
}
