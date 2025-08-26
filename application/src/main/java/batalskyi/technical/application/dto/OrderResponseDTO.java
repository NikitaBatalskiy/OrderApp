package batalskyi.technical.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderResponseDTO {

  @NotBlank
  private Long id;

  @NotBlank
  private String title;

  @NotBlank
  private Long supplierId;

  @NotBlank
  private Long consumerId;

  @NotBlank
  private BigDecimal price;

  @NotBlank
  private LocalDateTime processingStartTime;

  @NotBlank
  private LocalDateTime processingEndTime;

  @NotBlank
  private LocalDateTime createdAt;

}
