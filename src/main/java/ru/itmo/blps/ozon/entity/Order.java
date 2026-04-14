package ru.itmo.blps.ozon.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private OrderStatus status;

    private Boolean stockAvailable;

    @Column(length = 255)
    private String cancellationReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Delivery delivery;

    public static Order create(String customerName, String deliveryAddress, LocalDateTime now) {
        Order order = new Order();
        order.customerName = customerName;
        order.deliveryAddress = deliveryAddress;
        order.status = OrderStatus.CREATED;
        order.stockAvailable = null;
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void setDelivery(Delivery delivery) {
        this.delivery = delivery;
        if (delivery != null) {
            delivery.setOrder(this);
        }
    }

    public void changeStatus(OrderStatus status) {
        this.status = status;
    }

    public void markStockAvailable(boolean stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public void changeCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public void touch(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
