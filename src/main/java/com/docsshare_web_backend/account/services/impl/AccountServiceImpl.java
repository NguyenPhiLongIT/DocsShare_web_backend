package com.docsshare_web_backend.account.services.impl;

import com.docsshare_web_backend.account.dto.requests.AccountFilterRequest;
import com.docsshare_web_backend.account.dto.requests.AccountRequest;
import com.docsshare_web_backend.account.dto.responses.AccountResponse;
import com.docsshare_web_backend.account.enums.AccountStatus;
import com.docsshare_web_backend.account.filters.AccountFilter;
import com.docsshare_web_backend.account.repositories.AccountRepository;
import com.docsshare_web_backend.account.services.AccountService;
import com.docsshare_web_backend.users.enums.UserType;
import com.docsshare_web_backend.users.models.User;
import com.docsshare_web_backend.users.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class AccountServiceImpl implements AccountService {
        @Autowired
        private AccountRepository accountRepository;

        @Autowired
        private UserRepository userRepository;

        private Pageable getPageable(Pageable pageable) {
                return pageable != null ? pageable : Pageable.unpaged();
        }

        public static class AccountMapper {
                public static AccountResponse toAccountResponse(User account) {
                        return AccountResponse.builder()
                                        .id(account.getId())
                                        .name(account.getName())
                                        .email(account.getEmail())
                                        .nation(account.getNation())
                                        .degree(account.getDegree())
                                        .college(account.getCollege())
                                        .avatar(account.getAvatar())
                                        .status(account.getStatus() != null
                                                ? account.getStatus() : null)
                                .userType(account.getUserType() != null ? account.getUserType() : null)
                                        .createAt(account.getCreatedAt())
                                        .build();
                }
        }

        @Override
        @Transactional(readOnly = true)
        public Page<AccountResponse> getAllAccounts(AccountFilterRequest request, Pageable pageable) {
                Specification<User> spec = AccountFilter.filterByRequest(request);
                return accountRepository.findAll(spec, getPageable(pageable))
                                .map(AccountMapper::toAccountResponse);

        }

        @Override
        @Transactional(readOnly = true)
        public AccountResponse getAccount(long id) {
                User account = accountRepository.findById(id)
                                .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + id));

                return AccountMapper.toAccountResponse(account);
        }

        @Override
        @Transactional
        public AccountResponse updateAccount(long accountId, AccountRequest request) {
                User existingAccount = accountRepository.findById(accountId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Account not found with id: " + accountId));

                existingAccount.setName(request.getName());
                // existingAccount.setEmail(request.getEmail());
                existingAccount.setNation(request.getNation());
                existingAccount.setDegree(request.getDegree());
                existingAccount.setCollege(request.getCollege());
                existingAccount.setAvatar(request.getAvatar());
                // existingAccount.setPassword(request.getPassword()); // nếu cho phép cập nhật mật khẩu

                if (request.getUserType() != null) {
                        existingAccount.setUserType(request.getUserType());
                }

                if (request.getStatus() != null) {
                        existingAccount.setStatus(request.getStatus());
                }


                User updatedAccount = accountRepository.save(existingAccount);
                return AccountMapper.toAccountResponse(updatedAccount);
        }

        @Override
        public Page<AccountResponse> getAccountsByNation(String nation, Pageable pageable) {
                Page<User> users = accountRepository.findByNationIgnoreCaseContaining(nation, pageable);
                return users.map(AccountMapper::toAccountResponse);
        }

        @Override
        public Page<AccountResponse> getAccountsByDegree(String degree, Pageable pageable) {
                Page<User> users = accountRepository.findByDegreeIgnoreCaseContaining(degree, pageable);
                return users.map(AccountMapper::toAccountResponse);
        }

        @Override
        public Page<AccountResponse> getAccountsByCollege(String college, Pageable pageable) {
                Page<User> users = accountRepository.findByCollegeIgnoreCaseContaining(college, pageable);
                return users.map(AccountMapper::toAccountResponse);
        }
}
