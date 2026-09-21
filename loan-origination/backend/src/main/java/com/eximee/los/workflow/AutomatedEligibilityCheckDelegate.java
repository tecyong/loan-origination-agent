package com.eximee.los.workflow;

import com.eximee.los.domain.ApplicationStatus;
import com.eximee.los.domain.LoanApplication;
import com.eximee.los.domain.WorkflowAuditLog;
import com.eximee.los.repository.LoanApplicationRepository;
import com.eximee.los.repository.WorkflowAuditLogRepository;
import org.eximeebpms.bpm.engine.delegate.DelegateExecution;
import org.eximeebpms.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Component("automatedEligibilityCheckDelegate")
public class AutomatedEligibilityCheckDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(AutomatedEligibilityCheckDelegate.class);

    private final LoanApplicationRepository applicationRepository;
    private final WorkflowAuditLogRepository auditLogRepository;

    public AutomatedEligibilityCheckDelegate(
            LoanApplicationRepository applicationRepository,
            WorkflowAuditLogRepository auditLogRepository
    ) {
        this.applicationRepository = applicationRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void execute(DelegateExecution execution) throws Exception {
        Long applicationId = (Long) execution.getVariable("applicationId");
        log.info("Executing automated eligibility check for application ID: {}", applicationId);

        LoanApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        BigDecimal income = app.getGrossMonthlyIncome() != null ? app.getGrossMonthlyIncome() : BigDecimal.ZERO;
        BigDecimal debt = app.getExistingMonthlyDebt() != null ? app.getExistingMonthlyDebt() : BigDecimal.ZERO;

        // Calculate approximate monthly installment
        BigDecimal amount = app.getAmount();
        int tenure = app.getTenureMonths() != null && app.getTenureMonths() > 0 ? app.getTenureMonths() : 12;
        BigDecimal rate = app.getProduct().getInterestRate();
        BigDecimal monthlyRate = rate.divide(BigDecimal.valueOf(100 * 12), 8, RoundingMode.HALF_UP);
        
        BigDecimal emi;
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            emi = amount.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP);
        } else {
            // P * r * (1+r)^n / ((1+r)^n - 1)
            double r = monthlyRate.doubleValue();
            double factor = Math.pow(1 + r, tenure);
            double emiVal = amount.doubleValue() * r * factor / (factor - 1);
            emi = BigDecimal.valueOf(emiVal).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal totalMonthlyObligations = debt.add(emi);
        BigDecimal dti;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            dti = totalMonthlyObligations.divide(income, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            dti = BigDecimal.valueOf(100.00);
        }

        app.setCalculatedDti(dti);

        boolean criteriaMet = dti.compareTo(BigDecimal.valueOf(60.00)) <= 0;
        execution.setVariable("autoApproved", criteriaMet);
        execution.setVariable("calculatedDti", dti.doubleValue());

        if (criteriaMet) {
            app.setStatus(ApplicationStatus.IN_REVIEW);
            auditLogRepository.save(new WorkflowAuditLog(
                    app,
                    null,
                    ApplicationStatus.SUBMITTED.name(),
                    ApplicationStatus.IN_REVIEW.name(),
                    "AUTOMATED_CHECK_PASSED",
                    "Automated risk scoring passed. Calculated DTI: " + dti + "%. Assigned to Underwriting queue."
            ));
            log.info("Application {} passed criteria with DTI {}%. Moved to IN_REVIEW.", app.getApplicationNumber(), dti);
        } else {
            app.setStatus(ApplicationStatus.REJECTED);
            app.setDecisionReason("Debt-to-Income ratio (" + dti + "%) exceeds the maximum allowable threshold of 60.00%.");
            app.setDecidedAt(OffsetDateTime.now());
            auditLogRepository.save(new WorkflowAuditLog(
                    app,
                    null,
                    ApplicationStatus.SUBMITTED.name(),
                    ApplicationStatus.REJECTED.name(),
                    "AUTO_REJECTED",
                    "Automated rejection: DTI of " + dti + "% exceeds maximum 60% threshold."
            ));
            log.warn("Application {} auto-rejected with DTI {}%.", app.getApplicationNumber(), dti);
        }

        applicationRepository.save(app);
    }
}
