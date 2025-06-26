package com.docsshare_web_backend.users.services;

import com.docsshare_web_backend.users.dto.requests.LoginRequest;
import com.docsshare_web_backend.users.dto.requests.RegisterRequest;
import com.docsshare_web_backend.users.dto.responses.AuthenticationResponse;

public interface AuthenticationService {
    AuthenticationResponse register(RegisterRequest registerRequest);
    AuthenticationResponse login(LoginRequest loginRequest);
    AuthenticationResponse googleLogin(com.docsshare_web_backend.users.dto.requests.GoogleAuthRequest request);
}