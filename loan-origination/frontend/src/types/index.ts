export type Role = 'ROLE_APPLICANT' | 'ROLE_OFFICER' | 'ROLE_ADMIN';

export type ApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'IN_REVIEW'
  | 'INFORMATION_REQUESTED'
  | 'APPROVED'
  | 'REJECTED'
  | 'ACCEPTED';

export type DocumentType = 'ID_CARD' | 'PAYSLIP' | 'BANK_STATEMENT' | 'OTHER';

export interface User {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoanProduct {
  id: number;
  code: string;
  name: string;
  description: string;
  minAmount: number;
  maxAmount: number;
  minTenureMonths: number;
  maxTenureMonths: number;
  interestRate: number;
  isActive: boolean;
}

export interface DocumentDto {
  id: number;
  documentType: DocumentType;
  fileName: string;
  fileSize: number;
  contentType: string;
  uploadedAt: string;
}

export interface AuditLogDto {
  id: number;
  actorName: string;
  actorEmail: string;
  fromStatus: string;
  toStatus: string;
  actionType: string;
  notes: string;
  createdAt: string;
}

export interface LoanApplication {
  id: number;
  applicationNumber: string;
  applicant: User;
  product: LoanProduct;
  amount: number;
  tenureMonths: number;
  purpose?: string;
  employmentType?: string;
  employerName?: string;
  grossMonthlyIncome?: number;
  existingMonthlyDebt?: number;
  calculatedDti?: number;
  estimatedMonthlyPayment?: number;
  status: ApplicationStatus;
  processInstanceId?: string;
  approvedAmount?: number;
  decisionReason?: string;
  decidedByName?: string;
  decidedAt?: string;
  version: number;
  documents: DocumentDto[];
  auditLogs: AuditLogDto[];
  createdAt: string;
  updatedAt: string;
}

export interface LoanApplicationCreateRequest {
  productId: number;
  amount: number;
  tenureMonths: number;
  purpose?: string;
  employmentType?: string;
  employerName?: string;
  grossMonthlyIncome: number;
  existingMonthlyDebt: number;
}

export interface OfficerDecisionRequest {
  action: 'APPROVE' | 'REJECT' | 'REQUEST_INFO';
  approvedAmount?: number;
  decisionReason?: string;
  revisionNotes?: string;
}
