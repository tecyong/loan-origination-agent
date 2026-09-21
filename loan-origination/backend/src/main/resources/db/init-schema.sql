-- ==========================================================
-- Enterprise Loan Origination System (LOS) - Schema Definition
-- Target Schema: los
-- Database Engine: PostgreSQL 16+
-- ==========================================================

CREATE SCHEMA IF NOT EXISTS los;

-- 1. Users & Authentication
CREATE TABLE IF NOT EXISTS los.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    role VARCHAR(50) NOT NULL, -- ROLE_APPLICANT, ROLE_OFFICER, ROLE_ADMIN
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Loan Products Catalog
CREATE TABLE IF NOT EXISTS los.loan_products (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    min_amount NUMERIC(15,2) NOT NULL,
    max_amount NUMERIC(15,2) NOT NULL,
    min_tenure_months INT NOT NULL,
    max_tenure_months INT NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Loan Applications
CREATE TABLE IF NOT EXISTS los.loan_applications (
    id BIGSERIAL PRIMARY KEY,
    application_number VARCHAR(64) NOT NULL UNIQUE,
    applicant_id BIGINT NOT NULL REFERENCES los.users(id),
    product_id BIGINT NOT NULL REFERENCES los.loan_products(id),
    amount NUMERIC(15,2) NOT NULL,
    tenure_months INT NOT NULL,
    purpose VARCHAR(255),
    
    -- Applicant Financial Profile Snapshot
    employment_type VARCHAR(100),
    employer_name VARCHAR(255),
    gross_monthly_income NUMERIC(15,2),
    existing_monthly_debt NUMERIC(15,2),
    calculated_dti NUMERIC(5,2),
    
    -- State & Workflow
    status VARCHAR(50) NOT NULL, -- DRAFT, SUBMITTED, IN_REVIEW, INFORMATION_REQUESTED, APPROVED, REJECTED, ACCEPTED
    process_instance_id VARCHAR(255),
    
    -- Underwriter Decisions
    approved_amount NUMERIC(15,2),
    decision_reason TEXT,
    decided_by BIGINT REFERENCES los.users(id),
    decided_at TIMESTAMP WITH TIME ZONE,
    
    version INT DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Application Supporting Documents
CREATE TABLE IF NOT EXISTS los.application_documents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES los.loan_applications(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- ID_CARD, PAYSLIP, BANK_STATEMENT, OTHER
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Workflow & State Transition Audit Log
CREATE TABLE IF NOT EXISTS los.workflow_audit_log (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES los.loan_applications(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES los.users(id),
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for high-performance querying
CREATE INDEX IF NOT EXISTS idx_loan_apps_applicant ON los.loan_applications(applicant_id);
CREATE INDEX IF NOT EXISTS idx_loan_apps_status ON los.loan_applications(status);
CREATE INDEX IF NOT EXISTS idx_loan_apps_number ON los.loan_applications(application_number);
CREATE INDEX IF NOT EXISTS idx_audit_application ON los.workflow_audit_log(application_id);

-- Seed Data: Loan Products
INSERT INTO los.loan_products (code, name, description, min_amount, max_amount, min_tenure_months, max_tenure_months, interest_rate, is_active)
VALUES
('PERSONAL_FLEXI', 'Personal Flexi Loan', 'Unsecured personal loan for flexible individual financing needs with fixed monthly installments.', 1000.00, 50000.00, 6, 60, 8.50, TRUE),
('AUTO_DIRECT', 'Direct Auto Finance', 'Competitive auto financing for new and pre-owned vehicles with rapid pre-qualification.', 5000.00, 100000.00, 12, 84, 5.90, TRUE),
('HOME_EQUITY', 'Home Prime Mortgage', 'Fixed-rate residential mortgage and home equity financing with favorable extended tenures.', 25000.00, 1000000.00, 60, 360, 4.25, TRUE)
ON CONFLICT (code) DO NOTHING;
