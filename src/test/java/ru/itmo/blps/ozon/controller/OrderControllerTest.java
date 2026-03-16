package ru.itmo.blps.ozon.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.itmo.blps.ozon.dto.DeliveryResponse;
import ru.itmo.blps.ozon.dto.OrderItemResponse;
import ru.itmo.blps.ozon.dto.OrderResponse;
import ru.itmo.blps.ozon.entity.OrderStatus;
import ru.itmo.blps.ozon.exception.GlobalExceptionHandler;
import ru.itmo.blps.ozon.service.OrderService;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void createOrderShouldReturnCreatedOrder() throws Exception {
        when(orderService.createOrder(any())).thenReturn(orderResponse(OrderStatus.ORDER_CONTENTS_LOADED, null));

        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Ivan Petrov",
                                  "deliveryAddress": "Saint Petersburg, Nevsky 1",
                                  "items": [
                                    {
                                      "sku": "SKU-1",
                                      "productName": "Phone",
                                      "quantity": 2,
                                      "unitPrice": 499.99
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/1"))
                .andExpect(jsonPath("$.status").value("ORDER_CONTENTS_LOADED"))
                .andExpect(jsonPath("$.items[0].sku").value("SKU-1"));
    }

    @Test
    void getOrderByIdShouldReturnOrder() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(orderResponse(OrderStatus.ORDER_PACKED, null));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ORDER_PACKED"));
    }

    @Test
    void createOrderShouldReturnValidationErrorForEmptyItems() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName": "Ivan Petrov",
                                  "deliveryAddress": "Saint Petersburg, Nevsky 1",
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.items").value("Order must contain at least one item"));
    }

    @Test
    void handoffShouldReturnUpdatedDeliveryInfo() throws Exception {
        DeliveryResponse deliveryResponse = new DeliveryResponse(
                3L,
                "OZON Delivery",
                "TRACK-001",
                LocalDateTime.of(2026, 3, 16, 13, 15, 30),
                null
        );
        when(orderService.handToDelivery(eq(1L), any())).thenReturn(orderResponse(OrderStatus.HANDED_TO_DELIVERY, deliveryResponse));

        mockMvc.perform(post("/api/orders/1/handoff")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "carrierName": "OZON Delivery",
                                  "trackingNumber": "TRACK-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HANDED_TO_DELIVERY"))
                .andExpect(jsonPath("$.delivery.trackingNumber").value("TRACK-001"));
    }

    private OrderResponse orderResponse(OrderStatus status, DeliveryResponse deliveryResponse) {
        return new OrderResponse(
                1L,
                "Ivan Petrov",
                "Saint Petersburg, Nevsky 1",
                status,
                true,
                null,
                LocalDateTime.of(2026, 3, 16, 13, 15, 30),
                LocalDateTime.of(2026, 3, 16, 13, 15, 30),
                List.of(new OrderItemResponse(10L, "SKU-1", "Phone", 2, BigDecimal.valueOf(499.99))),
                deliveryResponse
        );
    }
}
