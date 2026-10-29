package com.canteen.model;

public enum OrderStatus {
    PLACED("Placed"), PREPARING("Preparing"), READY("Ready for pickup"), COLLECTED("Collected"), CANCELLED("Cancelled");

    private final String label;
    OrderStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    public OrderStatus next() {
        switch (this) {
            case PLACED: return PREPARING;
            case PREPARING: return READY;
            case READY: return COLLECTED;
            default: return this;
        }
    }
}
