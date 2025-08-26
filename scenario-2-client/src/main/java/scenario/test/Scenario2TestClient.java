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
public class Scenario2TestClient implements CommandLineRunner {

  @Value("${order.service.url}")
  private String baseUrl;
  private final RestTemplate restTemplate = new RestTemplate();

  public static void main(String[] args) {
    SpringApplication.run(Scenario2TestClient.class, args);
    Runtime.getRuntime().halt(0);
  }

  @Override
  public void run(String... args) {
    log.info("=== Automation Scenario 2 run: Profit limitation -1000 ===");

    var randomInt = new Random().nextInt(1, 1000);
    var supplierId = createTestClient("Supplier Test2", "supplier" + randomInt + "@test.com");
    var consumerId = createTestClient("Consumer Test2", "consumer" + randomInt + "@test.com");

    setInitialConsumerProfit(consumerId, new BigDecimal("-970"), randomInt);

    testProfitLimitScenario(supplierId, consumerId);

    log.info("=== Scenario 2 completed ===");
  }

  private void testProfitLimitScenario(Long supplierId, Long consumerId) {
    log.info("Testing order creation with profit limitation -1000...");
    log.info("Consumer's starting profit: -970");
    log.info("Sending 10 orders: price goes from 100 to 10 (step -10)");

    var executor = Executors.newFixedThreadPool(12);
    var successCount = new AtomicInteger(0);
    var errorCount = new AtomicInteger(0);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      final int orderNum = i + 1;
      final var price = BigDecimal.valueOf(100 - (i * 10));

      var future = CompletableFuture.runAsync(() -> {
        try {
          var orderName = "Test Order";
          createOrder(orderName, supplierId, consumerId, price);
          successCount.incrementAndGet();
          log.info("Order #{} (price={}) created successfully", orderNum, price);
        } catch (Exception e) {
          errorCount.incrementAndGet();
          log.info("Order #{} (price={}) rejected: {}", orderNum, price, e.getMessage());
        }
      }, executor);

      futures.add(future);
    }

    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    executor.shutdown();

    log.info("=== SCENARIO 2 RESULTS ===");
    log.info("Successfully created orders: {}", successCount.get());
    log.info("Rejected orders: {}", errorCount.get());

    if (successCount.get() == 1) {
      log.info("✅ SCENARIO 2 SUCCESSFUL: only 1 out of 10 orders was created");

      var finalConsumerProfit = getClientProfit(consumerId);
      var orderAmount = finalConsumerProfit.add(
          BigDecimal.valueOf(970));
      log.info("Successful order price: {}", orderAmount.abs());

    } else {
      log.error("❌ SCENARIO 2 FAILED: was expected 1 order, created {}",
          successCount.get());
    }

    checkFinalState(supplierId, consumerId);
  }

  private void setInitialConsumerProfit(Long consumerId, BigDecimal targetProfit, int randomInt) {
    log.info("Setting client profit: {}", targetProfit);

    try {
      var tempSupplierId = createTestClient("Temp Supplier", "temp" + randomInt + "@test.com");

      var setupOrderPrice = targetProfit.abs();
      createOrder("Setup Order", tempSupplierId, consumerId, setupOrderPrice);

      log.info("Starting profit is set via order with price: {}", setupOrderPrice);

    } catch (Exception e) {
      log.error("Error setting client's profit: {}", e.getMessage());
    }
  }

  private void checkFinalState(Long supplierId, Long consumerId) {
    try {
      var supplierProfit = getClientProfit(supplierId);
      var consumerProfit = getClientProfit(consumerId);

      log.info("=== FINAL STATE ===");
      log.info("Supplier's profit (ID={}): {}", supplierId, supplierProfit);
      log.info("Consumer's profit (ID={}): {}", consumerId, consumerProfit);

      if (consumerProfit.compareTo(BigDecimal.valueOf(-1000)) >= 0) {
        log.info("✅ Profit limit was not exceeded");
      } else {
        log.error("❌ Profit limit violated! Consumer's profit: {}", consumerProfit);
      }

      var totalChange = consumerProfit.add(
          BigDecimal.valueOf(970));
      log.info("Total change in consumer profit: {}", totalChange);

    } catch (Exception e) {
      log.error("Final state check error: {}", e.getMessage());
    }
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

  private void createOrder(String title, Long supplierId, Long consumerId, BigDecimal price) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    var requestBody = String.format("""
        {
            "title": "%s",
            "supplierId": %d,
            "consumerId": %d,
            "price": %s
        }
        """, title, supplierId, consumerId, price.toString());

    var request = new HttpEntity<>(requestBody, headers);

    try {
      var response = restTemplate.postForEntity(
          baseUrl + "/orders/createOrder", request, String.class);
      log.debug("Order created successfully\n: {}", response.getStatusCode());
    } catch (HttpClientErrorException e) {
      log.debug("Order rejected: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
      throw new RuntimeException("Order rejected: " + e.getResponseBodyAsString());
    }
  }

  private BigDecimal getClientProfit(Long clientId) {
    try {
      var response = restTemplate.getForEntity(
          baseUrl + "/clients/getProfit/" + clientId, String.class);
      return new BigDecimal(Objects.requireNonNull(response.getBody()));
    } catch (Exception e) {
      log.error("Error retrieving client's profit {}: {}", clientId, e.getMessage());
      return BigDecimal.ZERO;
    }
  }
}
