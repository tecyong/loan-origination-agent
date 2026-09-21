package com.eximee.los.controller;

import com.eximee.los.domain.ApplicationStatus;
import com.eximee.los.domain.User;
import com.eximee.los.dto.LoanApplicationResponse;
import com.eximee.los.dto.OfficerDecisionRequest;
import com.eximee.los.service.AuthService;
import com.eximee.los.service.LoanApplicationService;
import com.eximee.los.service.OfficerUnderwritingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/officer/applications")
@PreAuthorize("hasAnyAuthority('ROLE_OFFICER', 'ROLE_ADMIN')")
@Tag(name = "Underwriting Operations", description = "Loan officer queue inspection and decisioning endpoints")
public class OfficerController {

    private final OfficerUnderwritingService underwritingService;
    private final LoanApplicationService applicationService;
    private final AuthService authService;

    public OfficerController(
            OfficerUnderwritingService underwritingService,
            LoanApplicationService applicationService,
            AuthService authService
    ) {
        this.underwritingService = underwritingService;
        this.applicationService = applicationService;
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Get applications in review queue with optional status filter")
    public ResponseEntity<List<LoanApplicationResponse>> getQueue(
            @RequestParam(required = false) ApplicationStatus status
    ) {
        return ResponseEntity.ok(underwritingService.getApplications(status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full 360-degree application profile for underwriting")
    public ResponseEntity<LoanApplicationResponse> getDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id
    ) {
        User officer = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(applicationService.getApplicationById(id, officer));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Submit underwriting decision (APPROVE, REJECT, REQUEST_INFO)")
    public ResponseEntity<LoanApplicationResponse> processDecision(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody OfficerDecisionRequest request
    ) {
        User officer = authService.getUserByEmail(userDetails.getUsername());
        return ResponseEntity.ok(underwritingService.processDecision(id, officer, request));
    }
}
