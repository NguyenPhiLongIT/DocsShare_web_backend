package com.docsshare_web_backend.users.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.docsshare_web_backend.users.enums.UserStatus;
import com.docsshare_web_backend.users.enums.UserType;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    private String nation;
    private String degree;
    private String college;
    private String avatar;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    private UserType userType;
    
    @Enumerated(EnumType.STRING)
    private UserStatus status;
    
    @CreatedDate
    private LocalDateTime created_at;
}
