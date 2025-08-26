package batalskyi.technical.application.controller;

import batalskyi.technical.application.dto.OrderDTO;
import batalskyi.technical.application.dto.OrderResponseDTO;
import batalskyi.technical.application.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

  private final OrderService orderService;

  @Operation(summary = "Create order",
      description = "Create order.")
  @PostMapping("/createOrder")
  public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderDTO orderDTO) {
    return ResponseEntity.status(HttpStatus.OK).body(orderService.createOrder(orderDTO));
  }

  @Operation(summary = "Get orders",
      description = "Get list of orders.")
  @GetMapping("/getAllOrders")
  public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
    return ResponseEntity.status(HttpStatus.OK).body(orderService.getAllOrders());
  }

  @Operation(summary = "Get client's orders",
      description = "Get a list of orders in which this client participated.")
  @GetMapping("/getOrdersForClient/{clientId}")
  public ResponseEntity<Map<String, Object>> getOrdersByClient(@PathVariable Long clientId) {
    return ResponseEntity.status(HttpStatus.OK).body(orderService.getAllOrdersByClientId(clientId));
  }
}
