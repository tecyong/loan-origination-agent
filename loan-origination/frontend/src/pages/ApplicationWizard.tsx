import React, { useState, useEffect, useMemo } from 'react';
import { api } from '../api/client';
import type { LoanProduct } from '../types';
import {
  ArrowLeft,
  ArrowRight,
  Send,
  Upload,
  CheckCircle2,
  AlertCircle,
  FileCheck,
  ShieldAlert,
} from 'lucide-react';

interface ApplicationWizardProps {
  initialProductId?: number;
  initialAmount?: number;
  initialTenure?: number;
  onNavigate: (view: string, params?: { applicationId?: number }) => void;
}

export const ApplicationWizard: React.FC<ApplicationWizardProps> = ({
  initialProductId,
  initialAmount,
  initialTenure,
  onNavigate,
}) => {
  const [currentStep, setCurrentStep] = useState(1);
  const [products, setProducts] = useState<LoanProduct[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form Fields
  const [productId, setProductId] = useState<number>(initialProductId || 1);
  const [amount, setAmount] = useState<number>(initialAmount || 10000);
  const [tenureMonths, setTenureMonths] = useState<number>(initialTenure || 24);
  const [purpose, setPurpose] = useState('Personal Expense / Debt Consolidation');
  const [employmentType, setEmploymentType] = useState('Full-Time Salaried');
  const [employerName, setEmployerName] = useState('Global Tech Inc.');
  const [grossMonthlyIncome, setGrossMonthlyIncome] = useState<number>(6500);
  const [existingMonthlyDebt, setExistingMonthlyDebt] = useState<number>(600);

  // Files
  const [createdApplicationId, setCreatedApplicationId] = useState<number | null>(null);
  const [uploadedDocs, setUploadedDocs] = useState<{ [key: string]: boolean }>({});
  const [uploadingDoc, setUploadingDoc] = useState<string | null>(null);

  useEffect(() => {
    api.getProducts().then((res) => {
      setProducts(res);
      if (res.length > 0 && !initialProductId) {
        setProductId(res[0].id);
        setAmount(res[0].minAmount * 2);
        setTenureMonths(res[0].minTenureMonths + 12);
      }
    });
  }, [initialProductId]);

  const selectedProduct = useMemo(() => {
    return products.find((p) => p.id === productId) || products[0];
  }, [products, productId]);

  // Live DTI calculation preview
  const estimatedDti = useMemo(() => {
    if (!selectedProduct || grossMonthlyIncome <= 0) return 0;
    const r = selectedProduct.interestRate / (12 * 100);
    const n = tenureMonths;
    const factor = Math.pow(1 + r, n);
    const emi = (amount * r * factor) / (factor - 1);
    const totalDebt = existingMonthlyDebt + emi;
    return Math.round((totalDebt / grossMonthlyIncome) * 100 * 10) / 10;
  }, [selectedProduct, amount, tenureMonths, grossMonthlyIncome, existingMonthlyDebt]);

  // Step Navigation
  const handleNext = async () => {
    setError(null);

    if (currentStep === 1) {
      if (!selectedProduct) return;
      if (amount < selectedProduct.minAmount || amount > selectedProduct.maxAmount) {
        setError(`Amount must be between $${selectedProduct.minAmount.toLocaleString()} and $${selectedProduct.maxAmount.toLocaleString()}`);
        return;
      }
      if (tenureMonths < selectedProduct.minTenureMonths || tenureMonths > selectedProduct.maxTenureMonths) {
        setError(`Tenure must be between ${selectedProduct.minTenureMonths} and ${selectedProduct.maxTenureMonths} months`);
        return;
      }
    }

    if (currentStep === 3) {
      // Create draft application before document upload step
      setLoading(true);
      try {
        if (!createdApplicationId) {
          const draft = await api.createApplicationDraft({
            productId,
            amount,
            tenureMonths,
            purpose,
            employmentType,
            employerName,
            grossMonthlyIncome,
            existingMonthlyDebt,
          });
          setCreatedApplicationId(draft.id);
        } else {
          await api.updateApplication(createdApplicationId, {
            productId,
            amount,
            tenureMonths,
            purpose,
            employmentType,
            employerName,
            grossMonthlyIncome,
            existingMonthlyDebt,
          });
        }
      } catch (err: any) {
        setError(err.response?.data?.message || 'Error saving application draft');
        setLoading(false);
        return;
      } finally {
        setLoading(false);
      }
    }

    setCurrentStep((prev) => Math.min(prev + 1, 5));
  };

  const handlePrev = () => {
    setError(null);
    setCurrentStep((prev) => Math.max(prev - 1, 1));
  };

  const handleFileUpload = async (type: string, file: File) => {
    if (!createdApplicationId) {
      setError('Please proceed from Step 3 first to initialize application record');
      return;
    }
    setUploadingDoc(type);
    setError(null);
    try {
      await api.uploadDocument(createdApplicationId, type, file);
      setUploadedDocs((prev) => ({ ...prev, [type]: true }));
    } catch (err: any) {
      setError('File upload failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setUploadingDoc(null);
    }
  };

  const handleSubmitApplication = async () => {
    if (!createdApplicationId) return;
    setLoading(true);
    setError(null);
    try {
      const res = await api.submitApplication(createdApplicationId);
      onNavigate('application-detail', { applicationId: res.id });
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to submit application to EximeeBPMS process');
    } finally {
      setLoading(false);
    }
  };

  const wizardSteps = [
    '1. Loan Terms',
    '2. Personal & Employment',
    '3. Financial Profile',
    '4. Document Uploads',
    '5. Review & Submit',
  ];

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <button
        className="btn btn-secondary btn-sm"
        style={{ marginBottom: '1.5rem', background: 'transparent', border: 'none' }}
        onClick={() => onNavigate('applicant-dashboard')}
      >
        <ArrowLeft size={16} /> Back to Dashboard
      </button>

      {/* Stepper Progress */}
      <div className="stepper" style={{ marginBottom: '2rem' }}>
        {wizardSteps.map((title, idx) => {
          const stepNum = idx + 1;
          const isActive = currentStep === stepNum;
          const isCompleted = currentStep > stepNum;
          return (
            <div
              key={title}
              className={`step-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}
            >
              <div className="step-node">
                {isCompleted ? <CheckCircle2 size={18} /> : stepNum}
              </div>
              <div className="step-label" style={{ fontSize: '0.75rem' }}>{title}</div>
            </div>
          );
        })}
      </div>

      {error && (
        <div
          style={{
            padding: '1rem',
            background: 'var(--danger-bg)',
            border: '1px solid rgba(239, 68, 68, 0.3)',
            borderRadius: 'var(--radius-md)',
            color: '#F87171',
            marginBottom: '1.5rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
          }}
        >
          <AlertCircle size={18} /> {error}
        </div>
      )}

      <div className="card">
        {/* Step 1: Loan Terms */}
        {currentStep === 1 && (
          <div>
            <h2 className="card-title">Step 1: Choose Your Loan Configuration</h2>
            <p className="card-subtitle">
              Select the financing product, requested loan principal, and preferred repayment tenure.
            </p>

            <div className="form-group">
              <label className="form-label">Loan Product</label>
              <select
                className="form-select"
                value={productId}
                onChange={(e) => {
                  const id = Number(e.target.value);
                  setProductId(id);
                  const p = products.find((prod) => prod.id === id);
                  if (p) {
                    setAmount(Math.min(Math.max(amount, p.minAmount), p.maxAmount));
                    setTenureMonths(Math.min(Math.max(tenureMonths, p.minTenureMonths), p.maxTenureMonths));
                  }
                }}
              >
                {products.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name} ({p.interestRate}% APR)
                  </option>
                ))}
              </select>
            </div>

            <div className="grid-cols-2">
              <div className="form-group">
                <label className="form-label">
                  Loan Amount ($) [Min: ${selectedProduct?.minAmount?.toLocaleString()} - Max: ${selectedProduct?.maxAmount?.toLocaleString()}]
                </label>
                <input
                  type="number"
                  className="form-input"
                  value={amount}
                  onChange={(e) => setAmount(Number(e.target.value))}
                  step={500}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">
                  Tenure in Months [Min: {selectedProduct?.minTenureMonths} - Max: {selectedProduct?.maxTenureMonths}]
                </label>
                <input
                  type="number"
                  className="form-input"
                  value={tenureMonths}
                  onChange={(e) => setTenureMonths(Number(e.target.value))}
                  step={selectedProduct?.code === 'HOME_EQUITY' ? 12 : 6}
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label">Purpose of Loan</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Home Renovation, Vehicle Purchase, Debt Refinancing"
                value={purpose}
                onChange={(e) => setPurpose(e.target.value)}
                required
              />
            </div>
          </div>
        )}

        {/* Step 2: Personal & Employment */}
        {currentStep === 2 && (
          <div>
            <h2 className="card-title">Step 2: Personal & Employment Details</h2>
            <p className="card-subtitle">
              Provide information regarding your current employment status and workplace.
            </p>

            <div className="form-group">
              <label className="form-label">Employment Status</label>
              <select
                className="form-select"
                value={employmentType}
                onChange={(e) => setEmploymentType(e.target.value)}
              >
                <option value="Full-Time Salaried">Full-Time Salaried</option>
                <option value="Self-Employed / Business Owner">Self-Employed / Business Owner</option>
                <option value="Contract / Freelance">Contract / Freelance</option>
                <option value="Retired">Retired</option>
              </select>
            </div>

            <div className="form-group">
              <label className="form-label">Employer / Business Name</label>
              <input
                type="text"
                className="form-input"
                placeholder="e.g. Acme Corporation"
                value={employerName}
                onChange={(e) => setEmployerName(e.target.value)}
                required
              />
            </div>
          </div>
        )}

        {/* Step 3: Financial Profile */}
        {currentStep === 3 && (
          <div>
            <h2 className="card-title">Step 3: Income & Monthly Debt Obligations</h2>
            <p className="card-subtitle">
              Our automated EximeeBPMS engine uses this data to compute your Debt-to-Income (DTI) ratio.
            </p>

            <div className="grid-cols-2">
              <div className="form-group">
                <label className="form-label">Gross Monthly Income ($)</label>
                <input
                  type="number"
                  className="form-input"
                  value={grossMonthlyIncome}
                  onChange={(e) => setGrossMonthlyIncome(Number(e.target.value))}
                  min={1}
                  required
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Total before taxes (salary, commissions, investments).
                </span>
              </div>

              <div className="form-group">
                <label className="form-label">Existing Monthly Debt Obligations ($)</label>
                <input
                  type="number"
                  className="form-input"
                  value={existingMonthlyDebt}
                  onChange={(e) => setExistingMonthlyDebt(Number(e.target.value))}
                  min={0}
                  required
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Rent/mortgage, existing credit card minimums, auto loans.
                </span>
              </div>
            </div>

            {/* DTI Live Gauge Indicator */}
            <div
              style={{
                marginTop: '1.5rem',
                padding: '1.25rem',
                background: 'rgba(31, 41, 55, 0.4)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-subtle)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontWeight: 600, fontSize: '0.9rem' }}>Estimated Debt-to-Income (DTI) Ratio</span>
                <span
                  className={`risk-pill ${estimatedDti < 36 ? 'risk-low' : estimatedDti <= 50 ? 'risk-medium' : 'risk-high'}`}
                >
                  {estimatedDti}% — {estimatedDti < 36 ? 'Low Risk' : estimatedDti <= 50 ? 'Moderate Risk' : 'High Risk (>50%)'}
                </span>
              </div>

              {estimatedDti > 60 && (
                <div style={{ color: '#F87171', fontSize: '0.8rem', display: 'flex', alignItems: 'center', gap: '0.4rem', marginTop: '0.5rem' }}>
                  <ShieldAlert size={15} /> Note: Applications with DTI exceeding 60% are flagged for auto-rejection by automated policy rules.
                </div>
              )}
            </div>
          </div>
        )}

        {/* Step 4: Document Uploads */}
        {currentStep === 4 && (
          <div>
            <h2 className="card-title">Step 4: Upload Supporting Documents</h2>
            <p className="card-subtitle">
              Upload official verification documents (PDF, JPG, PNG up to 10MB).
            </p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              {[
                { type: 'ID_CARD', label: 'Government Photo ID (Passport or Driver License)', required: true },
                { type: 'PAYSLIP', label: 'Recent Pay Slip or Proof of Earnings', required: true },
                { type: 'BANK_STATEMENT', label: 'Recent Bank Statement (Last 3 Months)', required: false },
              ].map((doc) => {
                const isUploaded = uploadedDocs[doc.type];
                const isUploading = uploadingDoc === doc.type;
                return (
                  <div
                    key={doc.type}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '1rem 1.25rem',
                      background: 'rgba(255, 255, 255, 0.03)',
                      border: '1px solid var(--border-subtle)',
                      borderRadius: 'var(--radius-md)',
                    }}
                  >
                    <div>
                      <div style={{ fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                        <FileCheck size={16} color={isUploaded ? '#10B981' : '#9CA3AF'} />
                        {doc.label}
                      </div>
                      <div style={{ fontSize: '0.75rem', color: isUploaded ? '#34D399' : 'var(--text-muted)' }}>
                        {isUploaded ? 'Document uploaded successfully' : doc.required ? 'Mandatory' : 'Optional'}
                      </div>
                    </div>

                    <label className="btn btn-secondary btn-sm" style={{ cursor: isUploading ? 'wait' : 'pointer' }}>
                      <Upload size={14} />
                      {isUploading ? 'Uploading...' : isUploaded ? 'Replace File' : 'Upload'}
                      <input
                        type="file"
                        style={{ display: 'none' }}
                        onChange={(e) => {
                          const file = e.target.files?.[0];
                          if (file) handleFileUpload(doc.type, file);
                        }}
                        disabled={isUploading}
                      />
                    </label>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* Step 5: Review & Submit */}
        {currentStep === 5 && (
          <div>
            <h2 className="card-title">Step 5: Review & Authorize Submission</h2>
            <p className="card-subtitle">
              Verify your application details before sending to EximeeBPMS process automation.
            </p>

            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(2, 1fr)',
                gap: '1rem',
                padding: '1.25rem',
                background: 'rgba(255, 255, 255, 0.02)',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border-subtle)',
                marginBottom: '1.5rem',
              }}
            >
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Selected Product:</span>
                <div style={{ fontWeight: 600 }}>{selectedProduct?.name} ({selectedProduct?.interestRate}% APR)</div>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Requested Principal:</span>
                <div style={{ fontWeight: 700, color: '#38BDF8', fontFamily: 'var(--font-mono)' }}>
                  ${amount.toLocaleString()}
                </div>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Tenure:</span>
                <div style={{ fontWeight: 600 }}>{tenureMonths} Months</div>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Purpose:</span>
                <div style={{ fontWeight: 600 }}>{purpose}</div>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Employer / Occupation:</span>
                <div style={{ fontWeight: 600 }}>{employerName} ({employmentType})</div>
              </div>
              <div>
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Calculated DTI:</span>
                <div style={{ fontWeight: 700, color: estimatedDti <= 50 ? '#34D399' : '#F87171', fontFamily: 'var(--font-mono)' }}>
                  {estimatedDti}%
                </div>
              </div>
            </div>

            <div
              style={{
                padding: '1rem',
                background: 'rgba(2, 132, 199, 0.08)',
                border: '1px solid rgba(2, 132, 199, 0.2)',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.85rem',
                color: 'var(--text-secondary)',
                marginBottom: '1.5rem',
              }}
            >
              By submitting this application, you authorize Eximee LOS to trigger automated credit scoring and underwriter review procedures in compliance with lending regulatory standards.
            </div>
          </div>
        )}

        {/* Wizard Footer Controls */}
        <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: '2rem', paddingTop: '1.5rem', borderTop: '1px solid var(--border-subtle)' }}>
          {currentStep > 1 ? (
            <button className="btn btn-secondary" onClick={handlePrev} disabled={loading}>
              <ArrowLeft size={16} /> Back
            </button>
          ) : (
            <div />
          )}

          {currentStep < 5 ? (
            <button className="btn btn-primary" onClick={handleNext} disabled={loading}>
              Continue <ArrowRight size={16} />
            </button>
          ) : (
            <button
              className="btn btn-success"
              onClick={handleSubmitApplication}
              disabled={loading}
              style={{ padding: '0.75rem 2rem' }}
            >
              <Send size={16} /> {loading ? 'Submitting Application...' : 'Submit Application'}
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
