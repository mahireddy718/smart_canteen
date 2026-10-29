package com.canteen.controller;

import com.canteen.model.User;
import com.canteen.repository.UserRepository;
import com.canteen.service.CartService;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Adds the logged-in user and cart size to every page. */
@ControllerAdvice
public class GlobalModelAdvice {
    private final UserRepository users;
    private final CartService cart;

    public GlobalModelAdvice(UserRepository users, CartService cart) {
        this.users = users; this.cart = cart;
    }

    private boolean loggedIn(Authentication a) {
        return a != null && a.isAuthenticated() && !(a instanceof AnonymousAuthenticationToken);
    }

    @ModelAttribute("currentUser")
    public User currentUser(Authentication auth) {
        return loggedIn(auth) ? users.findByEmailIgnoreCase(auth.getName()).orElse(null) : null;
    }

    @ModelAttribute("cartCount")
    public int cartCount(Authentication auth) {
        return loggedIn(auth) ? cart.count() : 0;
    }
}
