package ru.itmo.blps.ozon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.blps.ozon.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
