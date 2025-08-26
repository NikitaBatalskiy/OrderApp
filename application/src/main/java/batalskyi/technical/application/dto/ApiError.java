package batalskyi.technical.application.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiError {

  private int status;
  private String message;
  private LocalDateTime timestamp = LocalDateTime.now();

  public ApiError(int status, String message) {
    this.status = status;
    this.message = message;
  }
}
