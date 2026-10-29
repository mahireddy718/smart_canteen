package com.canteen.repository;

import com.canteen.model.CanteenOrder;
import com.canteen.model.OrderStatus;
import com.canteen.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OrderRepository extends JpaRepository<CanteenOrder, Long> {
    List<CanteenOrder> findByUserOrderByCreatedAtDesc(User user);
    List<CanteenOrder> findByStatusInOrderByCreatedAtAscIdAsc(Collection<OrderStatus> statuses);
    List<CanteenOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    List<CanteenOrder> findAllByOrderByCreatedAtDesc();
    List<CanteenOrder> findTop8ByOrderByCreatedAtDesc();
    List<CanteenOrder> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime from);
    long countByCreatedAtAfter(LocalDateTime from);
    long countByStatus(OrderStatus status);
}
