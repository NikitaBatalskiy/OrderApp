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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Log4j2
@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final ClientService clientService;
  private final OrderMapper orderMapper;

  @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 5)
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
      supplier.setProfit(supplier.getProfit().add(orderPrice));
      consumer.setProfit(consumer.getProfit().subtract(orderPrice));

      log.info("Processing order.");
      var delay = (new Random().nextInt(10) + 1) * 1000;
      Thread.sleep(delay);
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

    if (!consumer.isActive()) {
      log.error("Consumer with id {} is not active.", consumerId);
      throw new ClientNotActiveException("Consumer is not active");
    }
    if (!supplier.isActive()) {
      log.error("Supplier with id {} is not active.", supplierId);
      throw new ClientNotActiveException("Supplier is not active");
    }
    log.info("Validation finished.");

    if (consumer.getProfit().subtract(orderPrice)
        .compareTo(BigDecimal.valueOf(-1000)) < 0) {
      log.error("Consumer's profit is {}, must not be less than -1000 after the order.",
          consumer.getProfit());
      throw new ClientProfitLimitExceededException("Consumer's profit must not be less than -1000");
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
}
