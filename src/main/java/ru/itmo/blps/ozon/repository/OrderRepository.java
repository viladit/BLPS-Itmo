package ru.itmo.blps.ozon.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.itmo.blps.ozon.entity.Order;
import ru.itmo.blps.ozon.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Override
    @EntityGraph(attributePaths = {"items", "delivery"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"items", "delivery"})
    List<Order> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"items", "delivery"})
    List<Order> findAllByStatusOrderByIdAsc(OrderStatus status);

    @Query("""
            select o
            from Order o
            where o.status = :status
              and o.createdAt < :createdBefore
              and (o.pendingReminderSentAt is null or o.pendingReminderSentAt < :remindedBefore)
            order by o.createdAt
            """)
    @EntityGraph(attributePaths = {"items", "delivery"})
    List<Order> findPendingOrdersForReminder(
            OrderStatus status,
            LocalDateTime createdBefore,
            LocalDateTime remindedBefore
    );
}
