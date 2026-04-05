package com.integrityfamily.auth.service;

import com.integrityfamily.auth.domain.User;
import com.integrityfamily.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                mapAuthorities(user)
        );
    }

    private Collection<? extends GrantedAuthority> mapAuthorities(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return user.getRoles().stream()
                .map(role -> {
                    String name = role.getName();
                    if (name == null || name.isBlank()) {
                        name = "USER";
                    }
                    if (!name.startsWith("ROLE_")) {
                        name = "ROLE_" + name;
                    }
                    return new SimpleGrantedAuthority(name);
                })
                .toList();
    }
}