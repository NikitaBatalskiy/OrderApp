package batalskyi.technical.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderDTO {

  @NotBlank
  private String title;

  @NotBlank
  private Long supplierId;

  @NotBlank
  private Long consumerId;

  @NotBlank
  @Min(1)
  private BigDecimal price;
}
