package com.integrityfamily.auth.service;

import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MasterCredentialService")
class MasterCredentialServiceTest {

    @Mock UserRepository  userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks MasterCredentialService service;

    private static final String ADMIN_EMAIL = "william@integrity.ia";

    @Test
    @DisplayName("crea al usuario admin cuando no existe previamente")
    void provisionMasterAdmin_userNotExists_createsUser() {
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$hashed$");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        service.provisionMasterAdmin();

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(saved.getPasswordHash()).isEqualTo("$hashed$");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getFullName()).contains("William");
    }

    @Test
    @DisplayName("no crea al usuario admin si ya existe")
    void provisionMasterAdmin_userAlreadyExists_skipsCreation() {
        User existing = User.builder().id(1L).email(ADMIN_EMAIL).build();
        when(userRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(existing));

        service.provisionMasterAdmin();

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(anyString());
    }
}
