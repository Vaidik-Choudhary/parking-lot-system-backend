package com.parkease.auth.security;

import com.parkease.auth.entity.AuthProvider;
import com.parkease.auth.entity.Role;
import com.parkease.auth.entity.User;
import com.parkease.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserDetailsServiceImpl service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L).fullName("Test User").email("test@test.com")
                .password("encodedPassword").role(Role.DRIVER)
                .provider(AuthProvider.LOCAL).active(true)
                .build();
    }

    @Test
    void loadUserByUsername_activeUser_shouldReturnUserDetails() {
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@test.com");

        assertNotNull(details);
        assertEquals("test@test.com", details.getUsername());
        assertEquals("encodedPassword", details.getPassword());
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DRIVER")));
    }

    @Test
    void loadUserByUsername_inactiveUser_shouldReturnLockedAccount() {
        user.setActive(false);
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@test.com");

        assertFalse(details.isEnabled());
        assertFalse(details.isAccountNonLocked());
    }

    @Test
    void loadUserByUsername_notFound_shouldThrowUsernameNotFoundException() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("unknown@test.com"));
    }

    @Test
    void loadUserByUsername_oauthUserNullPassword_shouldUseEmptyPassword() {
        user.setPassword(null);
        user.setProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmail("oauth@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("oauth@test.com");

        assertEquals("", details.getPassword());
    }

    @Test
    void loadUserByUsername_adminRole_shouldHaveAdminAuthority() {
        user.setRole(Role.ADMIN);
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("admin@test.com");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }
}
