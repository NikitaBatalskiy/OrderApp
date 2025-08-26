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
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Log4j2
@SpringBootApplication
@RequiredArgsConstructor
public class Scenario3TestClient implements CommandLineRunner {

  @Value("${order.service.url}")
  private String baseUrl;
  private final RestTemplate restTemplate = new RestTemplate();

  public static void main(String[] args) {
    SpringApplication.run(Scenario3TestClient.class, args);
    Runtime.getRuntime().halt(0);
  }

  @Override
  public void run(String... args) {
    log.info(
        "=== Automation Scenario 3 run: Client deactivation during order processing ===");

    var randomInt = new Random().nextInt(1, 1000);
    var supplierId = createTestClient("Supplier Test3", "supplier" + randomInt + "@test.com");
    var consumerId = createTestClient("Consumer Test3", "consumer" + randomInt + "@test.com");

    testClientDeactivationScenario(supplierId, consumerId);

    log.info("=== Scenario 3 completed ===");
  }

  private void testClientDeactivationScenario(Long supplierId, Long consumerId) {
    final int totalOrders = 10;

    log.info("Testing client deactivation during order processing...");
    log.info("Sending {} orders", totalOrders);
    log.info("We deactivate the consumer after delay of 10 seconds...");

    var executor = Executors.newFixedThreadPool(25);
    var successCount = new AtomicInteger(0);
    var errorCount = new AtomicInteger(0);
    List<CompletableFuture<Void>> orderFutures = new ArrayList<>();

    for (int i = 0; i < totalOrders; i++) {
      final int orderNum = i + 1;
      final var price = BigDecimal.valueOf(10 + (i * 5));

      var orderFuture = CompletableFuture.runAsync(() -> {
        try {
          var orderName = "DeactivationTest_Order_" + orderNum + "_Price_" + price;
          createOrder(orderName, supplierId, consumerId, price);
          successCount.incrementAndGet();
          log.info("✅ Order #{} (price={}) created successfully", orderNum, price);
        } catch (Exception e) {
          errorCount.incrementAndGet();
          log.info("❌ Order #{} (price={}) rejected: {}", orderNum, price, e.getMessage());
        }
      }, executor);

      orderFutures.add(orderFuture);
    }

    try {
      Thread.sleep(10000);
    } catch (InterruptedException e) {
      log.warn("Sleep interrupted", e);
    }

    var deactivationFuture = CompletableFuture.runAsync(() -> {
      try {
        log.warn("🚫 DEACTIVATING consumer (ID={})", consumerId);
        deactivateClient(consumerId);
        log.warn("🚫 Consumer successfully deactivated");
      } catch (Exception e) {
        log.error("Error deactivating client: {}", e.getMessage());
      }
    }, executor);

    CompletableFuture.allOf(orderFutures.toArray(new CompletableFuture[0])).join();
    deactivationFuture.join();
    executor.shutdown();

    log.info("=== SCENARIO 3 RESULT ===");
    log.info("Orders send: {}", totalOrders);
    log.info("Successfully created orders: {}", successCount.get());
    log.info("Rejected orders: {}", errorCount.get());

    if (successCount.get() > 0 && errorCount.get() > 0) {
      log.info(
          "✅ SCENARIO 3 SUCCESSFUL: some orders were created before deactivation, some were rejected");
      log.info("Orders created before deactivation: {}", successCount.get());
      log.info("Orders rejected after deactivation: {}", errorCount.get());
    } else if (successCount.get() == 0) {
      log.error(
          "❌ SCENARIO 3 FAILED: no orders were created (deactivation occurred too quickly)");
    } else if (errorCount.get() == 0) {
      log.error(
          "❌ SCENARIO 3 FAILED: all orders went through (deactivation did not work or happened too late)");
    }

    checkFinalState(supplierId, consumerId);
    listCreatedOrders(consumerId);
  }

  private void deactivateClient(Long clientId) {
    restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory());
    try {
      var url = baseUrl + "/clients/editClient/" + clientId + "?active=false";
      restTemplate.patchForObject(url, null, String.class);
    } catch (Exception e) {
      log.error("Error deactivating client {}: {}", clientId, e.getMessage());
      throw new RuntimeException("Failed to deactivate client", e);
    }
  }

  private void checkFinalState(Long supplierId, Long consumerId) {
    try {
      var supplierProfit = getClientProfit(supplierId);
      var consumerProfit = getClientProfit(consumerId);
      boolean consumerActive = isClientActive(consumerId);

      log.info("=== FINAL STATE ===");
      log.info("Supplier Profit (ID={}): {}", supplierId, supplierProfit);
      log.info("Consumer Profit (ID={}): {}", consumerId, consumerProfit);
      log.info("Consumer is active: {}", consumerActive);

      if (!consumerActive) {
        log.info("✅ The consumer has been correctly deactivated");
      } else {
        log.warn("⚠️ The consumer remained active");
      }

      var expectedConsumerProfit = supplierProfit.negate();
      if (consumerProfit.compareTo(expectedConsumerProfit) == 0) {
        log.info("✅ Profits are balanced correctly");
      } else {
        log.warn("⚠️ Profit imbalance: supplier={}, consumer={}", supplierProfit,
            consumerProfit);
      }

    } catch (Exception e) {
      log.error("Final state check error: {}", e.getMessage());
    }
  }

  private void listCreatedOrders(Long clientId) {
    try {
      var response = restTemplate.getForEntity(
          baseUrl + "/orders/getOrdersForClient/" + clientId, Map.class);

      @SuppressWarnings("unchecked")
      var purchases = (List<Map<String, Object>>) Objects.requireNonNull(response.getBody())
          .get("purchases");

      log.info("=== CREATED ORDERS ===");
      log.info("Total consumer orders: {}", purchases.size());

      for (int i = 0; i < purchases.size(); i++) {
        var order = purchases.get(i);
        log.info("Order {}: {} - price: {}",
            i + 1,
            order.get("title"),
            order.get("price"));
      }

    } catch (Exception e) {
      log.error("Error getting list of orders: {}", e.getMessage());
    }
  }

  private boolean isClientActive(Long clientId) {
    try {
      var response = restTemplate.getForEntity(
          baseUrl + "/clients/getClient/" + clientId, Map.class);
      return Boolean.TRUE.equals(Objects.requireNonNull(response.getBody()).get("active"));
    } catch (Exception e) {
      log.error("Error checking client status {}: {}", clientId, e.getMessage());
      return false;
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
      log.debug("Order created successfully: {}", response.getStatusCode());
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
      log.error("Error getting client profit {}: {}", clientId, e.getMessage());
      return BigDecimal.ZERO;
    }
  }
}
