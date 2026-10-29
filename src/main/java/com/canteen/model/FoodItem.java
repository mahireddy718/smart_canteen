package com.canteen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_items")
@Getter @Setter @NoArgsConstructor
public class FoodItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(length = 300)
    private String description;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;
    private String category = "Snacks";
    private String emoji = "🍽️";
    private boolean available = true;
    private int stock = 50;
    private int prepMinutes = 5;
    private LocalDateTime createdAt = LocalDateTime.now();

    public FoodItem(String name, String description, String price, String category, String emoji, int stock, int prepMinutes) {
        this.name = name; this.description = description; this.price = new BigDecimal(price);
        this.category = category; this.emoji = emoji; this.stock = stock; this.prepMinutes = prepMinutes;
    }
}
