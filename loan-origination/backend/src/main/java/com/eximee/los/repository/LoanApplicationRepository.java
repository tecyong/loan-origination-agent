package com.eximee.los.repository;

import com.eximee.los.domain.ApplicationStatus;
import com.eximee.los.domain.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);
    Optional<LoanApplication> findByApplicationNumber(String applicationNumber);
    List<LoanApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status);
    List<LoanApplication> findAllByOrderByCreatedAtDesc();
    long countByStatus(ApplicationStatus status);
}
