package ru.itmo.blps.ozon.security.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.blps.ozon.security.entity.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @EntityGraph(attributePaths = {"roles", "roles.privileges"})
    Optional<UserAccount> findByUsername(String username);
}
