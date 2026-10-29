package com.canteen.controller;

import com.canteen.model.CanteenOrder;
import com.canteen.model.User;
import com.canteen.repository.UserRepository;
import com.canteen.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

/** Kitchen screen: works through the FIFO queue. */
@Controller
@RequestMapping("/kitchen")
public class KitchenController {
    private final OrderService orders;
    private final UserRepository users;

    public KitchenController(OrderService orders, UserRepository users) {
        this.orders = orders; this.users = users;
    }

    @GetMapping
    public String queue(Model model) {
        model.addAttribute("queue", orders.activeQueue());
        model.addAttribute("ready", orders.readyOrders());
        return "kitchen";
    }

    @PostMapping("/{id}/advance")
    public String advance(@PathVariable Long id, RedirectAttributes ra) {
        try {
            CanteenOrder o = orders.advance(id);
            ra.addFlashAttribute("success", o.getToken().getCode() + " is now: " + o.getStatus().getLabel() + ".");
        } catch (IllegalStateException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/kitchen";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            User u = users.findByEmailIgnoreCase(principal.getName()).orElseThrow();
            orders.cancel(id, u);
            ra.addFlashAttribute("success", "Order cancelled and stock restored.");
        } catch (IllegalStateException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/kitchen";
    }

    @PostMapping("/next")
    public String next(RedirectAttributes ra) {
        try {
            CanteenOrder o = orders.callNext();
            ra.addFlashAttribute("success", "Started " + o.getToken().getCode() + " (oldest waiting order).");
        } catch (IllegalStateException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/kitchen";
    }

    @PostMapping("/verify")
    public String verify(@RequestParam String code, RedirectAttributes ra) {
        try {
            CanteenOrder o = orders.collectByToken(code);
            ra.addFlashAttribute("success", o.getToken().getCode() + " handed over to " + o.getUser().getName() + ".");
        } catch (IllegalStateException e) { ra.addFlashAttribute("error", e.getMessage()); }
        return "redirect:/kitchen";
    }
}
