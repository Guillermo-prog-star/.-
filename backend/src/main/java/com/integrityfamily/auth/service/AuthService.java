package com.integrityfamily.auth.service;

import com.integrityfamily.auth.dto.*;
import com.integrityfamily.auth.domain.User;
import com.integrityfamily.auth.repository.UserRepository;
import com.integrityfamily.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String token = jwtService.generateToken(
                (org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal()
            );
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            return new AuthResponse(user.getId(), user.getEmail(), token, user.getFullName(), roles);

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Credenciales incorrectas");
        }
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("El correo ya está registrado");
        }

        User newUser = new User();
        newUser.setFullName(request.fullName());
        newUser.setEmail(request.email());
        newUser.setPassword(passwordEncoder.encode(request.password()));
        newUser.setActive(true);

        userRepository.save(newUser);

        // Generamos token inmediatamente para login automático post-registro
        String token = jwtService.generateToken(newUser);

        return new AuthResponse(newUser.getId(), newUser.getEmail(), token, newUser.getFullName(), List.of("ROLE_USER"));
    }
}