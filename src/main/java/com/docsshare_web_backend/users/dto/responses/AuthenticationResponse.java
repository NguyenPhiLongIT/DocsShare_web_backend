package com.docsshare_web_backend.users.dto.responses;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor  
@AllArgsConstructor
public class AuthenticationResponse {
    private String tokenType;
    private String email;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;
}