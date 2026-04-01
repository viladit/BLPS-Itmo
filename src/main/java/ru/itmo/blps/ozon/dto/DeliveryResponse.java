package ru.itmo.blps.ozon.dto;

import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryResponse {

    private Long id;
    private String carrierName;
    private String trackingNumber;
    private LocalDateTime handedAt;
    private LocalDateTime deliveredAt;
}
