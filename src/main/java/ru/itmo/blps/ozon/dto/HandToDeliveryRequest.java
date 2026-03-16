package ru.itmo.blps.ozon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HandToDeliveryRequest {

    @NotBlank(message = "Carrier name must not be blank")
    private String carrierName;

    @NotBlank(message = "Tracking number must not be blank")
    private String trackingNumber;
}
