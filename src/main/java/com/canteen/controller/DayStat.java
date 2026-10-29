package com.canteen.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class DayStat {
    private String label;
    private long orders;
    private BigDecimal revenue;
    private int percent;
}
