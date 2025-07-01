package com.docsshare_web_backend.account.dto.requests;

import com.docsshare_web_backend.account.enums.AccountStatus;
import com.docsshare_web_backend.users.enums.UserStatus;
import com.docsshare_web_backend.users.enums.UserType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Setter
@Getter
public class AccountFilterRequest {
    private String q;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdDate_from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdDate_to;
    private UserType userType;
    private UserStatus status;
}
