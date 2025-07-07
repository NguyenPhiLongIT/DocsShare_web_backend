package com.docsshare_web_backend.account.repositories;

import com.docsshare_web_backend.users.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface AccountRepository
    extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String name);
    //Optional<User> findByGoogleId(String googleId);
    Page<User> findByNationIgnoreCaseContaining(String nation, Pageable pageable);
    Page<User> findByDegreeIgnoreCaseContaining(String degree, Pageable pageable);
    Page<User> findByCollegeIgnoreCaseContaining(String college, Pageable pageable);
}
