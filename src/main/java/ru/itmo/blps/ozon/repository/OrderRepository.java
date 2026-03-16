package ru.itmo.blps.ozon.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.blps.ozon.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = {"items", "delivery"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"items", "delivery"})
    List<Order> findAllByOrderByIdAsc();
}
