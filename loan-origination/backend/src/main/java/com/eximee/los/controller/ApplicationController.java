package com.eximee.los.controller;

import com.eximee.los.domain.User;
import com.eximee.los.dto.LoanApplicationCreateRequest;
import com.eximee.los.dto.LoanApplicationResponse;
import com.eximee.los.dto.LoanApplicationUpdateRequest;
import com.eximee.los.service.AuthService;
import com.eximee.los.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Applications", description = "Applicant loan application lifecycle endpoints")
public class ApplicationController {

    private final LoanApplicationService applicationService;
    private final AuthService authService;

    public ApplicationController(LoanApplicationService applicationService, AuthService authService) {
        this.applicationService = applicationService;
        this.authService = authService;
    }

    @PostMapping
    @Operation(summary = "Create a new loan application in DRAFT status")
    public ResponseEntity<LoanApplicationResponse> createDraft(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody LoanApplicationCreateRequest request
    ) {
        User applicant = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.createDraft(applicant, request));
    }

    @GetMapping("/my")
    @Operation(summary = "Get all applications submitted by the logged-in applicant")
    public ResponseEntity<List<LoanApplicationResponse>> getMyApplications(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User applicant = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.getMyApplications(applicant));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application details by ID")
    public ResponseEntity<LoanApplicationResponse> getApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        User user = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.getApplicationById(id, user));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update application fields (allowed in DRAFT or INFORMATION_REQUESTED)")
    public ResponseEntity<LoanApplicationResponse> updateApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody LoanApplicationUpdateRequest request
    ) {
        User applicant = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.updateApplication(id, applicant, request));
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit application for review (starts EximeeBPMS process)")
    public ResponseEntity<LoanApplicationResponse> submitApplication(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        User applicant = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.submitApplication(id, applicant));
    }
}
