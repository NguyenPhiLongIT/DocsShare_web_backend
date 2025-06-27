package com.docsshare_web_backend.users.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    @NotNull(message = "Name cannot be null")
    private String name;
    @NotBlank(message = "Email is required")
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email should be valid")
    private String email;
    @NotBlank(message = "Password is required")
    @NotNull(message = "Password cannot be null")
    private String password;
    @NotBlank(message = "Nation is required")
    @NotNull(message = "Nation cannot be null")
    private String nation;
    private String degree;
    private String college;
    private String avatar;
    private String userType;
}
