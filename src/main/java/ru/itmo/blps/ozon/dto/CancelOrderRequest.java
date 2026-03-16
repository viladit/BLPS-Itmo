package ru.itmo.blps.ozon.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CancelOrderRequest {

    @NotBlank(message = "Cancellation reason must not be blank")
    private String reason;
}
