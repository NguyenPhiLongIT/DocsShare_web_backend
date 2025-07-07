package com.docsshare_web_backend.account.dto.responses;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
        private Long id;
        private String name;
        private String avatar;
        private String college;
}
