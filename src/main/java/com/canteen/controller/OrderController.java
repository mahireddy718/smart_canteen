package com.canteen.controller;

import com.canteen.model.CanteenOrder;
import com.canteen.model.OrderStatus;
import com.canteen.model.Role;
import com.canteen.model.User;
import com.canteen.repository.OrderRepository;
import com.canteen.repository.UserRepository;
import com.canteen.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;

@Controller
public class OrderController {
    private final OrderService orders;
    private final OrderRepository orderRepo;
    private final UserRepository users;

    public OrderController(OrderService orders, OrderRepository orderRepo, UserRepository users) {
        this.orders = orders; this.orderRepo = orderRepo; this.users = users;
    }

    private User me(Principal p) { return users.findByEmailIgnoreCase(p.getName()).orElseThrow(); }

    private CanteenOrder load(Long id, User u) {
        CanteenOrder o = orderRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (u.getRole() == Role.STUDENT && !o.getUser().getId().equals(u.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return o;
    }

    @GetMapping("/orders")
    public String myOrders(Principal principal, Model model) {
        model.addAttribute("orders", orderRepo.findByUserOrderByCreatedAtDesc(me(principal)));
        return "orders";
    }

    @GetMapping("/orders/{id}")
    public String detail(@PathVariable Long id, Principal principal, Model model) {
        CanteenOrder o = load(id, me(principal));
        model.addAttribute("order", o);
        model.addAttribute("position", orders.positionOf(o));
        model.addAttribute("eta", orders.estimateMinutes(o));
        return "order-detail";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            orders.cancel(id, me(principal));
            ra.addFlashAttribute("success", "Order cancelled.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    /** Polled by the order page to update live status. */
    @GetMapping("/api/orders/{id}/status")
    @ResponseBody
    public Map<String, Object> status(@PathVariable Long id, Principal principal) {
        CanteenOrder o = load(id, me(principal));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", o.getStatus().name());
        m.put("label", o.getStatus().getLabel());
        m.put("position", orders.positionOf(o));
        m.put("eta", orders.estimateMinutes(o));
        return m;
    }
}
