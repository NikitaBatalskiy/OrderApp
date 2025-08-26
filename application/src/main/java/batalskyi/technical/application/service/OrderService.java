package batalskyi.technical.application.service;

import batalskyi.technical.application.dto.OrderDTO;
import batalskyi.technical.application.dto.OrderResponseDTO;
import batalskyi.technical.application.entity.Client;
import batalskyi.technical.application.entity.Order;
import batalskyi.technical.application.exception.AttributeMismatchException;
import batalskyi.technical.application.exception.ClientNotActiveException;
import batalskyi.technical.application.exception.ClientProfitLimitExceededException;
import batalskyi.technical.application.exception.DuplicateOrderException;
import batalskyi.technical.application.exception.InvalidPriceException;
import batalskyi.technical.application.mapper.OrderMapper;
import batalskyi.technical.application.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.StaleObjectStateException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderService {

  @Value("${consumer.limit.value}")
  private BigDecimal limit;

  private final OrderRepository orderRepository;
  private final ClientService clientService;
  private final OrderMapper orderMapper;
  private final EntityManager entityManager;

  @Transactional
  public OrderResponseDTO createOrder(OrderDTO orderDTO) {
    var supplier = clientService.getClientById(orderDTO.getSupplierId());
    var consumer = clientService.getClientById(orderDTO.getConsumerId());
    validateOrder(orderDTO, supplier, consumer);

    try {
      var orderPrice = orderDTO.getPrice();
      var order = new Order();
      order.setTitle(orderDTO.getTitle());
      order.setPrice(orderPrice);
      order.setSupplier(supplier);
      order.setConsumer(consumer);
      order.setProcessingStartTime(LocalDateTime.now());

      log.info("Processing order.");
      var delay = (new Random().nextInt(10) + 1) * 1000;
      Thread.sleep(delay);
      log.info(
          "Initiating additional activity check on clients with ids {} and {}. Refreshing clients state.",
          supplier.getId(), consumer.getId());
      entityManager.refresh(supplier);
      entityManager.refresh(consumer);
      checkClientsActivity(supplier, consumer);
      order.setProcessingEndTime(LocalDateTime.now());
      log.info("Finished processing order.");
      return orderMapper.toOrderResponseDto(orderRepository.save(order));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (DataIntegrityViolationException e) {
      log.error("Order with this business key already exists");
      throw new DuplicateOrderException("Order with this business key already exists");
    } catch (StaleObjectStateException e) {
      log.error("Transaction locked changes: {}", e.getMessage());
      throw new StaleObjectStateException(e.getMessage(), e);
    }
  }

  private void validateOrder(OrderDTO orderDTO, Client supplier, Client consumer) {
    log.info("Validating order..");
    var orderPrice = orderDTO.getPrice();
    if (orderPrice.compareTo(BigDecimal.ZERO) <= 0) {
      log.error("Order price is less than or equal to 1.");
      throw new InvalidPriceException("Order price must be equal or greater than 1");
    }

    var supplierId = orderDTO.getSupplierId();
    var consumerId = orderDTO.getConsumerId();

    if (isOrderExists(orderDTO.getTitle(), supplierId, consumerId)) {
      log.error("Order with business key already exists: {} + {} + {}",
          orderDTO.getTitle(), supplierId, consumerId);
      throw new DuplicateOrderException("Order with this business key already exists");
    }
    if (supplierId.equals(consumerId)) {
      log.error("Supplier Id and Consumer Id can't be the same.");
      throw new AttributeMismatchException("Supplier Id and Consumer Id cannot be the same");
    }

    checkClientsActivity(supplier, consumer);

    if (clientService.wouldExceedProfitLimit(consumerId, orderPrice)) {
      var currentProfit = clientService.calculateClientProfit(consumerId);
      log.error("Consumer's profit is {}, must not be less than -{} after the order.",
          currentProfit, limit);
      throw new ClientProfitLimitExceededException("Consumer's profit limit exceeded");
    }
    log.info("Validation finished.");
  }

  private void checkClientsActivity(Client supplier, Client consumer) {
    if (!consumer.isActive()) {
      log.error("Consumer with id {} is not active.", consumer.getId());
      throw new ClientNotActiveException("Consumer is not active");
    }
    if (!supplier.isActive()) {
      log.error("Supplier with id {} is not active.", supplier.getId());
      throw new ClientNotActiveException("Supplier is not active");
    }
  }

  private boolean isOrderExists(String title, Long supplierId, Long consumerId) {
    return orderRepository.existsByTitleAndSupplierIdAndConsumerId(title, supplierId, consumerId);
  }

  @Transactional(readOnly = true)
  public List<OrderResponseDTO> getAllOrders() {
    return orderRepository.findAll().stream().map(orderMapper::toOrderResponseDto).toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getAllOrdersByClientId(Long clientId) {
    log.info("Collecting client's orders.");
    var sales = orderRepository.findBySupplierId(clientId)
        .stream()
        .map(orderMapper::toOrderResponseDto)
        .toList();
    var purchases = orderRepository.findByConsumerId(clientId)
        .stream()
        .map(orderMapper::toOrderResponseDto)
        .toList();

    Map<String, Object> result = new HashMap<>();
    result.put("sales", sales);
    result.put("purchases", purchases);
    return result;
  }

  public List<Order> getOrdersByConsumerId(Long clientId) {
    return orderRepository.findByConsumerId(clientId);
  }

  public List<Order> getOrdersBySupplierId(Long clientId) {
    return orderRepository.findBySupplierId(clientId);
  }
}
