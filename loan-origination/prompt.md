# Enterprise Loan Origination System (LOS) - Specification & Implementation Prompt

A comprehensive, production-grade requirements and technical specification for building a modern **Loan Origination System (LOS)**.

---

## 1. Project Overview & Objectives

The **Loan Origination System (LOS)** is an end-to-end digital lending solution designed to automate and streamline the processing of retail and commercial loan applications. The system coordinates the entire application lifecycle—from initial borrower intake, document upload, and automated financial scoring, to underwriter review, decisioning (approval/rejection), and status tracking.

### Core Objectives
- **Self-Service Borrower Portal**: Provide borrowers with an intuitive, multi-step loan application wizard, instant amortization calculations, document uploads, and real-time status tracking.
- **Workflow Automation with EximeeBPMS**: Orchestrate application state transitions and underwriting tasks using BPMN 2.0 compliant processes powered by EximeeBPMS.
- **Underwriter / Officer Operations Console**: Provide loan officers with an efficient review queue, automated debt-to-income (DTI) metrics, document inspection, and structured decisioning capabilities (Approve, Reject, Request Revisions).
- **Auditability & Compliance**: Maintain an immutable audit log of all status transitions, officer remarks, and applicant interactions.

---

## 2. Technology Stack & Infrastructure

### 2.1 Backend Core
- **Language & Runtime**: Java 25 (utilizing modern Java features: Records, Pattern Matching, Virtual Threads).
- **Framework**: Spring Boot 3.3+
  - `spring-boot-starter-web` (REST APIs)
  - `spring-boot-starter-data-jpa` (ORM & Data Access)
  - `spring-boot-starter-security` (Authentication & RBAC)
  - `spring-boot-starter-validation` (Jakarta Validation)
  - `spring-boot-starter-actuator` (Health & Telemetry)
- **Workflow & Process Engine**:
  - **EximeeBPMS Spring Boot Starter** (`org.eximeebpms.bpm:eximeebpms-spring-boot-starter`, version `1.4.0` or compatible)
  - Native BPMN 2.0 execution engine with Spring integration.
- **Security & Authentication**:
  - Stateless JWT (JSON Web Tokens) with HMAC-SHA256 / RSA.
  - BCrypt password hashing (work factor 12).
  - Role-Based Access Control (`ROLE_APPLICANT`, `ROLE_OFFICER`, `ROLE_ADMIN`).
- **Database Migrations**: Flyway or Liquibase for automated schema versioning.
- **API Documentation**: OpenAPI 3 / Swagger UI via `springdoc-openapi-starter-webmvc-ui`.
- **Build System**: Apache Maven 3.9+ with multi-module or clean standard architecture.

### 2.2 Database
- **Engine**: PostgreSQL 16+ / 18
- **Connection Configuration**:
  - **Host / Port**: `127.0.0.1:5432`
  - **JDBC URL**: `jdbc:postgresql://127.0.0.1:5432/postgres?currentSchema=los`
  - **Schema**: `los` (isolated schema for all application tables and EximeeBPMS engine tables)
  - **Username**: `postgres`
  - **Password**: `${DB_PASSWORD:}` (empty string fallback for local dev environments)
- **Engine Database Integration**: EximeeBPMS configured to share the primary `DataSource` under the `los` schema.

### 2.3 Frontend Application
- **Runtime & Bundler**: Node.js 20+ with Vite.
- **Framework**: React 18+ with TypeScript.
- **Routing**: React Router v6.
- **Styling & UI**: Modern, responsive design system (Vanilla CSS or Tailwind CSS) with dark/light theme support, glassmorphic cards, intuitive status badges, and responsive layouts.
- **Icons**: Lucide React.
- **State Management & Network**:
  - Axios or Fetch with global interceptors for JWT token attachment and 401 refresh/redirects.
  - React Context API or Zustand for auth and active application state.

---

## 3. User Roles & Permission Matrix

| Feature / Action | Applicant (`ROLE_APPLICANT`) | Loan Officer (`ROLE_OFFICER`) | Administrator (`ROLE_ADMIN`) |
| :--- | :---: | :---: | :---: |
| Self-Registration & Login | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| View Loan Products & Calculate Amortization | :white_check_mark: | :white_check_mark: | :white_check_mark: |
| Create Draft Application | :white_check_mark: | :x: | :x: |
| Edit Application (Draft / Revisions Requested) | :white_check_mark: | :x: | :x: |
| Upload Supporting Documents | :white_check_mark: | :x: | :x: |
| Submit Application for Review | :white_check_mark: | :x: | :x: |
| Track Personal Application Status & Timeline | :white_check_mark: | :x: | :x: |
| View Officer Underwriting Queue | :x: | :white_check_mark: | :white_check_mark: |
| Inspect Application 360 & Documents | :x: | :white_check_mark: | :white_check_mark: |
| Approve / Reject Application | :x: | :white_check_mark: | :white_check_mark: |
| Request Application Information/Revision | :x: | :white_check_mark: | :white_check_mark: |
| Manage Loan Products (Rates, Limits) | :x: | :x: | :white_check_mark: |
| User Management & Role Assignment | :x: | :x: | :white_check_mark: |
| View System Workflow SLA Metrics | :x: | :white_check_mark: | :white_check_mark: |

---

## 4. Business Process & EximeeBPMS Workflow Architecture

### 4.1 Process Diagram Overview (`loan-origination-process.bpmn`)
```
 [Start: Application Submitted]
               │
               ▼
   [Service Task: Automated Risk & Eligibility Check]
               │
      ┌────────┴──────────────────────────┐
      ▼ (DTI > 60% or Blacklisted)        ▼ (Passed Criteria)
 [Auto Reject Service Task]          [User Task: Underwriter Review]
      │                                   │
      │                             ┌─────┴───────────────┐
      │                             ▼ (Need Info)         ▼ (Decided)
      │                   [User Task: Revise Info]   [Exclusive Gateway]
      │                             │                 ├── Approve ──► [Service Task: Issue Approval Offer]
      │                             └─────► (Resubmit)└── Reject  ──► [Service Task: Process Rejection]
      ▼                                                                         │
 [End: Rejected] ◄──────────────────────────────────────────────────────────────┘
```

### 4.2 Application Lifecycle States
1. **`DRAFT`**: Application initialized by applicant. Editable at any time. Not yet visible in officer review queue.
2. **`SUBMITTED`**: Applicant finalizes details and documents, triggering the EximeeBPMS process instance. Application is locked against applicant edits.
3. **`IN_REVIEW`**: Automated eligibility checks passed; application is assigned to the `underwriting-officers` group task list.
4. **`INFORMATION_REQUESTED`**: Officer requires corrected documents or details. Application locks are temporarily lifted for the applicant to amend and re-submit.
5. **`APPROVED`**: Officer issues formal approval. Offer terms (final amount, rate, tenure, monthly installment) generated.
6. **`REJECTED`**: Application declined either automatically via policy rules or manually by officer with mandatory rejection rationale.
7. **`ACCEPTED`**: (Optional post-approval) Applicant accepts the offer terms.

---

## 5. Detailed Functional Requirements

### 5.1 Authentication & Profile
- **Registration**: Allows applicants to register with Email, Password, Full Name, and Phone Number.
- **Login & Tokens**: Authenticates credentials, returning JWT access token with user role and profile metadata.
- **Session Management**: Secure client storage, automatic expiration handling, and protected route wrappers.

### 5.2 Loan Product Catalog & Repayment Calculator
- **Product Parameters**:
  - Product Code (e.g., `PERSONAL_LOAN`, `MORTGAGE`, `AUTO_LOAN`).
  - Min / Max principal amount (e.g., $1,000 to $100,000).
  - Min / Max tenure in months (e.g., 6 to 84 months).
  - Standard Annual Percentage Rate (APR, e.g., 7.50%).
- **Interactive Calculator**: Computes Equated Monthly Installment (EMI), total interest payable, and amortization schedule table dynamically on the frontend using the formula:
  $$EMI = P \times r \times \frac{(1 + r)^n}{(1 + r)^n - 1}$$
  where $P$ is principal, $r$ is monthly interest rate, and $n$ is tenure in months.

### 5.3 Multi-Step Loan Application Wizard
The applicant creates an application via an intuitive step-by-step wizard:
- **Step 1: Loan Configuration**: Select loan product, requested amount, tenure, and purpose of loan.
- **Step 2: Personal Identification**: National ID / SSN, Date of Birth, Marital Status, Residential Address (Street, City, Postal Code), Housing Status (Own/Rent).
- **Step 3: Employment & Financial Information**: Employment Type (Employed, Self-Employed, Retired), Employer Name, Job Title, Years at Job, Gross Monthly Income, Other Monthly Income, Existing Monthly Debt Obligations (Rent, existing loans, credit cards).
- **Step 4: Supporting Documents**:
  - Government ID / Passport scan.
  - Proof of Income (recent payslip or tax return).
  - Recent Bank Statement (PDF, JPG, PNG up to 10MB).
- **Step 5: Review & Acknowledgement**: Consolidated summary of all entries, calculated Debt-to-Income (DTI) ratio, declaration check, and digital signature submission.

### 5.4 Application Tracking & Self-Service Management
- **Dashboard Summary**: Applicant sees active applications with progress badges, application reference number (e.g., `APP-2026-0001`), and submission date.
- **Visual Progress Timeline**: Step-by-step visual tracker highlighting the current phase:
  `[Draft] ➔ [Submitted] ➔ [Underwriter Review] ➔ [Decision]`.
- **Edit & Re-submission**:
  - When status is `DRAFT` or `INFORMATION_REQUESTED`, applicant can modify fields and replace uploaded documents.
  - Includes a banner highlighting officer comments when revisions are requested.

### 5.5 Loan Officer Operations Queue
- **Inbox & Filtering**:
  - Filter applications by status (`IN_REVIEW`, `INFORMATION_REQUESTED`, `APPROVED`, `REJECTED`), product type, or date range.
  - Search by applicant name, email, or application reference number.
- **Application 360 Degree View**:
  - Applicant personal profile and contact details.
  - Requested loan terms vs. product constraints.
  - Financial overview: Monthly Income, Total Debt, Calculated Debt-to-Income (DTI) ratio with color-coded risk flag:
    - **Low Risk**: DTI < 36% (Green)
    - **Moderate Risk**: 36% ≤ DTI ≤ 50% (Yellow)
    - **High Risk**: DTI > 50% (Red)
  - Embedded document viewer / downloader.
  - Full audit trail of past events and remarks.
- **Underwriting Action Modal**:
  - **Approve**: Input final approved amount (can adjust if needed), approved tenure, officer notes.
  - **Reject**: Select standard rejection reason (e.g., *Insufficient Income*, *High Debt Ratio*, *Incomplete Documentation*, *Policy Mismatch*) and provide detailed rationale.
  - **Request Information**: Specify which fields or documents require applicant clarification. Transitions status to `INFORMATION_REQUESTED`.

---

## 6. Data Model & Database Schema (`los`)

```sql
-- Schema initialization
CREATE SCHEMA IF NOT EXISTS los;

-- Users & Authentication
CREATE TABLE los.users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    role VARCHAR(50) NOT NULL, -- ROLE_APPLICANT, ROLE_OFFICER, ROLE_ADMIN
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Loan Products Catalog
CREATE TABLE los.loan_products (
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

-- Loan Applications
CREATE TABLE los.loan_applications (
    id BIGSERIAL PRIMARY KEY,
    application_number VARCHAR(64) NOT NULL UNIQUE,
    applicant_id BIGINT NOT NULL REFERENCES los.users(id),
    product_id BIGINT NOT NULL REFERENCES los.loan_products(id),
    amount NUMERIC(15,2) NOT NULL,
    tenure_months INT NOT NULL,
    purpose VARCHAR(255),
    
    -- Applicant Snapshot at submission
    employment_type VARCHAR(100),
    employer_name VARCHAR(255),
    gross_monthly_income NUMERIC(15,2),
    existing_monthly_debt NUMERIC(15,2),
    calculated_dti NUMERIC(5,2),
    
    -- Status & Workflow
    status VARCHAR(50) NOT NULL, -- DRAFT, SUBMITTED, IN_REVIEW, INFORMATION_REQUESTED, APPROVED, REJECTED, ACCEPTED
    process_instance_id VARCHAR(255), -- EximeeBPMS process execution ID
    
    -- Decision Metadata
    approved_amount NUMERIC(15,2),
    decision_reason TEXT,
    decided_by BIGINT REFERENCES los.users(id),
    decided_at TIMESTAMP WITH TIME ZONE,
    
    version INT DEFAULT 0, -- Optimistic locking
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Supporting Documents
CREATE TABLE los.application_documents (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES los.loan_applications(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL, -- ID_CARD, PAYSLIP, BANK_STATEMENT, OTHER
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Immutable Audit & Workflow Event History
CREATE TABLE los.workflow_audit_log (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES los.loan_applications(id) ON DELETE CASCADE,
    actor_id BIGINT REFERENCES los.users(id),
    from_status VARCHAR(50),
    to_status VARCHAR(50) NOT NULL,
    action_type VARCHAR(100) NOT NULL,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 7. RESTful API Specification

### 7.1 Authentication Endpoints (`/api/v1/auth`)
- `POST /register`: Register a new user (default `ROLE_APPLICANT`).
- `POST /login`: Authenticate and receive JWT `{ token, user: { id, email, fullName, role } }`.
- `GET /me`: Retrieve current logged-in user profile.

### 7.2 Product Endpoints (`/api/v1/products`)
- `GET /`: List all active loan products with rates and ranges (public/authenticated).
- `GET /{id}`: Retrieve detailed terms for a specific product.
- `POST /`: Create or update product (Admin only).

### 7.3 Applicant Endpoints (`/api/v1/applications`)
- `POST /`: Create a new application draft.
- `GET /my`: List all applications submitted by the logged-in applicant.
- `GET /{id}`: Retrieve comprehensive details, status, and document list for an owned application.
- `PUT /{id}`: Update application fields (only permitted if status is `DRAFT` or `INFORMATION_REQUESTED`).
- `POST /{id}/submit`: Validate all required fields, transition status to `SUBMITTED`, and start the EximeeBPMS process.
- `POST /{id}/documents`: Upload a supporting document (multipart/form-data).
- `GET /{id}/documents/{docId}`: Download / preview an uploaded document.

### 7.4 Underwriter / Officer Endpoints (`/api/v1/officer/applications`)
- `GET /`: Query queue with pagination, status filter, and keyword search.
- `GET /{id}`: Retrieve full underwriting case details (application, documents, audit logs).
- `POST /{id}/approve`: Issue approval decision `{ approvedAmount, notes }`. Completes EximeeBPMS review task.
- `POST /{id}/reject`: Issue rejection decision `{ reason, notes }`. Completes EximeeBPMS review task.
- `POST /{id}/request-info`: Return application for revisions `{ revisionNotes }`. Completes EximeeBPMS task and updates state.

---

## 8. Non-Functional Requirements (NFR)

1. **Transaction Integrity & Idempotency**:
   - All state transitions and EximeeBPMS task completions must execute within a transactional boundary (`@Transactional`).
   - Optimistic locking via JPA `@Version` on `loan_applications` prevents concurrent modification conflicts between officers.
2. **Security**:
   - Applicant endpoints must enforce ownership validation (users can only access applications where `applicant_id == principal.id`).
   - Role-based method security via `@PreAuthorize("hasRole('OFFICER')")` on review endpoints.
   - File upload validation restricts MIME types to `application/pdf`, `image/jpeg`, `image/png`, with maximum size limit of 10MB.
3. **Auditability**:
   - Any status alteration must emit an event to `los.workflow_audit_log` capturing timestamp, actor ID, old state, new state, and comments.
4. **Resilience & Zero-Config Local Setup**:
   - Seed data runner automatically creates sample loan products (`Personal Loan`, `Home Mortgage`, `Auto Loan`), test users (`applicant@demo.com`, `officer@demo.com`, `admin@demo.com` with password `password123`), and sample applications for immediate testing.

---

## 9. Recommended Step-by-Step Implementation Roadmap

1. **Module & Infrastructure Setup**:
   - Set up Maven project structure with Spring Boot 3.3+, Java 25, PostgreSQL driver, and EximeeBPMS starter.
   - Configure `application.yml` targeting `postgresql://127.0.0.1:5432/postgres?currentSchema=los`.
2. **Data Layer & Entities**:
   - Create schema migration scripts / JPA entities (`User`, `LoanProduct`, `LoanApplication`, `ApplicationDocument`, `WorkflowAuditLog`).
3. **BPMN 2.0 Process Definition**:
   - Define `loan-origination-process.bpmn` with service tasks for automated scoring and user tasks for officer review.
4. **Service & Controller Layer**:
   - Implement Auth Service with JWT filter and Spring Security.
   - Implement Loan Application Service linked with Eximee process engine execution.
   - Implement Officer Underwriting Service.
5. **Frontend Application (React + Vite)**:
   - Scaffold Vite React TypeScript project.
   - Implement global auth state and login/registration views.
   - Build Applicant Portal (Dashboard, Loan Calculator, Multi-Step Wizard, Real-time Status Tracker).
   - Build Officer Portal (Underwriting Inbox, Case Review 360, Document Viewer, Action Modals).
6. **End-to-End Verification**:
   - Verify complete applicant journey from creation to submission.
   - Verify automated status transition and task assignment in EximeeBPMS.
   - Verify officer review, revision loop, and final approval/rejection outcomes.
