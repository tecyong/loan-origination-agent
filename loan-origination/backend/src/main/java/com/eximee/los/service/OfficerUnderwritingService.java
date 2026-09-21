package com.eximee.los.service;

import com.eximee.los.domain.ApplicationStatus;
import com.eximee.los.domain.LoanApplication;
import com.eximee.los.domain.User;
import com.eximee.los.domain.WorkflowAuditLog;
import com.eximee.los.dto.LoanApplicationResponse;
import com.eximee.los.dto.OfficerDecisionRequest;
import com.eximee.los.repository.LoanApplicationRepository;
import com.eximee.los.repository.WorkflowAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class OfficerUnderwritingService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanApplicationService applicationService;
    private final WorkflowOrchestrationService workflowService;
    private final WorkflowAuditLogRepository auditLogRepository;

    public OfficerUnderwritingService(
            LoanApplicationRepository applicationRepository,
            LoanApplicationService applicationService,
            WorkflowOrchestrationService workflowService,
            WorkflowAuditLogRepository auditLogRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.applicationService = applicationService;
        this.workflowService = workflowService;
        this.auditLogRepository = auditLogRepository;
    }

    public List<LoanApplicationResponse> getApplications(ApplicationStatus status) {
        List<LoanApplication> list;
        if (status != null) {
            list = applicationRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            list = applicationRepository.findAllByOrderByCreatedAtDesc();
        }
        return list.stream().map(applicationService::toResponse).toList();
    }

    @Transactional
    public LoanApplicationResponse processDecision(Long id, User officer, OfficerDecisionRequest request) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

        if (application.getStatus() != ApplicationStatus.IN_REVIEW && application.getStatus() != ApplicationStatus.SUBMITTED) {
            throw new IllegalStateException("Cannot make underwriting decision on application in status: " + application.getStatus());
        }

        String action = request.action().toUpperCase().trim();
        ApplicationStatus previousStatus = application.getStatus();

        switch (action) {
            case "APPROVE" -> {
                application.setStatus(ApplicationStatus.APPROVED);
                application.setApprovedAmount(request.approvedAmount() != null ? request.approvedAmount() : application.getAmount());
                application.setDecisionReason(request.decisionReason());
                application.setDecidedBy(officer);
                application.setDecidedAt(OffsetDateTime.now());

                if (application.getProcessInstanceId() != null) {
                    workflowService.completeUnderwriterReview(
                            application.getProcessInstanceId(),
                            "APPROVE",
                            application.getApprovedAmount(),
                            request.decisionReason()
                    );
                }

                auditLogRepository.save(new WorkflowAuditLog(
                        application,
                        officer,
                        previousStatus.name(),
                        ApplicationStatus.APPROVED.name(),
                        "OFFICER_APPROVED",
                        "Approved with amount: $" + application.getApprovedAmount() + ". Notes: " + request.decisionReason()
                ));
            }
            case "REJECT" -> {
                application.setStatus(ApplicationStatus.REJECTED);
                application.setDecisionReason(request.decisionReason() != null ? request.decisionReason() : "Application does not meet credit underwriting requirements.");
                application.setDecidedBy(officer);
                application.setDecidedAt(OffsetDateTime.now());

                if (application.getProcessInstanceId() != null) {
                    workflowService.completeUnderwriterReview(
                            application.getProcessInstanceId(),
                            "REJECT",
                            null,
                            application.getDecisionReason()
                    );
                }

                auditLogRepository.save(new WorkflowAuditLog(
                        application,
                        officer,
                        previousStatus.name(),
                        ApplicationStatus.REJECTED.name(),
                        "OFFICER_REJECTED",
                        "Rejected. Reason: " + application.getDecisionReason()
                ));
            }
            case "REQUEST_INFO" -> {
                application.setStatus(ApplicationStatus.INFORMATION_REQUESTED);
                application.setDecisionReason(request.revisionNotes() != null ? request.revisionNotes() : "Additional financial details or documentation requested.");

                if (application.getProcessInstanceId() != null) {
                    workflowService.completeUnderwriterReview(
                            application.getProcessInstanceId(),
                            "REQUEST_INFO",
                            null,
                            application.getDecisionReason()
                    );
                }

                auditLogRepository.save(new WorkflowAuditLog(
                        application,
                        officer,
                        previousStatus.name(),
                        ApplicationStatus.INFORMATION_REQUESTED.name(),
                        "REVISION_REQUESTED",
                        "Underwriter requested revisions: " + application.getDecisionReason()
                ));
            }
            default -> throw new IllegalArgumentException("Unknown decision action: " + action);
        }

        LoanApplication saved = applicationRepository.save(application);
        return applicationService.toResponse(saved);
    }
}
