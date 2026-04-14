package ru.itmo.blps.ozon.security.bootstrap;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.blps.ozon.security.PrivilegeName;
import ru.itmo.blps.ozon.security.RoleName;
import ru.itmo.blps.ozon.security.SecuritySeedProperties;
import ru.itmo.blps.ozon.security.entity.Privilege;
import ru.itmo.blps.ozon.security.entity.Role;
import ru.itmo.blps.ozon.security.entity.UserAccount;
import ru.itmo.blps.ozon.security.repository.PrivilegeRepository;
import ru.itmo.blps.ozon.security.repository.RoleRepository;
import ru.itmo.blps.ozon.security.repository.UserAccountRepository;

@Component
@ConditionalOnProperty(name = "app.security.seed.enabled", havingValue = "true", matchIfMissing = true)
public class SecurityDataSeeder implements ApplicationRunner {

    private final PrivilegeRepository privilegeRepository;
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecuritySeedProperties seedProperties;

    public SecurityDataSeeder(PrivilegeRepository privilegeRepository,
                              RoleRepository roleRepository,
                              UserAccountRepository userAccountRepository,
                              PasswordEncoder passwordEncoder,
                              SecuritySeedProperties seedProperties) {
        this.privilegeRepository = privilegeRepository;
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedProperties = seedProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Privilege orderCreate = ensurePrivilege(PrivilegeName.ORDER_CREATE);
        Privilege orderRead = ensurePrivilege(PrivilegeName.ORDER_READ);
        Privilege orderAccept = ensurePrivilege(PrivilegeName.ORDER_ACCEPT);
        Privilege orderPack = ensurePrivilege(PrivilegeName.ORDER_PACK);
        Privilege orderHandoff = ensurePrivilege(PrivilegeName.ORDER_HANDOFF);
        Privilege orderDeliver = ensurePrivilege(PrivilegeName.ORDER_DELIVER);
        Privilege orderCancel = ensurePrivilege(PrivilegeName.ORDER_CANCEL);

        Role manager = ensureRole(
                RoleName.ROLE_MANAGER,
                orderCreate, orderRead, orderAccept, orderCancel
        );
        Role warehouse = ensureRole(
                RoleName.ROLE_WAREHOUSE,
                orderRead, orderPack
        );
        Role delivery = ensureRole(
                RoleName.ROLE_DELIVERY,
                orderRead, orderHandoff, orderDeliver
        );
        Role admin = ensureRole(
                RoleName.ROLE_ADMIN,
                orderCreate, orderRead, orderAccept, orderPack, orderHandoff, orderDeliver, orderCancel
        );

        ensureUser("manager", seedProperties.getManagerPassword(), manager);
        ensureUser("warehouse", seedProperties.getWarehousePassword(), warehouse);
        ensureUser("delivery", seedProperties.getDeliveryPassword(), delivery);
        ensureUser("admin", seedProperties.getAdminPassword(), admin);
    }

    private Privilege ensurePrivilege(PrivilegeName privilegeName) {
        return privilegeRepository.findByName(privilegeName)
                .orElseGet(() -> privilegeRepository.save(new Privilege(privilegeName)));
    }

    private Role ensureRole(RoleName roleName, Privilege... privileges) {
        Role role = roleRepository.findByName(roleName).orElseGet(() -> {
            Role newRole = new Role(roleName);
            newRole.setPrivileges(new LinkedHashSet<>(Arrays.asList(privileges)));
            return newRole;
        });

        if (role.getId() != null) {
            Set<Privilege> mergedPrivileges = new LinkedHashSet<>(role.getPrivileges());
            mergedPrivileges.addAll(Arrays.asList(privileges));
            role.setPrivileges(mergedPrivileges);
        }

        return roleRepository.save(role);
    }

    private void ensureUser(String username, String rawPassword, Role role) {
        if (userAccountRepository.findByUsername(username).isPresent()) {
            return;
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        userAccount.setEnabled(true);
        userAccount.setPasswordHash(passwordEncoder.encode(rawPassword));
        Set<Role> roles = new LinkedHashSet<>();
        roles.add(role);
        userAccount.setRoles(roles);
        userAccountRepository.save(userAccount);
    }
}
