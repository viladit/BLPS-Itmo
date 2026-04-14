package ru.itmo.blps.ozon.security.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.blps.ozon.security.RoleName;
import ru.itmo.blps.ozon.security.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = "privileges")
    Optional<Role> findByName(RoleName name);
}
