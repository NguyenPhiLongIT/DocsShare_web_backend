package com.docsshare_web_backend.users.dto.requests;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {
    private String email;
    private String code; // mã xác nhận
    private String newPassword;
}
