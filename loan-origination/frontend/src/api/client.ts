import axios from 'axios';
import type {
  AuthResponse,
  LoanApplication,
  LoanApplicationCreateRequest,
  LoanProduct,
  OfficerDecisionRequest,
  User,
  DocumentDto,
} from '../types';

const API_BASE_URL = 'http://localhost:8080/api/v1';

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('los_auth_token');
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Don't auto-redirect if on login or register
      const isAuthPath = window.location.pathname.includes('/login') || window.location.pathname.includes('/register');
      if (!isAuthPath) {
        localStorage.removeItem('los_auth_token');
        localStorage.removeItem('los_auth_user');
      }
    }
    return Promise.reject(error);
  }
);

export const api = {
  // Auth
  login: (data: { email: string; password: string }) =>
    apiClient.post<AuthResponse>('/auth/login', data).then((r) => r.data),
  register: (data: { email: string; password: string; fullName: string; phoneNumber?: string; role?: string }) =>
    apiClient.post<AuthResponse>('/auth/register', data).then((r) => r.data),
  getMe: () => apiClient.get<User>('/auth/me').then((r) => r.data),

  // Products
  getProducts: () => apiClient.get<LoanProduct[]>('/products').then((r) => r.data),
  getProduct: (id: number) => apiClient.get<LoanProduct>(`/products/${id}`).then((r) => r.data),

  // Applicant Applications
  createApplicationDraft: (data: LoanApplicationCreateRequest) =>
    apiClient.post<LoanApplication>('/applications', data).then((r) => r.data),
  getMyApplications: () =>
    apiClient.get<LoanApplication[]>('/applications/my').then((r) => r.data),
  getApplication: (id: number) =>
    apiClient.get<LoanApplication>(`/applications/${id}`).then((r) => r.data),
  updateApplication: (id: number, data: Partial<LoanApplicationCreateRequest>) =>
    apiClient.put<LoanApplication>(`/applications/${id}`, data).then((r) => r.data),
  submitApplication: (id: number) =>
    apiClient.post<LoanApplication>(`/applications/${id}/submit`).then((r) => r.data),

  // Documents
  uploadDocument: (applicationId: number, documentType: string, file: File) => {
    const formData = new FormData();
    formData.append('documentType', documentType);
    formData.append('file', file);
    return apiClient
      .post<DocumentDto>(`/applications/${applicationId}/documents`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      .then((r) => r.data);
  },
  getDocuments: (applicationId: number) =>
    apiClient.get<DocumentDto[]>(`/applications/${applicationId}/documents`).then((r) => r.data),
  getDocumentDownloadUrl: (docId: number) => `${API_BASE_URL}/documents/${docId}/download`,

  // Officer Underwriting
  getOfficerQueue: (status?: string) =>
    apiClient
      .get<LoanApplication[]>('/officer/applications', {
        params: status ? { status } : {},
      })
      .then((r) => r.data),
  getOfficerApplicationDetails: (id: number) =>
    apiClient.get<LoanApplication>(`/officer/applications/${id}`).then((r) => r.data),
  submitOfficerDecision: (id: number, decision: OfficerDecisionRequest) =>
    apiClient.post<LoanApplication>(`/officer/applications/${id}/decision`, decision).then((r) => r.data),
};
