package com.docsshare_web_backend.account.dto.responses;

import com.docsshare_web_backend.account.enums.AccountStatus;
import com.docsshare_web_backend.users.enums.UserStatus;
import com.docsshare_web_backend.users.enums.UserType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountResponse {
    private Long id;
    private String name;
    private String email;
    private String nation;
    private String degree;
    private String college;
    private String avatar;
    private UserType userType;
    private UserStatus status;
    private LocalDateTime createAt;
}
