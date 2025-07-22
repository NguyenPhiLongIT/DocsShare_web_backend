package com.docsshare_web_backend.users.domains;

import com.docsshare_web_backend.users.dto.requests.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import com.docsshare_web_backend.commons.utils.JwtUtils;
import com.docsshare_web_backend.users.dto.responses.AuthenticationResponse;
import com.docsshare_web_backend.users.services.AuthenticationService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Objects;
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    @Autowired
    @Qualifier("AuthenticationServiceImpl")
    private AuthenticationService authenticationService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    private ResponseCookie createCookie(String tokenName, String tokenValue, long expiresIn) {
        return ResponseCookie.from(tokenName, tokenValue)
                .httpOnly(false)
                .secure(false)
                .maxAge(expiresIn)
                .path("/")
                .build();
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            return ResponseEntity.ok(authenticationService.register(request));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Objects.requireNonNull(e.getRootCause()).getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Register failed");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request
    ) {

        try {
            AuthenticationResponse authenticationResponse = authenticationService.login(request);
            ResponseCookie accessCookieToken = createCookie(JwtUtils.TOKEN_NAME, authenticationResponse.getAccessToken(), authenticationResponse.getExpiresIn());
            ResponseCookie refreshCookieToken = createCookie(JwtUtils.REFRESH_TOKEN_NAME, authenticationResponse.getRefreshToken(), Duration.ofDays(7).getSeconds());

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .headers(httpHeaders -> {
                        httpHeaders.add(HttpHeaders.SET_COOKIE, accessCookieToken.toString());
                        httpHeaders.add(HttpHeaders.SET_COOKIE, refreshCookieToken.toString());
                    })
                    .body(authenticationResponse);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthenticationResponse> logout() {
        System.out.println("[Logout] Logging out user");

        ResponseCookie deleteAccessTokenCookie = createCookie(JwtUtils.TOKEN_NAME, "", 0);
        ResponseCookie deleteRefreshTokenCookie = createCookie(JwtUtils.REFRESH_TOKEN_NAME, "", 0);

        SecurityContextHolder.getContext().setAuthentication(null);

        return ResponseEntity
                .status(HttpStatus.OK)
                .headers(httpHeaders -> {
                    httpHeaders.add(HttpHeaders.SET_COOKIE, deleteAccessTokenCookie.toString());
                    httpHeaders.add(HttpHeaders.SET_COOKIE, deleteRefreshTokenCookie.toString());
                })
                .build();
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody com.docsshare_web_backend.users.dto.requests.GoogleAuthRequest request) {
        return ResponseEntity.ok(authenticationService.googleLogin(request));
    }

    @PostMapping("/{accountId}/change-password")
    public ResponseEntity<?> changePassword(
            @PathVariable Long accountId,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        try {
            authenticationService.changePassword(accountId, request);
            return ResponseEntity.ok("🔐 Password changed successfully.");
        } catch (Exception e) {
            log.error("Lỗi khi đổi mật khẩu: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> sendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        try {
            authenticationService.sendForgotPasswordCode(request.getEmail());
            return ResponseEntity.ok("✅ OTP has been sent to your email.");
        } catch (Exception e) {
            log.error("Lỗi khi gửi OTP: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        try {
            authenticationService.resetPassword(request);
            return ResponseEntity.ok("🔐 Password reset successfully.");
        } catch (Exception e) {
            log.error("Lỗi khi đặt lại mật khẩu: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }


}
