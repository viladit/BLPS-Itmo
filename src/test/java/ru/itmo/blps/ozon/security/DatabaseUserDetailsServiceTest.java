package ru.itmo.blps.ozon.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import ru.itmo.blps.ozon.security.entity.Privilege;
import ru.itmo.blps.ozon.security.entity.Role;
import ru.itmo.blps.ozon.security.entity.UserAccount;
import ru.itmo.blps.ozon.security.repository.PrivilegeRepository;
import ru.itmo.blps.ozon.security.repository.RoleRepository;
import ru.itmo.blps.ozon.security.repository.UserAccountRepository;
import ru.itmo.blps.ozon.security.service.DatabaseUserDetailsService;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseUserDetailsServiceTest {

    @Autowired
    private DatabaseUserDetailsService userDetailsService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();
        privilegeRepository.deleteAll();
    }

    @Test
    void shouldLoadUserAuthoritiesFromDatabase() {
        Privilege read = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_READ));
        Privilege accept = privilegeRepository.save(new Privilege(PrivilegeName.ORDER_ACCEPT));

        Role manager = new Role(RoleName.ROLE_MANAGER);
        manager.setPrivileges(new LinkedHashSet<>(Set.of(read, accept)));
        Role savedRole = roleRepository.save(manager);

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername("manager");
        userAccount.setPasswordHash(passwordEncoder.encode("manager123"));
        userAccount.setEnabled(true);
        userAccount.setRoles(new LinkedHashSet<>(Set.of(savedRole)));
        userAccountRepository.save(userAccount);

        UserDetails userDetails = userDetailsService.loadUserByUsername("manager");

        Set<String> authorities = userDetails.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .collect(Collectors.toSet());

        assertThat(userDetails.getUsername()).isEqualTo("manager");
        assertThat(authorities).containsExactlyInAnyOrder(
                "ROLE_MANAGER",
                "ORDER_READ",
                "ORDER_ACCEPT"
        );
    }
}
