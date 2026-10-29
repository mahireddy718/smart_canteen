package com.canteen.service;

import com.canteen.model.*;
import com.canteen.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final FoodItemRepository foodRepo;
    private final TokenRepository tokenRepo;

    @Value("${canteen.kitchen-capacity:2}")
    private int capacity;

    public OrderService(OrderRepository orderRepo, FoodItemRepository foodRepo, TokenRepository tokenRepo) {
        this.orderRepo = orderRepo;
        this.foodRepo = foodRepo;
        this.tokenRepo = tokenRepo;
    }

    /** Creates an order, reduces stock and generates today's next token number. */
    @Transactional
    public synchronized CanteenOrder placeOrder(User user, Map<Long, Integer> cart, String note, PaymentMethod method) {
        if (cart == null || cart.isEmpty()) throw new IllegalStateException("Your cart is empty.");
        CanteenOrder order = new CanteenOrder();
        order.setUser(user);
        order.setPaymentMethod(method == null ? PaymentMethod.CASH : method);
        order.setNote(note == null || note.isBlank() ? null : note.trim());
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> e : cart.entrySet()) {
            FoodItem f = foodRepo.findById(e.getKey())
                    .orElseThrow(() -> new IllegalStateException("An item in your cart no longer exists."));
            int qty = e.getValue();
            if (!f.isAvailable() || f.getStock() < qty) {
                throw new IllegalStateException(f.getName() + " is not available in the quantity you chose.");
            }
            f.setStock(f.getStock() - qty);
            if (f.getStock() == 0) f.setAvailable(false);
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setFood(f);
            oi.setQuantity(qty);
            oi.setUnitPrice(f.getPrice());
            order.getItems().add(oi);
            total = total.add(f.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        order.setTotal(total);

        LocalDate today = LocalDate.now();
        Integer max = tokenRepo.maxTokenNumber(today);
        Token token = new Token();
        token.setOrder(order);
        token.setTokenDate(today);
        token.setTokenNumber((max == null ? 0 : max) + 1);
        order.setToken(token);
        return orderRepo.save(order);
    }

    public CanteenOrder get(Long id) {
        return orderRepo.findById(id).orElseThrow(() -> new IllegalStateException("Order not found."));
    }

    @Transactional
    public void cancel(Long id, User actor) {
        CanteenOrder o = get(id);
        boolean staff = actor.getRole() != Role.STUDENT;
        if (!staff && !o.getUser().getId().equals(actor.getId())) {
            throw new IllegalStateException("This is not your order.");
        }
        boolean cancellable = o.getStatus() == OrderStatus.PLACED || (staff && o.getStatus() == OrderStatus.PREPARING);
        if (!cancellable) throw new IllegalStateException("This order can no longer be cancelled.");
        for (OrderItem oi : o.getItems()) {
            FoodItem f = oi.getFood();
            f.setStock(f.getStock() + oi.getQuantity());
            if (f.getStock() > 0) f.setAvailable(true);
        }
        o.setStatus(OrderStatus.CANCELLED);
        o.setUpdatedAt(LocalDateTime.now());
    }

    /** Moves an order one step forward: PLACED -> PREPARING -> READY -> COLLECTED. */
    @Transactional
    public CanteenOrder advance(Long id) {
        CanteenOrder o = get(id);
        if (o.getStatus() == OrderStatus.CANCELLED || o.getStatus() == OrderStatus.COLLECTED) {
            throw new IllegalStateException("This order is already closed.");
        }
        o.setStatus(o.getStatus().next());
        o.setUpdatedAt(LocalDateTime.now());
        return o;
    }

    /** FIFO: starts the oldest waiting order. */
    @Transactional
    public CanteenOrder callNext() {
        return activeQueue().stream()
                .filter(o -> o.getStatus() == OrderStatus.PLACED)
                .findFirst()
                .map(o -> advance(o.getId()))
                .orElseThrow(() -> new IllegalStateException("No orders are waiting."));
    }

    /** Counter check: hand over food when the customer shows a token. */
    @Transactional
    public CanteenOrder collectByToken(String code) {
        String digits = code == null ? "" : code.replaceAll("\\D", "");
        if (digits.isEmpty() || digits.length() > 6) throw new IllegalStateException("Enter a valid token number, e.g. T-012.");
        Token t = tokenRepo.findByTokenDateAndTokenNumber(LocalDate.now(), Integer.parseInt(digits))
                .orElseThrow(() -> new IllegalStateException("No token " + code.trim() + " was issued today."));
        CanteenOrder o = t.getOrder();
        if (o.getStatus() != OrderStatus.READY) {
            throw new IllegalStateException("Token " + t.getCode() + " is " + o.getStatus().getLabel().toLowerCase() + ", not ready for pickup.");
        }
        o.setStatus(OrderStatus.COLLECTED);
        o.setUpdatedAt(LocalDateTime.now());
        return o;
    }

    // ---- FIFO queue helpers ----

    /** Orders waiting or being cooked, oldest first. */
    public List<CanteenOrder> activeQueue() {
        return orderRepo.findByStatusInOrderByCreatedAtAscIdAsc(List.of(OrderStatus.PLACED, OrderStatus.PREPARING));
    }

    public List<CanteenOrder> readyOrders() {
        return orderRepo.findByStatusInOrderByCreatedAtAscIdAsc(List.of(OrderStatus.READY));
    }

    /** Position in the queue (1 = next up). 0 when the order is not queued. */
    public int positionOf(CanteenOrder order) {
        List<CanteenOrder> q = activeQueue();
        for (int i = 0; i < q.size(); i++) {
            if (q.get(i).getId().equals(order.getId())) return i + 1;
        }
        return 0;
    }

    /** Estimated minutes until ready, based on prep times of orders ahead and the kitchen capacity. */
    public int estimateMinutes(CanteenOrder order) {
        List<CanteenOrder> q = activeQueue();
        int sum = 0;
        for (CanteenOrder o : q) {
            sum += o.getPrepMinutes();
            if (o.getId().equals(order.getId())) {
                return Math.max(1, (int) Math.ceil(sum / (double) Math.max(1, capacity)));
            }
        }
        return 0;
    }
}
