package com.eximee.los.service;

import com.eximee.los.domain.ApplicationStatus;
import com.eximee.los.domain.LoanApplication;
import com.eximee.los.domain.LoanProduct;
import com.eximee.los.domain.Role;
import com.eximee.los.domain.User;
import com.eximee.los.dto.LoanApplicationCreateRequest;
import com.eximee.los.dto.LoanApplicationResponse;
import com.eximee.los.repository.LoanApplicationRepository;
import com.eximee.los.repository.WorkflowAuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanApplicationServiceTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private LoanProductService productService;

    @Mock
    private WorkflowOrchestrationService workflowService;

    @Mock
    private DocumentStorageService documentService;

    @Mock
    private WorkflowAuditLogRepository auditLogRepository;

    @InjectMocks
    private LoanApplicationService applicationService;

    private User applicant;
    private LoanProduct product;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        applicant = new User("applicant@demo.com", "hash", "Alex Morgan", "+123456", Role.ROLE_APPLICANT);
        applicant.setId(10L);

        product = new LoanProduct();
        product.setId(1L);
        product.setCode("PERSONAL_FLEXI");
        product.setName("Personal Flexi Loan");
        product.setMinAmount(BigDecimal.valueOf(1000));
        product.setMaxAmount(BigDecimal.valueOf(50000));
        product.setMinTenureMonths(6);
        product.setMaxTenureMonths(60);
        product.setInterestRate(BigDecimal.valueOf(8.5));
        product.setIsActive(true);
    }

    @Test
    void testCreateDraftSuccess() {
        LoanApplicationCreateRequest req = new LoanApplicationCreateRequest(
                1L,
                BigDecimal.valueOf(10000),
                24,
                "Home Renovation",
                "Employed",
                "Acme Corp",
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(500)
        );

        when(productService.getEntityById(1L)).thenReturn(product);
        when(applicationRepository.save(any(LoanApplication.class))).thenAnswer(i -> {
            LoanApplication app = i.getArgument(0);
            app.setId(100L);
            return app;
        });
        when(documentService.getDocumentsByApplication(any())).thenReturn(Collections.emptyList());
        when(auditLogRepository.findByApplicationIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        LoanApplicationResponse res = applicationService.createDraft(applicant, req);

        assertNotNull(res);
        assertEquals(ApplicationStatus.DRAFT, res.status());
        assertEquals(BigDecimal.valueOf(10000), res.amount());
        assertNotNull(res.calculatedDti());
        assertTrue(res.calculatedDti().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testCreateDraftAmountOutOfBoundsThrows() {
        LoanApplicationCreateRequest req = new LoanApplicationCreateRequest(
                1L,
                BigDecimal.valueOf(80000), // Max is 50000
                24,
                "Car",
                "Employed",
                "Acme Corp",
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(500)
        );

        when(productService.getEntityById(1L)).thenReturn(product);

        assertThrows(IllegalArgumentException.class, () -> applicationService.createDraft(applicant, req));
    }

    @Test
    void testSubmitDraftTriggersWorkflow() {
        LoanApplication app = new LoanApplication();
        app.setId(100L);
        app.setApplicationNumber("APP-2026-0001");
        app.setApplicant(applicant);
        app.setProduct(product);
        app.setAmount(BigDecimal.valueOf(10000));
        app.setTenureMonths(24);
        app.setGrossMonthlyIncome(BigDecimal.valueOf(6000));
        app.setExistingMonthlyDebt(BigDecimal.valueOf(400));
        app.setStatus(ApplicationStatus.DRAFT);

        when(applicationRepository.findById(100L)).thenReturn(Optional.of(app));
        when(workflowService.startLoanOriginationWorkflow(eq(100L), eq("applicant@demo.com"))).thenReturn("proc-inst-123");
        when(applicationRepository.save(any(LoanApplication.class))).thenAnswer(i -> i.getArgument(0));
        when(documentService.getDocumentsByApplication(any())).thenReturn(Collections.emptyList());
        when(auditLogRepository.findByApplicationIdOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        LoanApplicationResponse res = applicationService.submitApplication(100L, applicant);

        assertNotNull(res);
        assertEquals(ApplicationStatus.SUBMITTED, res.status());
        assertEquals("proc-inst-123", res.processInstanceId());
        verify(workflowService, times(1)).startLoanOriginationWorkflow(100L, "applicant@demo.com");
    }
}
