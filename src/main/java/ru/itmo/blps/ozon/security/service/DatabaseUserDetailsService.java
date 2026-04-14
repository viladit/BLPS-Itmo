package ru.itmo.blps.ozon.security.service;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.blps.ozon.security.entity.Role;
import ru.itmo.blps.ozon.security.entity.UserAccount;
import ru.itmo.blps.ozon.security.repository.UserAccountRepository;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DatabaseUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        UserAccount userAccount = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User " + username + " was not found"));

        return User.withUsername(userAccount.getUsername())
                .password(userAccount.getPasswordHash())
                .disabled(!userAccount.isEnabled())
                .authorities(extractAuthorities(userAccount))
                .build();
    }

    private Set<GrantedAuthority> extractAuthorities(UserAccount userAccount) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (Role role : userAccount.getRoles()) {
            authorities.add(new SimpleGrantedAuthority(role.getName().name()));
            role.getPrivileges().forEach(privilege ->
                    authorities.add(new SimpleGrantedAuthority(privilege.getName().name()))
            );
        }
        return authorities;
    }
}
