package batalskyi.technical.application.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientDTO {

  private Long id;

  private String name;

  private String email;

  private String address;

  private boolean active;

  private LocalDateTime deactivatedAt;
}
