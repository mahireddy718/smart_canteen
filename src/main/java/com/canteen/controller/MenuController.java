package com.canteen.controller;

import com.canteen.model.*;
import com.canteen.repository.FoodItemRepository;
import com.canteen.repository.UserRepository;
import com.canteen.service.CartService;
import com.canteen.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class MenuController {
    private final FoodItemRepository foods;
    private final UserRepository users;
    private final CartService cart;
    private final OrderService orders;

    public MenuController(FoodItemRepository foods, UserRepository users, CartService cart, OrderService orders) {
        this.foods = foods; this.users = users; this.cart = cart; this.orders = orders;
    }

    @GetMapping("/menu")
    public String menu(@RequestParam(required = false) String category, @RequestParam(required = false) String q, Model model) {
        List<FoodItem> all = foods.findAllByOrderByCategoryAscNameAsc();
        List<String> categories = all.stream().map(FoodItem::getCategory).distinct().collect(Collectors.toList());
        String needle = q == null ? "" : q.trim().toLowerCase();
        List<FoodItem> shown = all.stream()
                .filter(f -> category == null || category.isBlank() || f.getCategory().equalsIgnoreCase(category))
                .filter(f -> needle.isEmpty() || f.getName().toLowerCase().contains(needle)
                        || (f.getDescription() != null && f.getDescription().toLowerCase().contains(needle)))
                .collect(Collectors.toList());
        model.addAttribute("foods", shown);
        model.addAttribute("categories", categories);
        model.addAttribute("category", category);
        model.addAttribute("q", q);
        model.addAttribute("waiting", orders.activeQueue().size());
        return "menu";
    }

    @PostMapping("/cart/add")
    public String add(@RequestParam Long foodId, @RequestParam(defaultValue = "1") int qty, RedirectAttributes ra) {
        FoodItem f = foods.findById(foodId).orElse(null);
        if (f == null || !f.isAvailable()) {
            ra.addFlashAttribute("error", "That item is sold out.");
            return "redirect:/menu";
        }
        int current = cart.getItems().getOrDefault(foodId, 0);
        int wanted = Math.max(1, Math.min(qty, 10));
        if (current + wanted > f.getStock()) {
            ra.addFlashAttribute("error", "Only " + f.getStock() + " of " + f.getName() + " left.");
            return "redirect:/menu";
        }
        cart.add(foodId, wanted);
        ra.addFlashAttribute("success", f.getName() + " added to your cart.");
        return "redirect:/menu";
    }

    @GetMapping("/cart")
    public String viewCart(Model model) {
        List<CartLine> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : new ArrayList<>(cart.getItems().entrySet())) {
            Optional<FoodItem> f = foods.findById(e.getKey());
            if (f.isEmpty()) { cart.remove(e.getKey()); continue; }
            BigDecimal sub = f.get().getPrice().multiply(BigDecimal.valueOf(e.getValue()));
            lines.add(new CartLine(f.get(), e.getValue(), sub));
            total = total.add(sub);
        }
        model.addAttribute("lines", lines);
        model.addAttribute("total", total);
        model.addAttribute("methods", PaymentMethod.values());
        return "cart";
    }

    @PostMapping("/cart/update")
    public String update(@RequestParam Long foodId, @RequestParam int qty, RedirectAttributes ra) {
        FoodItem f = foods.findById(foodId).orElse(null);
        if (f != null && qty > f.getStock()) {
            ra.addFlashAttribute("error", "Only " + f.getStock() + " of " + f.getName() + " left.");
            return "redirect:/cart";
        }
        cart.set(foodId, Math.min(qty, 10));
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String remove(@RequestParam Long foodId) {
        cart.remove(foodId);
        return "redirect:/cart";
    }

    @PostMapping("/checkout")
    public String checkout(Principal principal, @RequestParam(defaultValue = "CASH") PaymentMethod paymentMethod,
                           @RequestParam(required = false) String note, RedirectAttributes ra) {
        User user = users.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        try {
            CanteenOrder order = orders.placeOrder(user, new LinkedHashMap<>(cart.getItems()), note, paymentMethod);
            cart.clear();
            ra.addFlashAttribute("success", "Order placed! Your token is " + order.getToken().getCode() + ".");
            return "redirect:/orders/" + order.getId();
        } catch (IllegalStateException ex) {
            ra.addFlashAttribute("error", ex.getMessage());
            return "redirect:/cart";
        }
    }
}
