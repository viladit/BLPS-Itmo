package ru.itmo.blps.ozon.security.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.blps.ozon.security.PrivilegeName;
import ru.itmo.blps.ozon.security.entity.Privilege;

public interface PrivilegeRepository extends JpaRepository<Privilege, Long> {

    Optional<Privilege> findByName(PrivilegeName name);
}
