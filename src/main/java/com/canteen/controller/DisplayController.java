package com.canteen.controller;

import com.canteen.model.CanteenOrder;
import com.canteen.model.OrderStatus;
import com.canteen.repository.OrderRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.stream.Collectors;

/** Public token board for the canteen TV screen. */
@Controller
public class DisplayController {
    private final OrderRepository orders;

    public DisplayController(OrderRepository orders) { this.orders = orders; }

    @GetMapping("/display")
    public String display() { return "display"; }

    @GetMapping("/api/queue")
    @ResponseBody
    public Map<String, Object> queue() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("preparing", codes(OrderStatus.PREPARING));
        m.put("ready", codes(OrderStatus.READY));
        m.put("waiting", codes(OrderStatus.PLACED));
        return m;
    }

    private List<String> codes(OrderStatus s) {
        return orders.findByStatusInOrderByCreatedAtAscIdAsc(List.of(s)).stream()
                .map(o -> o.getToken().getCode()).collect(Collectors.toList());
    }
}
