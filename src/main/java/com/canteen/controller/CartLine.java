package com.canteen.controller;

import com.canteen.model.FoodItem;
import java.math.BigDecimal;

public class CartLine {
    private FoodItem food;
    private int qty;
    private BigDecimal subtotal;

    public CartLine(FoodItem food, int qty, BigDecimal subtotal) {
        this.food = food;
        this.qty = qty;
        this.subtotal = subtotal;
    }

    public FoodItem getFood() {
        return food;
    }

    public int getQty() {
        return qty;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
