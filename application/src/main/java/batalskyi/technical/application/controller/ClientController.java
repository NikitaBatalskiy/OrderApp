package batalskyi.technical.application.controller;

import batalskyi.technical.application.dto.ClientCreateOrUpdateDTO;
import batalskyi.technical.application.dto.ClientDTO;
import batalskyi.technical.application.entity.Client;
import batalskyi.technical.application.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/clients")
public class ClientController {

  private final ClientService clientService;

  @Operation(summary = "Create client",
      description = "Create new Client.")
  @PostMapping("/createClient")
  public ResponseEntity<ClientDTO> createClient(
      @Valid @RequestBody ClientCreateOrUpdateDTO clientDTO) {
    return ResponseEntity.status(HttpStatus.CREATED).body(clientService.createClient(clientDTO));
  }

  @Operation(summary = "Get clients",
      description = "Get list of clients.")
  @GetMapping("/getAllClients")
  public ResponseEntity<List<ClientDTO>> getAllClients() {
    return ResponseEntity.status(HttpStatus.OK).body(clientService.getAllClients());
  }

  @Operation(summary = "Find clients",
      description = "Filter clients by name, email or address."
          + " One filter at a time, at least 3 symbols to filter list.")
  @GetMapping("/findClients")
  public ResponseEntity<List<ClientDTO>> searchClients(@RequestParam String field,
      @RequestParam String text) {
    return ResponseEntity.status(HttpStatus.OK).body(clientService.searchClients(field, text));
  }

  @Operation(summary = "Get client",
      description = "Get client by his id.")
  @GetMapping("/getClient/{id}")
  public ResponseEntity<Client> getClient(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.OK).body(clientService.getClientById(id));
  }

  @Operation(summary = "Update client",
      description = "Update client by his id, at least 1 attribute should be passed.")
  @PatchMapping("/editClient/{id}")
  public ResponseEntity<ClientDTO> editClient(
      @PathVariable Long id,
      @RequestParam(required = false) String name,
      @Valid @RequestParam(required = false) String email,
      @RequestParam(required = false) String address,
      @RequestParam(required = false) Boolean active
  ) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(clientService.updateClient(id, name, email, address, active));
  }

  @Operation(summary = "Get client's profit",
      description = "Get profit of the client by his id.")
  @GetMapping("/getProfit/{id}")
  public ResponseEntity<BigDecimal> getProfit(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.OK).body(clientService.getProfitById(id));
  }

  @Operation(summary = "Get clients in the profit range",
      description = "Get list of clients that have profit in specific range.")
  @GetMapping("/searchClientsInProfitRange")
  public ResponseEntity<List<ClientDTO>> searchClientsInProfitRange(@RequestParam BigDecimal min,
      @RequestParam BigDecimal max) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(clientService.searchClientsInProfitRange(min, max));
  }
}
