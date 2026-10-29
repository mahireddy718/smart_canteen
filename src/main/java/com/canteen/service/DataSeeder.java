package com.canteen.service;

import com.canteen.model.*;
import com.canteen.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.List;

/** Creates demo accounts and a starter menu on first run. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final UserRepository users;
    private final FoodItemRepository foods;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, FoodItemRepository foods, PasswordEncoder encoder) {
        this.users = users; this.foods = foods; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() == 0) {
            users.save(make("Canteen Admin", "admin@canteen.com", "admin123", Role.ADMIN));
            users.save(make("Kitchen Staff", "staff@canteen.com", "staff123", Role.STAFF));
            users.save(make("Demo Student", "student@canteen.com", "student123", Role.STUDENT));
        }
        if (foods.count() == 0) {
            foods.saveAll(List.of(
                new FoodItem("Idli Sambar (3 pcs)", "Soft steamed idlis with sambar and chutney", "30", "Breakfast", "🍚", 60, 4),
                new FoodItem("Masala Dosa", "Crisp dosa with spiced potato filling", "50", "Breakfast", "🥞", 40, 7),
                new FoodItem("Poha", "Flattened rice with peanuts and lemon", "25", "Breakfast", "🥣", 40, 4),
                new FoodItem("Veg Thali", "Rice, 2 rotis, dal, sabzi, salad", "80", "Meals", "🍛", 50, 8),
                new FoodItem("Rajma Chawal", "Kidney bean curry with steamed rice", "70", "Meals", "🍲", 40, 8),
                new FoodItem("Chole Bhature", "Spicy chickpeas with 2 fluffy bhature", "75", "Meals", "🫓", 35, 10),
                new FoodItem("Veg Burger", "Crispy patty, lettuce, cheese", "45", "Snacks", "🍔", 40, 6),
                new FoodItem("Samosa (2 pcs)", "Golden samosas with green chutney", "20", "Snacks", "🥟", 80, 3),
                new FoodItem("Veg Sandwich", "Grilled sandwich with veggies", "35", "Snacks", "🥪", 40, 5),
                new FoodItem("Masala Chai", "Hot ginger masala tea", "10", "Beverages", "☕", 150, 2),
                new FoodItem("Cold Coffee", "Chilled creamy coffee", "40", "Beverages", "🥤", 50, 3),
                new FoodItem("Fresh Lime Soda", "Sweet or salted", "25", "Beverages", "🍋", 60, 2),
                new FoodItem("Gulab Jamun (2 pcs)", "Warm syrupy dessert", "30", "Desserts", "🍮", 50, 2),
                new FoodItem("Ice Cream Cup", "Vanilla, chocolate or strawberry", "30", "Desserts", "🍨", 50, 2)
            ));
        }
    }

    private User make(String name, String email, String pw, Role role) {
        User u = new User();
        u.setName(name); u.setEmail(email); u.setPassword(encoder.encode(pw)); u.setRole(role);
        return u;
    }
}
