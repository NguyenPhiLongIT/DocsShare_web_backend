package com.docsshare_web_backend.account.domain;

import com.docsshare_web_backend.account.dto.requests.AccountFilterRequest;
import com.docsshare_web_backend.account.dto.requests.AccountRequest;
import com.docsshare_web_backend.account.dto.responses.AccountResponse;
import com.docsshare_web_backend.account.dto.responses.TopUserAddDocumentResponse;
import com.docsshare_web_backend.account.enums.AccountStatus;
import com.docsshare_web_backend.account.services.AccountService;
import com.docsshare_web_backend.commons.services.ExcelExportService;
import com.docsshare_web_backend.documents.dto.responses.TopDocumentReportResponse;
import com.docsshare_web_backend.documents.services.DocumentService;
import com.docsshare_web_backend.order.dto.requests.OrderFilterRequest;
import com.docsshare_web_backend.order.dto.responses.OrderResponse;
import com.docsshare_web_backend.users.enums.UserStatus;
import com.docsshare_web_backend.users.enums.UserType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {
    @Autowired
    private AccountService accountService;

    @Autowired
    private DocumentService documentService;

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

    @GetMapping("/nation")
    public ResponseEntity<Page<AccountResponse>> getAccountByNation(
            @RequestParam String nation,
            @ModelAttribute AccountFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort.equalsIgnoreCase("asc") ? Sort.by("createdAt").ascending()
                        : Sort.by("createdAt").descending()
        );

        Page<AccountResponse> accounts = accountService.getAccountsByNation(nation, pageable);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/degree")
    public ResponseEntity<Page<AccountResponse>> getAccountByDegree(
            @RequestParam String degree,
            @ModelAttribute AccountFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort.equalsIgnoreCase("asc") ? Sort.by("createdAt").ascending()
                        : Sort.by("createdAt").descending()
        );

        Page<AccountResponse> accounts = accountService.getAccountsByDegree(degree, pageable);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/college")
    public ResponseEntity<Page<AccountResponse>> getAccountByCollege(
            @RequestParam String college,
            @ModelAttribute AccountFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(defaultValue = "desc") String sort) {
        Pageable pageable = PageRequest.of(
                page,
                size,
                sort.equalsIgnoreCase("asc") ? Sort.by("createdAt").ascending()
                        : Sort.by("createdAt").descending()
        );

        Page<AccountResponse> accounts = accountService.getAccountsByCollege(college, pageable);
        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{accountId}/update")
    public ResponseEntity<AccountResponse> updateAccount(
            @PathVariable long accountId,
            @RequestBody AccountRequest request) {
        log.debug("[AccountController] Update Account with id {}", accountId);
        return ResponseEntity.ok(accountService.updateAccount(accountId, request));
    }

    @PutMapping("/{accountId}/updated/status" )
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @PathVariable long accountId,
            UserType userType, UserStatus status
    ){
        log.debug("[AccountController] Update Account with id {}", accountId);
        return ResponseEntity.ok(accountService.updateAccountStatus(accountId, userType, status));
    }

    @GetMapping("/export")
    public void exportAccountExcel(
            @ModelAttribute AccountFilterRequest filterRequest,
            HttpServletResponse response
    ) {
        Page<AccountResponse> page = accountService.getAllAccounts(filterRequest, Pageable.unpaged());
        List<AccountResponse> data = page.getContent();

        new ExcelExportService<AccountResponse>().export(response, "order_export", data);
    }
    @GetMapping("/top-users-add-document")
    public ResponseEntity<List<TopUserAddDocumentResponse>> getTopUsersAddDocument(
            @RequestParam("fromDate") LocalDate fromDate,
            @RequestParam("toDate") LocalDate toDate,
            @RequestParam(defaultValue = "10") int top) {

        List<TopUserAddDocumentResponse> result = documentService.getTopUsersAddDocumentBetween(fromDate, toDate, top);
        return ResponseEntity.ok(result);
    }
}
