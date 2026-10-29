package com.canteen.service;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shopping cart kept in the user's HTTP session. */
@Component
@SessionScope
public class CartService {
    private final Map<Long, Integer> items = new LinkedHashMap<>();

    public void add(Long foodId, int qty) { items.merge(foodId, qty, Integer::sum); }
    public void set(Long foodId, int qty) { if (qty <= 0) items.remove(foodId); else items.put(foodId, qty); }
    public void remove(Long foodId) { items.remove(foodId); }
    public void clear() { items.clear(); }
    public Map<Long, Integer> getItems() { return items; }
    public int count() { return items.values().stream().mapToInt(Integer::intValue).sum(); }
}
