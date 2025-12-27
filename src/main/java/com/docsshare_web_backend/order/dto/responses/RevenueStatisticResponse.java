package com.docsshare_web_backend.order.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RevenueStatisticResponse {
    private String label;   // day / month / year
    private BigDecimal revenue;
}
