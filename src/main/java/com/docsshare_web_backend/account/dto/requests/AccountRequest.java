package com.docsshare_web_backend.account.dto.requests;

import com.docsshare_web_backend.account.enums.AccountStatus;
import com.docsshare_web_backend.users.enums.UserStatus;
import com.docsshare_web_backend.users.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {
    @NotBlank(message = "Name is required")
    @NotNull(message = "Name cannot be null")
    private String name;

    @NotBlank(message = "Nation is required")
    @NotNull(message = "Nation cannot be null")

    private String nation;
    private String degree;
    private String college;
    private String avatar;
}
