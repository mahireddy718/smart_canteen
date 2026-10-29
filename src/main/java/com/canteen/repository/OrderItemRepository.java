package com.canteen.repository;

import com.canteen.model.OrderItem;
import com.canteen.model.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("select f.name, sum(oi.quantity) from OrderItem oi join oi.food f join oi.order o " +
           "where o.status <> :cancelled group by f.name order by sum(oi.quantity) desc")
    List<Object[]> topSelling(@Param("cancelled") OrderStatus cancelled, Pageable pageable);
}
