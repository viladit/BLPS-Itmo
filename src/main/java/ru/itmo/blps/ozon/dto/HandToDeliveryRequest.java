package ru.itmo.blps.ozon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HandToDeliveryRequest {

    @NotBlank(message = "Carrier name must not be blank")
    private String carrierName;

    @NotBlank(message = "Tracking number must not be blank")
    private String trackingNumber;
}
