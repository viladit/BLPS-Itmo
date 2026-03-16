package ru.itmo.blps.ozon.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String carrierName;

    @Column(nullable = false, unique = true)
    private String trackingNumber;

    @Column(nullable = false)
    private LocalDateTime handedAt;

    private LocalDateTime deliveredAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;
}
