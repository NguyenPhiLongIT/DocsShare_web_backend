package com.docsshare_web_backend.account.services;

import com.docsshare_web_backend.account.dto.requests.AccountFilterRequest;
import com.docsshare_web_backend.account.dto.requests.AccountRequest;
import com.docsshare_web_backend.account.dto.responses.AccountResponse;
import com.docsshare_web_backend.account.enums.AccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public interface AccountService {
    Page<AccountResponse> getAllAccounts(AccountFilterRequest request, Pageable pageable);
    AccountResponse getAccount(long id);
    //AccountResponse createDocument(AccountRequest request);
    AccountResponse updateAccount(long documentId, AccountRequest request);
    //AccountResponse updateDocumentStatus(long id, AccountStatus status);
    Page<AccountResponse> getAccountsByNation(String nation, Pageable pageable);
    Page<AccountResponse> getAccountsByDegree(String degree, Pageable pageable);
    Page<AccountResponse> getAccountsByCollege(String college, Pageable pageable);
}
