package com.docsshare_web_backend.users.services;

import com.docsshare_web_backend.users.dto.requests.ChangePasswordRequest;
import com.docsshare_web_backend.users.dto.requests.LoginRequest;
import com.docsshare_web_backend.users.dto.requests.RegisterRequest;
import com.docsshare_web_backend.users.dto.requests.ResetPasswordRequest;
import com.docsshare_web_backend.users.dto.responses.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest registerRequest);
    AuthenticationResponse login(LoginRequest loginRequest);
    AuthenticationResponse googleLogin(com.docsshare_web_backend.users.dto.requests.GoogleAuthRequest request);
    void changePassword(Long accountId, ChangePasswordRequest request);
    void sendForgotPasswordCode(String email);
    void resetPassword(ResetPasswordRequest request);



}