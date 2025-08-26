package batalskyi.technical.application.dto;

import batalskyi.technical.application.entity.Client;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonPropertyOrder({"id", "name", "email", "address", "active", "profit", "deactivatedAt"})
public class ClientWithProfitDTO {

  private Long id;

  private String name;

  private String email;

  private String address;

  private boolean active;

  private BigDecimal profit;

  private LocalDateTime deactivatedAt;

  public ClientWithProfitDTO(Client client, BigDecimal profit) {
    this.id = client.getId();
    this.name = client.getName();
    this.email = client.getEmail();
    this.address = client.getAddress();
    this.active = client.isActive();
    this.deactivatedAt = client.getDeactivatedAt();
    this.profit = profit;
  }
}
