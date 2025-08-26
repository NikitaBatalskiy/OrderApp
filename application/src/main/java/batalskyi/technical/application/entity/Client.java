package batalskyi.technical.application.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Email;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "clients")
@Getter
@NoArgsConstructor
public class Client {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Setter
  private String name;

  @Email
  @Column(nullable = false, unique = true)
  @Setter
  private String email;

  @Setter
  private String address;

  @Getter
  @Setter
  private BigDecimal profit = BigDecimal.ZERO;

  @Setter
  private boolean active = true;

  @Setter
  private LocalDateTime deactivatedAt;

  @Version
  private Long version;
}
