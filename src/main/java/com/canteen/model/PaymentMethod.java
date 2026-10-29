package com.canteen.model;

public enum PaymentMethod {
    CASH("Pay cash at counter"), UPI("Pay by UPI at counter");
    private final String label;
    PaymentMethod(String label) { this.label = label; }
    public String getLabel() { return label; }
}
