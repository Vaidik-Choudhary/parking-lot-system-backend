package com.parkease.auth.security;

import com.parkease.auth.entity.*;
import com.parkease.auth.repository.UserRepository;
import com.parkease.auth.service.RefreshTokenService;
import com.parkease.auth.util.JwtUtil;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;


@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email       = oAuth2User.getAttribute("email");
        String name        = oAuth2User.getAttribute("name");
        String picture     = oAuth2User.getAttribute("picture");
        String googleId    = oAuth2User.getAttribute("sub");

        log.info("OAuth2 login success for email: {}", email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = User.builder()
                    .fullName(name)
                    .email(email)
                    .role(Role.DRIVER)           
                    .provider(AuthProvider.GOOGLE)
                    .providerId(googleId)
                    .profilePicUrl(picture)
                    .active(true)
                    .build();
            log.info("Creating new OAuth2 user: {}", email);
            return userRepository.save(newUser);
        });

        String accessToken  = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user).getToken();

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/success")
                .queryParam("token", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
