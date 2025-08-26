package batalskyi.technical.application.repository;

import batalskyi.technical.application.entity.Order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

  List<Order> findBySupplierId(Long supplierId);

  List<Order> findByConsumerId(Long consumerId);

  boolean existsByTitleAndSupplierIdAndConsumerId(String title, Long supplierId, Long consumerId);
}
