package com.docsshare_web_backend.account.domain;

import com.docsshare_web_backend.account.dto.requests.AccountFilterRequest;
import com.docsshare_web_backend.account.dto.requests.AccountRequest;
import com.docsshare_web_backend.account.dto.responses.AccountResponse;
import com.docsshare_web_backend.account.enums.AccountStatus;
import com.docsshare_web_backend.account.services.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @GetMapping
    public ResponseEntity<Page<AccountResponse>> getAllAccounts(
            @ModelAttribute AccountFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        log.debug("Received request to get all Accounts with filter: {}, page: {}, size: {}, sort: {}",
                request, page, size, sort);
        Sort sortOrder = sort.equals("asc") ? Sort.by("createdAt").ascending() : Sort.by("createdAt").descending();
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        Page<AccountResponse> accounts = accountService.getAllAccounts(request, pageable);
        return ResponseEntity.ok(accounts);

    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable long accountId) {
        log.debug("[AccountController] Get Account with id {}", accountId);
        return ResponseEntity.ok(accountService.getAccount(accountId));
    }

//    @PostMapping("/create")
//    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountRequest accountRequest) {
//        log.debug("[AccountController] Create Account {}", accountRequest);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(accountService.createAccount(accountRequest));
//    }

    @PutMapping("/{accountId}/update")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable long accountId,
            @RequestBody AccountRequest request) {
        log.debug("[AccountController] Update Account with id {}", accountId);
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

}
