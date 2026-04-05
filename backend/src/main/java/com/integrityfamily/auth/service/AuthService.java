package com.integrityfamily.auth.service;

import com.integrityfamily.auth.dto.*;
import com.integrityfamily.auth.domain.User;
import com.integrityfamily.auth.repository.UserRepository;
import com.integrityfamily.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager, 
                       JwtService jwtService, 
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String token = jwtService.generateToken((org.springframework.security.core.userdetails.UserDetails) auth.getPrincipal());
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Retorna el objeto AuthResponse completo (5 campos)
        return new AuthResponse(user.getId(), user.getEmail(), token, user.getFullName(), roles);
    }

    public AuthResponse register(RegisterRequest request) {
        // En una implementación real aquí se guarda el usuario. 
        // Por ahora, devolvemos un objeto vacío o simulado para que el controlador compile.
        return new AuthResponse(0L, request.email(), "token_simulado", request.fullName(), List.of("ROLE_USER"));
    }
}