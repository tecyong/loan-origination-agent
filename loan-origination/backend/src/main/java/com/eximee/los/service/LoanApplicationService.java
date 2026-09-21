package com.eximee.los.service;

import com.eximee.los.domain.*;
import com.eximee.los.dto.*;
import com.eximee.los.repository.LoanApplicationRepository;
import com.eximee.los.repository.WorkflowAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Year;
import java.util.List;
import java.util.Random;

@Service
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final LoanProductService productService;
    private final WorkflowOrchestrationService workflowService;
    private final DocumentStorageService documentService;
    private final WorkflowAuditLogRepository auditLogRepository;

    public LoanApplicationService(
            LoanApplicationRepository applicationRepository,
            LoanProductService productService,
            WorkflowOrchestrationService workflowService,
            DocumentStorageService documentService,
            WorkflowAuditLogRepository auditLogRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.productService = productService;
        this.workflowService = workflowService;
        this.documentService = documentService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public LoanApplicationResponse createDraft(User applicant, LoanApplicationCreateRequest request) {
        LoanProduct product = productService.getEntityById(request.productId());

        validateProductBounds(product, request.amount(), request.tenureMonths());

        LoanApplication application = new LoanApplication();
        application.setApplicationNumber(generateApplicationNumber());
        application.setApplicant(applicant);
        application.setProduct(product);
        application.setAmount(request.amount());
        application.setTenureMonths(request.tenureMonths());
        application.setPurpose(request.purpose());
        application.setEmploymentType(request.employmentType());
        application.setEmployerName(request.employerName());
        application.setGrossMonthlyIncome(request.grossMonthlyIncome());
        application.setExistingMonthlyDebt(request.existingMonthlyDebt());
        application.setStatus(ApplicationStatus.DRAFT);

        BigDecimal dti = calculateDti(request.amount(), request.tenureMonths(), product.getInterestRate(),
                request.grossMonthlyIncome(), request.existingMonthlyDebt());
        application.setCalculatedDti(dti);

        LoanApplication saved = applicationRepository.save(application);

        auditLogRepository.save(new WorkflowAuditLog(
                saved,
                applicant,
                null,
                ApplicationStatus.DRAFT.name(),
                "DRAFT_CREATED",
                "Application draft initialized by applicant."
        ));

        return toResponse(saved);
    }

    @Transactional
    public LoanApplicationResponse updateApplication(Long id, User applicant, LoanApplicationUpdateRequest request) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

        if (!application.getApplicant().getId().equals(applicant.getId())) {
            throw new SecurityException("You do not have permission to update this application");
        }

        if (application.getStatus() != ApplicationStatus.DRAFT && application.getStatus() != ApplicationStatus.INFORMATION_REQUESTED) {
            throw new IllegalStateException("Cannot edit application in status: " + application.getStatus());
        }

        LoanProduct product = productService.getEntityById(request.productId());
        validateProductBounds(product, request.amount(), request.tenureMonths());

        application.setProduct(product);
        application.setAmount(request.amount());
        application.setTenureMonths(request.tenureMonths());
        application.setPurpose(request.purpose());
        application.setEmploymentType(request.employmentType());
        application.setEmployerName(request.employerName());
        application.setGrossMonthlyIncome(request.grossMonthlyIncome());
        application.setExistingMonthlyDebt(request.existingMonthlyDebt());

        BigDecimal dti = calculateDti(request.amount(), request.tenureMonths(), product.getInterestRate(),
                request.grossMonthlyIncome(), request.existingMonthlyDebt());
        application.setCalculatedDti(dti);

        LoanApplication saved = applicationRepository.save(application);

        auditLogRepository.save(new WorkflowAuditLog(
                saved,
                applicant,
                application.getStatus().name(),
                application.getStatus().name(),
                "APPLICATION_UPDATED",
                "Application fields revised by applicant."
        ));

        return toResponse(saved);
    }

    @Transactional
    public LoanApplicationResponse submitApplication(Long id, User applicant) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

        if (!application.getApplicant().getId().equals(applicant.getId())) {
            throw new SecurityException("You do not have permission to submit this application");
        }

        if (application.getStatus() != ApplicationStatus.DRAFT && application.getStatus() != ApplicationStatus.INFORMATION_REQUESTED) {
            throw new IllegalStateException("Application cannot be submitted in status: " + application.getStatus());
        }

        if (application.getGrossMonthlyIncome() == null || application.getGrossMonthlyIncome().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Gross monthly income must be provided prior to submission.");
        }

        ApplicationStatus previousStatus = application.getStatus();

        if (previousStatus == ApplicationStatus.DRAFT) {
            application.setStatus(ApplicationStatus.SUBMITTED);
            LoanApplication saved = applicationRepository.save(application);

            // Trigger EximeeBPMS process
            String processInstanceId = workflowService.startLoanOriginationWorkflow(saved.getId(), applicant.getEmail());
            saved.setProcessInstanceId(processInstanceId);

            auditLogRepository.save(new WorkflowAuditLog(
                    saved,
                    applicant,
                    previousStatus.name(),
                    ApplicationStatus.SUBMITTED.name(),
                    "SUBMITTED_FOR_REVIEW",
                    "Application submitted to EximeeBPMS process engine."
            ));

            return toResponse(applicationRepository.save(saved));
        } else {
            // Resubmission after information requested
            application.setStatus(ApplicationStatus.IN_REVIEW);
            LoanApplication saved = applicationRepository.save(application);

            if (saved.getProcessInstanceId() != null) {
                workflowService.completeApplicantRevision(saved.getProcessInstanceId());
            }

            auditLogRepository.save(new WorkflowAuditLog(
                    saved,
                    applicant,
                    previousStatus.name(),
                    ApplicationStatus.IN_REVIEW.name(),
                    "RESUBMITTED_BY_APPLICANT",
                    "Applicant provided requested revisions; returned to underwriting review."
            ));

            return toResponse(saved);
        }
    }

    @Transactional(readOnly = true)
    public LoanApplicationResponse getApplicationById(Long id, User currentUser) {
        LoanApplication application = applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + id));

        boolean isApplicant = application.getApplicant().getId().equals(currentUser.getId());
        boolean isStaff = currentUser.getRole() == Role.ROLE_OFFICER || currentUser.getRole() == Role.ROLE_ADMIN;

        if (!isApplicant && !isStaff) {
            throw new SecurityException("You do not have permission to view this application");
        }

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getMyApplications(User applicant) {
        return applicationRepository.findByApplicantIdOrderByCreatedAtDesc(applicant.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LoanApplicationResponse toResponse(LoanApplication app) {
        User applicant = app.getApplicant();
        UserDto applicantDto = new UserDto(
                applicant.getId(),
                applicant.getEmail(),
                applicant.getFullName(),
                applicant.getPhoneNumber(),
                applicant.getRole()
        );

        LoanProduct product = app.getProduct();
        LoanProductDto productDto = new LoanProductDto(
                product.getId(),
                product.getCode(),
                product.getName(),
                product.getDescription(),
                product.getMinAmount(),
                product.getMaxAmount(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getInterestRate(),
                product.getIsActive()
        );

        BigDecimal emi = calculateEmi(app.getAmount(), app.getTenureMonths(), product.getInterestRate());

        List<DocumentDto> docs = documentService.getDocumentsByApplication(app.getId());

        List<AuditLogDto> auditLogs = auditLogRepository.findByApplicationIdOrderByCreatedAtAsc(app.getId())
                .stream()
                .map(log -> new AuditLogDto(
                        log.getId(),
                        log.getActor() != null ? log.getActor().getFullName() : "System / EximeeBPMS",
                        log.getActor() != null ? log.getActor().getEmail() : "system@eximee.los",
                        log.getFromStatus(),
                        log.getToStatus(),
                        log.getActionType(),
                        log.getNotes(),
                        log.getCreatedAt()
                ))
                .toList();

        String decidedByName = app.getDecidedBy() != null ? app.getDecidedBy().getFullName() : null;

        return new LoanApplicationResponse(
                app.getId(),
                app.getApplicationNumber(),
                applicantDto,
                productDto,
                app.getAmount(),
                app.getTenureMonths(),
                app.getPurpose(),
                app.getEmploymentType(),
                app.getEmployerName(),
                app.getGrossMonthlyIncome(),
                app.getExistingMonthlyDebt(),
                app.getCalculatedDti(),
                emi,
                app.getStatus(),
                app.getProcessInstanceId(),
                app.getApprovedAmount(),
                app.getDecisionReason(),
                decidedByName,
                app.getDecidedAt(),
                app.getVersion(),
                docs,
                auditLogs,
                app.getCreatedAt(),
                app.getUpdatedAt()
        );
    }

    private void validateProductBounds(LoanProduct product, BigDecimal amount, Integer tenure) {
        if (amount.compareTo(product.getMinAmount()) < 0 || amount.compareTo(product.getMaxAmount()) > 0) {
            throw new IllegalArgumentException(String.format("Requested amount ($%s) must be between $%s and $%s",
                    amount, product.getMinAmount(), product.getMaxAmount()));
        }
        if (tenure < product.getMinTenureMonths() || tenure > product.getMaxTenureMonths()) {
            throw new IllegalArgumentException(String.format("Requested tenure (%s mos) must be between %s and %s months",
                    tenure, product.getMinTenureMonths(), product.getMaxTenureMonths()));
        }
    }

    private BigDecimal calculateEmi(BigDecimal principal, int tenureMonths, BigDecimal annualRate) {
        if (principal == null || tenureMonths <= 0) return BigDecimal.ZERO;
        if (annualRate == null || annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100 * 12), 8, RoundingMode.HALF_UP);
        double r = monthlyRate.doubleValue();
        double factor = Math.pow(1 + r, tenureMonths);
        double emiVal = principal.doubleValue() * r * factor / (factor - 1);
        return BigDecimal.valueOf(emiVal).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDti(BigDecimal principal, int tenure, BigDecimal rate, BigDecimal income, BigDecimal debt) {
        if (income == null || income.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.valueOf(100.00);
        }
        BigDecimal emi = calculateEmi(principal, tenure, rate);
        BigDecimal totalDebt = (debt != null ? debt : BigDecimal.ZERO).add(emi);
        return totalDebt.divide(income, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String generateApplicationNumber() {
        int currentYear = Year.now().getValue();
        int randomDigits = 1000 + new Random().nextInt(9000);
        return String.format("APP-%d-%04d", currentYear, randomDigits);
    }
}
