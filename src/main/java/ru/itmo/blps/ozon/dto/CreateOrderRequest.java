package ru.itmo.blps.ozon.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CreateOrderRequest {

    @NotBlank(message = "Customer name must not be blank")
    private String customerName;

    @NotBlank(message = "Delivery address must not be blank")
    private String deliveryAddress;

    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    private List<OrderItemRequest> items;
}
