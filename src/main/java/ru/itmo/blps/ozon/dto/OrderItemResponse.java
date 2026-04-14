package ru.itmo.blps.ozon.dto;

import java.math.BigDecimal;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private String sku;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
