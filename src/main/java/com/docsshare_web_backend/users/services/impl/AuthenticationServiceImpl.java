package com.docsshare_web_backend.users.services.impl;

import com.docsshare_web_backend.users.dto.requests.GoogleAuthRequest;
import com.docsshare_web_backend.users.dto.requests.LoginRequest;
import com.docsshare_web_backend.users.dto.requests.RegisterRequest;
import com.docsshare_web_backend.users.dto.responses.AuthenticationResponse;
import com.docsshare_web_backend.users.enums.UserStatus;
import com.docsshare_web_backend.users.enums.UserType;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;
import com.docsshare_web_backend.users.services.AuthenticationService;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import com.docsshare_web_backend.commons.utils.JwtUtils;

import io.jsonwebtoken.Claims;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.web.client.HttpClientErrorException;

@Service("AuthenticationServiceImpl")
@Primary
public class AuthenticationServiceImpl implements AuthenticationService{
    private final static JwtUtils jwtUtils = new JwtUtils();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;

    public static class AuthenticationMapper {
        public static AuthenticationResponse toAuthenticationResponse(User user) {
            String jwtToken = jwtUtils.generateToken(user);
            String refreshToken = jwtUtils.generateRefreshToken(user);

            return AuthenticationResponse.builder()
                    .tokenType("Bear")
                    .email(user.getEmail())
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtUtils.extractClaims(jwtToken, Claims::getExpiration).getTime())
                    .build();
        }
    }

    @Override
    public AuthenticationResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        ));

        // Set authentication in SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();


        return AuthenticationMapper.toAuthenticationResponse(user);
    }

    @Override
    public AuthenticationResponse register(RegisterRequest registerRequest) {
        User user = User.builder()
                .name(registerRequest.getName())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .email(registerRequest.getEmail())
                .nation(registerRequest.getNation())
                .degree(registerRequest.getDegree())
                .college(registerRequest.getCollege())
                .avatar(registerRequest.getAvatar())
                .userType(UserType.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return AuthenticationMapper.toAuthenticationResponse(userRepository.save(user));
    }

    @Override
    public AuthenticationResponse googleLogin(GoogleAuthRequest request) {
        String idToken = request.getIdToken();
        String url = "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> body;
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            body = response.getBody();
        } catch (HttpClientErrorException e) {
            // Google trả về lỗi 400 nếu token không hợp lệ
            throw new RuntimeException("Invalid Google id_token: " + e.getResponseBodyAsString());
        }
        if (body == null || !( "accounts.google.com".equals(body.get("iss")) || "https://accounts.google.com".equals(body.get("iss")) )) {
            throw new RuntimeException("Invalid issuer");
        }
        if (!"true".equals(String.valueOf(body.get("email_verified")))) {
            throw new RuntimeException("Email not verified");
        }
        String googleId = (String) body.get("sub");
        String email = (String) body.get("email");
        String name = (String) body.get("name");
        // String picture = (String) body.get("picture");
        User user = userRepository.findByGoogleId(googleId)
            .orElseGet(() -> userRepository.findByEmail(email).orElse(null));
        if (user == null) {
            user = User.builder()
                .googleId(googleId)
                .email(email) 
                .name(name)
                .password(passwordEncoder.encode(email.split("@")[0]))
                .status(UserStatus.ACTIVE)
                .userType(UserType.USER)
                .build();
            user = userRepository.save(user);
        } else if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }
        return AuthenticationMapper.toAuthenticationResponse(user);
    }
}