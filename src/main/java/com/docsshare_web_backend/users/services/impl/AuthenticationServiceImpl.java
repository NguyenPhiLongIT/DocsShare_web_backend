package com.docsshare_web_backend.users.services.impl;

import com.docsshare_web_backend.documents.models.DocumentCoAuthor;
import com.docsshare_web_backend.documents.repositories.DocumentCoAuthorRepository;
import com.docsshare_web_backend.users.dto.requests.*;
import com.docsshare_web_backend.users.dto.responses.AuthenticationResponse;
import com.docsshare_web_backend.users.enums.UserStatus;
import com.docsshare_web_backend.users.enums.UserType;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;
import com.docsshare_web_backend.users.services.AuthenticationService;

import com.docsshare_web_backend.users.services.MailService;
import com.docsshare_web_backend.users.utils.VerificationCodeStore;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSender;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service("AuthenticationServiceImpl")
@Primary
public class AuthenticationServiceImpl implements AuthenticationService{
    private final static JwtUtils jwtUtils = new JwtUtils();
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DocumentCoAuthorRepository documentCoAuthorRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MailService mailService;

    @Autowired
    private VerificationCodeStore codeStore;


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
        // 1. Kiểm tra email đã tồn tại
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại!");
        }

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

        user = userRepository.save(user);

        // 3. Gán user_id cho các dòng DocumentCoAuthor trùng tên & email
        List<DocumentCoAuthor> coAuthors = documentCoAuthorRepository
                .findByNameAndEmail(registerRequest.getName(), registerRequest.getEmail());

        for (DocumentCoAuthor coAuthor : coAuthors) {
            coAuthor.setUser(user);
            coAuthor.setName(null);   // xoá name/email để tránh duplicate unique key
            coAuthor.setEmail(null);
            coAuthor.setIsConfirmed(true);
        }

        documentCoAuthorRepository.saveAll(coAuthors);

        return AuthenticationMapper.toAuthenticationResponse(user);
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

    @Override
    @Transactional
    public void changePassword(Long accountId, ChangePasswordRequest request) {
        User user = userRepository.findById(accountId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + accountId));

        // LOG
        log.info("🔐 Changing password for userId: {}", user.getId());
        log.info("🔐 Current encoded password (from DB): {}", user.getPassword());
        log.info("🔐 Old password (plain text from request): {}", request.getOldPassword());


        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new IllegalArgumentException("❌ Old password is incorrect.");
        }

        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new IllegalArgumentException("❌ New password must be different from the old password.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Gửi mail thông báo đổi mật khẩu
        mailService.sendChangePasswordNotification(user.getEmail(), user.getName());
    }

    @Override
    public void sendForgotPasswordCode(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email không tồn tại"));

        String code = String.valueOf(new Random().nextInt(900_000) + 100_000);
        codeStore.saveCode(email, code);

        String content = String.format("Xin chào %s,\n\nMã xác nhận đặt lại mật khẩu của bạn là: %s\nMã này có hiệu lực trong 10 phút.",
                user.getName(), code);
        mailService.sendCustomEmail(email, "🔐 Mã xác nhận đặt lại mật khẩu", content);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        String code = request.getCode();

        if (!codeStore.isValid(email, code)) {
            throw new IllegalArgumentException("❌ Mã xác nhận không hợp lệ hoặc đã hết hạn.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng."));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        codeStore.remove(email);

        mailService.sendChangePasswordNotification(user.getEmail(), user.getName());
    }
}