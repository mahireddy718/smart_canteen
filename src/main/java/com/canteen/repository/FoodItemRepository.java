package com.canteen.repository;

import com.canteen.model.FoodItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FoodItemRepository extends JpaRepository<FoodItem, Long> {
    List<FoodItem> findAllByOrderByCategoryAscNameAsc();
    List<FoodItem> findByStockLessThanEqualOrderByStockAsc(int stock);
}
