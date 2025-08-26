package batalskyi.technical.application.repository;

import batalskyi.technical.application.entity.Client;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends
    JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

  Optional<Client> findByEmail(String email);

}
