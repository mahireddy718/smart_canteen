package com.canteen.controller;

import com.canteen.model.FoodItem;
import com.canteen.model.Role;
import com.canteen.model.User;
import com.canteen.repository.FoodItemRepository;
import com.canteen.repository.UserRepository;
import com.canteen.service.OrderService;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthController {
    private final UserRepository users;
    private final FoodItemRepository foods;
    private final PasswordEncoder encoder;
    private final OrderService orders;

    public AuthController(UserRepository users, FoodItemRepository foods, PasswordEncoder encoder, OrderService orders) {
        this.users = users; this.foods = foods; this.encoder = encoder; this.orders = orders;
    }

    @GetMapping("/")
    public String index(Authentication auth, Model model) {
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) return "redirect:/home";
        List<FoodItem> featured = foods.findAllByOrderByCategoryAscNameAsc().stream()
                .filter(FoodItem::isAvailable).limit(6).collect(Collectors.toList());
        model.addAttribute("featured", featured);
        model.addAttribute("waiting", orders.activeQueue().size());
        return "index";
    }

    @GetMapping("/home")
    public String home(Authentication auth) {
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return "redirect:/admin";
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_STAFF"))) return "redirect:/kitchen";
        return "redirect:/menu";
    }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/register")
    public String registerForm() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String name, @RequestParam String email, @RequestParam String password,
                           @RequestParam(required = false) String phone, Model model) {
        model.addAttribute("name", name);
        model.addAttribute("email", email);
        model.addAttribute("phone", phone);
        String em = email.trim().toLowerCase();
        if (name.isBlank()) { model.addAttribute("error", "Enter your name."); return "register"; }
        if (!em.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) { model.addAttribute("error", "Enter a valid email address."); return "register"; }
        if (password.length() < 6) { model.addAttribute("error", "Password must be at least 6 characters."); return "register"; }
        if (users.existsByEmailIgnoreCase(em)) { model.addAttribute("error", "An account with this email already exists. Log in instead."); return "register"; }
        User u = new User();
        u.setName(name.trim());
        u.setEmail(em);
        u.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        u.setPassword(encoder.encode(password));
        u.setRole(Role.STUDENT);
        users.save(u);
        return "redirect:/login?registered";
    }

    @GetMapping("/profile")
    public String profile() { return "profile"; }

    @PostMapping("/profile")
    public String updateProfile(Principal principal, @RequestParam String name, @RequestParam(required = false) String phone,
                                @RequestParam(required = false) String newPassword, Model model) {
        User u = users.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        if (name.isBlank()) { model.addAttribute("error", "Name cannot be empty."); return "profile"; }
        if (newPassword != null && !newPassword.isBlank() && newPassword.length() < 6) {
            model.addAttribute("error", "New password must be at least 6 characters."); return "profile";
        }
        u.setName(name.trim());
        u.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        if (newPassword != null && !newPassword.isBlank()) u.setPassword(encoder.encode(newPassword));
        users.save(u);
        model.addAttribute("currentUser", u);
        model.addAttribute("success", "Profile saved.");
        return "profile";
    }
}
