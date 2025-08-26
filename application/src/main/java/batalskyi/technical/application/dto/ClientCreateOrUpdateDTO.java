package batalskyi.technical.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClientCreateOrUpdateDTO {

  @NotBlank
  private String name;

  @Email
  private String email;

  @NotBlank
  private String address;

  @NotNull
  private boolean active;

}
