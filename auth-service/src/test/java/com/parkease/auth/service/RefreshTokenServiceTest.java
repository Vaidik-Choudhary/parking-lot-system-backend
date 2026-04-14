package com.parkease.auth.service;

import com.parkease.auth.entity.RefreshToken;
import com.parkease.auth.entity.User;
import com.parkease.auth.exception.InvalidTokenException;
import com.parkease.auth.repository.RefreshTokenRepository;
import com.parkease.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService service;

    private User user;
    private RefreshToken token;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "refreshTokenDurationMs", 604800000L);

        user = User.builder()
                .id(1L)
                .email("vaidik@test.com")
                .build();

        token = RefreshToken.builder()
                .token("refresh_123")
                .user(user)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    void shouldCreateNewRefreshToken() {
        when(refreshTokenRepository.findByUser(user))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = service.createRefreshToken(user);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertNotNull(result.getToken());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void shouldUpdateExistingRefreshToken() {
        when(refreshTokenRepository.findByUser(user))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = service.createRefreshToken(user);

        assertNotNull(result);
        assertEquals(user, result.getUser());
        assertNotNull(result.getToken());
        verify(refreshTokenRepository).save(token);
    }

    @Test
    void shouldValidateRefreshTokenSuccessfully() {
        when(refreshTokenRepository.findByToken("refresh_123"))
                .thenReturn(Optional.of(token));

        RefreshToken result = service.validateRefreshToken("refresh_123");

        assertNotNull(result);
        assertEquals("refresh_123", result.getToken());
    }

    @Test
    void shouldThrowWhenRefreshTokenNotFound() {
        when(refreshTokenRepository.findByToken("missing"))
                .thenReturn(Optional.empty());

        assertThrows(InvalidTokenException.class,
                () -> service.validateRefreshToken("missing"));
    }

    @Test
    void shouldThrowWhenRefreshTokenExpired() {
        token.setExpiryDate(Instant.now().minusSeconds(10));

        when(refreshTokenRepository.findByToken("refresh_123"))
                .thenReturn(Optional.of(token));

        assertThrows(InvalidTokenException.class,
                () -> service.validateRefreshToken("refresh_123"));

        verify(refreshTokenRepository).delete(token);
    }

    @Test
    void shouldDeleteByUserSuccessfully() {
        service.deleteByUser(user);

        verify(refreshTokenRepository).deleteByUser(user);
    }
}