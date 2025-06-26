package com.docsshare_web_backend.users.dto.requests;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleAuthRequest {
    private String idToken;
} 
