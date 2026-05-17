package com.parkease.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parkease.auth.dto.request.*;
import com.parkease.auth.dto.response.AuthResponse;
import com.parkease.auth.dto.response.UserResponse;
import com.parkease.auth.entity.Role;
import com.parkease.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;



import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;
    @InjectMocks private AuthController controller;

    private MockMvc mvc;
    private ObjectMapper om = new ObjectMapper();

    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.getParameterType().isAssignableFrom(UserDetails.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        UserDetails user = mock(UserDetails.class);
                        when(user.getUsername()).thenReturn("test@test.com");
                        return user;
                    }
                })
                .build();

        authResponse = AuthResponse.builder()
                .accessToken("access_token")
                .refreshToken("refresh_token")
                .email("test@test.com")
                .fullName("Test User")
                .role(Role.DRIVER)
                .build();

        UserResponse.builder()
                .email("test@test.com")
                .fullName("Test User")
                .role(Role.DRIVER)
                .build();
    }

    @Test
    void register_shouldReturn201() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("test@test.com");
        req.setPassword("password123");
        req.setRole(Role.DRIVER);

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access_token"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void login_shouldReturn200() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@test.com");
        req.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"));
    }

    @Test
    void refresh_shouldReturn200() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("refresh_token");

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(authResponse);

        mvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void forgotPassword_shouldReturn200() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest();
        req.setEmail("test@test.com");

        doNothing().when(authService).forgotPassword(any(ForgotPasswordRequest.class));

        mvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void resetPassword_shouldReturn200() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest();
        req.setToken("reset_token");
        req.setNewPassword("newpassword");

        doNothing().when(authService).resetPassword(any(ResetPasswordRequest.class));

        mvc.perform(post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void logout_shouldReturn200() throws Exception {
        mvc.perform(post("/api/auth/logout")
                .principal(() -> "test@test.com"))
                .andExpect(status().isOk());
    }

    @Test
    void getProfile_shouldReturn200() throws Exception {
        UserResponse profile = UserResponse.builder().email("test@test.com").build();
        when(authService.getProfile(anyString())).thenReturn(profile);

        mvc.perform(get("/api/auth/me")
                .principal(() -> "test@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    void changePassword_shouldReturn200() throws Exception {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setOldPassword("oldpassword");
        req.setNewPassword("newpassword123");

        doNothing().when(authService).changePassword(anyString(), any());

        mvc.perform(post("/api/auth/change-password")
                .principal(() -> "test@test.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }
}
