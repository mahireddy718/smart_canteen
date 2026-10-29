package com.canteen.controller;

import com.canteen.model.*;
import com.canteen.repository.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserRepository userRepo;
    private final FoodItemRepository foodRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository itemRepo;

    public AdminController(UserRepository userRepo, FoodItemRepository foodRepo, OrderRepository orderRepo, OrderItemRepository itemRepo) {
        this.userRepo = userRepo; this.foodRepo = foodRepo; this.orderRepo = orderRepo; this.itemRepo = itemRepo;
    }

    private static BigDecimal revenue(List<CanteenOrder> list) {
        return list.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(CanteenOrder::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ---------- Dashboard ----------
    @GetMapping
    public String dashboard(Model m) {
        LocalDateTime start = LocalDate.now().atStartOfDay();
        List<CanteenOrder> today = orderRepo.findByCreatedAtAfterOrderByCreatedAtDesc(start);
        m.addAttribute("todayOrders", today.size());
        m.addAttribute("todayRevenue", revenue(today));
        m.addAttribute("queueSize", orderRepo.countByStatus(OrderStatus.PLACED) + orderRepo.countByStatus(OrderStatus.PREPARING));
        m.addAttribute("readyCount", orderRepo.countByStatus(OrderStatus.READY));
        m.addAttribute("userCount", userRepo.count());
        m.addAttribute("foodCount", foodRepo.count());
        m.addAttribute("lowStock", foodRepo.findByStockLessThanEqualOrderByStockAsc(5));
        m.addAttribute("topSelling", itemRepo.topSelling(OrderStatus.CANCELLED, PageRequest.of(0, 5)));
        m.addAttribute("recent", orderRepo.findTop8ByOrderByCreatedAtDesc());
        return "admin/dashboard";
    }

    // ---------- Food management ----------
    @GetMapping("/foods")
    public String foods(Model m) {
        m.addAttribute("foods", foodRepo.findAllByOrderByCategoryAscNameAsc());
        return "admin/foods";
    }

    @GetMapping("/foods/new")
    public String newFood(Model m) {
        m.addAttribute("food", new FoodItem());
        m.addAttribute("categories", categories());
        return "admin/food-form";
    }

    @GetMapping("/foods/{id}/edit")
    public String editFood(@PathVariable Long id, Model m) {
        m.addAttribute("food", foodRepo.findById(id).orElseThrow());
        m.addAttribute("categories", categories());
        return "admin/food-form";
    }

    @PostMapping("/foods/save")
    public String saveFood(@ModelAttribute("food") FoodItem form, Model m, RedirectAttributes ra) {
        if (form.getName() == null || form.getName().isBlank() || form.getPrice() == null
                || form.getPrice().compareTo(BigDecimal.ZERO) <= 0 || form.getStock() < 0 || form.getPrepMinutes() < 1) {
            m.addAttribute("error", "Enter a name, a price above 0, stock of 0 or more and prep time of at least 1 minute.");
            m.addAttribute("categories", categories());
            return "admin/food-form";
        }
        FoodItem f = form.getId() == null ? new FoodItem() : foodRepo.findById(form.getId()).orElse(new FoodItem());
        f.setName(form.getName().trim());
        f.setDescription(form.getDescription());
        f.setPrice(form.getPrice());
        f.setCategory(form.getCategory() == null || form.getCategory().isBlank() ? "Snacks" : form.getCategory().trim());
        f.setEmoji(form.getEmoji() == null || form.getEmoji().isBlank() ? "🍽️" : form.getEmoji().trim());
        f.setStock(form.getStock());
        f.setPrepMinutes(form.getPrepMinutes());
        f.setAvailable(form.isAvailable() && form.getStock() > 0);
        foodRepo.save(f);
        ra.addFlashAttribute("success", "Saved " + f.getName() + ".");
        return "redirect:/admin/foods";
    }

    @PostMapping("/foods/{id}/toggle")
    public String toggleFood(@PathVariable Long id, RedirectAttributes ra) {
        FoodItem f = foodRepo.findById(id).orElseThrow();
        f.setAvailable(!f.isAvailable());
        foodRepo.save(f);
        ra.addFlashAttribute("success", f.getName() + (f.isAvailable() ? " is now on the menu." : " is hidden from ordering."));
        return "redirect:/admin/foods";
    }

    @PostMapping("/foods/{id}/restock")
    public String restock(@PathVariable Long id, @RequestParam int qty, RedirectAttributes ra) {
        FoodItem f = foodRepo.findById(id).orElseThrow();
        if (qty > 0) {
            f.setStock(f.getStock() + qty);
            f.setAvailable(true);
            foodRepo.save(f);
            ra.addFlashAttribute("success", "Added " + qty + " to " + f.getName() + ".");
        }
        return "redirect:/admin/foods";
    }

    @PostMapping("/foods/{id}/delete")
    public String deleteFood(@PathVariable Long id, RedirectAttributes ra) {
        FoodItem f = foodRepo.findById(id).orElseThrow();
        try {
            foodRepo.delete(f);
            foodRepo.flush();
            ra.addFlashAttribute("success", "Deleted " + f.getName() + ".");
        } catch (Exception e) {
            f.setAvailable(false);
            foodRepo.save(f);
            ra.addFlashAttribute("error", f.getName() + " has past orders, so it was hidden instead of deleted.");
        }
        return "redirect:/admin/foods";
    }

    private List<String> categories() {
        return foodRepo.findAll().stream().map(FoodItem::getCategory).distinct().sorted().collect(Collectors.toList());
    }

    // ---------- Users ----------
    @GetMapping("/users")
    public String users(Model m) {
        m.addAttribute("users", userRepo.findAll());
        m.addAttribute("roles", Role.values());
        return "admin/users";
    }

    @PostMapping("/users/{id}/role")
    public String changeRole(@PathVariable Long id, @RequestParam Role role, Principal principal, RedirectAttributes ra) {
        User u = userRepo.findById(id).orElseThrow();
        if (u.getEmail().equalsIgnoreCase(principal.getName())) {
            ra.addFlashAttribute("error", "You cannot change your own role.");
        } else {
            u.setRole(role);
            userRepo.save(u);
            ra.addFlashAttribute("success", u.getName() + " is now " + role.name().toLowerCase() + ".");
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User u = userRepo.findById(id).orElseThrow();
        if (u.getEmail().equalsIgnoreCase(principal.getName())) {
            ra.addFlashAttribute("error", "You cannot deactivate your own account.");
        } else {
            u.setActive(!u.isActive());
            userRepo.save(u);
            ra.addFlashAttribute("success", u.getName() + (u.isActive() ? " can log in again." : " has been deactivated."));
        }
        return "redirect:/admin/users";
    }

    // ---------- Orders ----------
    @GetMapping("/orders")
    public String orders(@RequestParam(required = false) OrderStatus status, Model m) {
        m.addAttribute("orders", status == null ? orderRepo.findAllByOrderByCreatedAtDesc() : orderRepo.findByStatusOrderByCreatedAtDesc(status));
        m.addAttribute("statuses", OrderStatus.values());
        m.addAttribute("status", status);
        return "admin/orders";
    }

    @GetMapping("/orders/export")
    public void exportCsv(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=canteen-orders.csv");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        StringBuilder sb = new StringBuilder("Order ID,Token,Date,Customer,Items,Total,Payment,Status\n");
        for (CanteenOrder o : orderRepo.findAllByOrderByCreatedAtDesc()) {
            String items = o.getItems().stream().map(i -> i.getQuantity() + "x " + i.getFood().getName()).collect(Collectors.joining("; "));
            sb.append(o.getId()).append(',')
              .append(o.getToken() == null ? "" : o.getToken().getCode()).append(',')
              .append(o.getCreatedAt().format(fmt)).append(',')
              .append(csv(o.getUser().getName())).append(',')
              .append(csv(items)).append(',')
              .append(o.getTotal()).append(',')
              .append(o.getPaymentMethod()).append(',')
              .append(o.getStatus()).append('\n');
        }
        response.getWriter().write(sb.toString());
    }

    private static String csv(String s) {
        return "\"" + (s == null ? "" : s.replace("\"", "\"\"")) + "\"";
    }

    // ---------- Reports ----------
    @GetMapping("/reports")
    public String reports(Model m) {
        LocalDate today = LocalDate.now();
        List<CanteenOrder> week = orderRepo.findByCreatedAtAfterOrderByCreatedAtDesc(today.minusDays(6).atStartOfDay());

        List<DayStat> days = new ArrayList<>();
        BigDecimal maxRev = BigDecimal.ONE;
        Map<LocalDate, List<CanteenOrder>> byDay = week.stream().collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));
        List<BigDecimal> revs = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            BigDecimal r = revenue(byDay.getOrDefault(today.minusDays(i), List.of()));
            revs.add(r);
            if (r.compareTo(maxRev) > 0) maxRev = r;
        }
        for (int i = 6, k = 0; i >= 0; i--, k++) {
            LocalDate d = today.minusDays(i);
            List<CanteenOrder> list = byDay.getOrDefault(d, List.of());
            int pct = revs.get(k).multiply(BigDecimal.valueOf(100)).divide(maxRev, 0, java.math.RoundingMode.HALF_UP).intValue();
            days.add(new DayStat(d.format(DateTimeFormatter.ofPattern("EEE d MMM")), list.size(), revs.get(k), pct));
        }

        long[] hourly = new long[24];
        for (CanteenOrder o : week) hourly[o.getCreatedAt().getHour()]++;
        long maxHour = Arrays.stream(hourly).max().orElse(1);
        List<DayStat> hours = new ArrayList<>();
        for (int h = 7; h <= 21; h++) {
            int pct = maxHour == 0 ? 0 : (int) (hourly[h] * 100 / maxHour);
            hours.add(new DayStat(String.format("%02d:00", h), hourly[h], BigDecimal.ZERO, pct));
        }

        m.addAttribute("days", days);
        m.addAttribute("hours", hours);
        m.addAttribute("weekRevenue", revenue(week));
        m.addAttribute("weekOrders", week.size());
        m.addAttribute("topSelling", itemRepo.topSelling(OrderStatus.CANCELLED, PageRequest.of(0, 10)));
        return "admin/reports";
    }
}
