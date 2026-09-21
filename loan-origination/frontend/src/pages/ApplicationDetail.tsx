import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import type { LoanApplication } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { WorkflowStepper } from '../components/WorkflowStepper';
import {
  ArrowLeft,
  Download,
  AlertTriangle,
  Send,
  Upload,
  History,
} from 'lucide-react';

interface ApplicationDetailProps {
  applicationId: number;
  onNavigate: (view: string) => void;
}

export const ApplicationDetail: React.FC<ApplicationDetailProps> = ({
  applicationId,
  onNavigate,
}) => {
  const [app, setApp] = useState<LoanApplication | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Revision Form State
  const [isEditing, setIsEditing] = useState(false);
  const [editIncome, setEditIncome] = useState<number>(0);
  const [editDebt, setEditDebt] = useState<number>(0);
  const [editAmount, setEditAmount] = useState<number>(0);
  const [uploadingDoc, setUploadingDoc] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    loadDetails();
  }, [applicationId]);

  const loadDetails = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getApplication(applicationId);
      setApp(data);
      setEditIncome(data.grossMonthlyIncome || 0);
      setEditDebt(data.existingMonthlyDebt || 0);
      setEditAmount(data.amount || 0);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load application details');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveAndResubmit = async () => {
    if (!app) return;
    setSubmitting(true);
    setError(null);
    try {
      await api.updateApplication(app.id, {
        productId: app.product.id,
        amount: editAmount,
        tenureMonths: app.tenureMonths,
        grossMonthlyIncome: editIncome,
        existingMonthlyDebt: editDebt,
        purpose: app.purpose,
        employmentType: app.employmentType,
        employerName: app.employerName,
      });

      const res = await api.submitApplication(app.id);
      setApp(res);
      setIsEditing(false);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error resubmitting application');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUploadAdditionalDoc = async (type: string, file: File) => {
    if (!app) return;
    setUploadingDoc(true);
    try {
      await api.uploadDocument(app.id, type, file);
      await loadDetails();
    } catch (err: any) {
      setError('Upload failed: ' + (err.response?.data?.message || err.message));
    } finally {
      setUploadingDoc(false);
    }
  };

  if (loading) {
    return <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>Loading application details...</div>;
  }

  if (!app) {
    return (
      <div className="card" style={{ textAlign: 'center', padding: '3rem' }}>
        <p style={{ color: '#F87171', marginBottom: '1rem' }}>Application not found</p>
        <button className="btn btn-secondary" onClick={() => onNavigate('applicant-dashboard')}>
          <ArrowLeft size={16} /> Return to Dashboard
        </button>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '960px', margin: '0 auto' }}>
      <button
        className="btn btn-secondary btn-sm"
        style={{ marginBottom: '1.5rem', background: 'transparent', border: 'none' }}
        onClick={() => onNavigate('applicant-dashboard')}
      >
        <ArrowLeft size={16} /> Back to Dashboard
      </button>

      {/* Top Card: Status & Reference Header */}
      <div className="card" style={{ marginBottom: '2rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.5rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.5rem' }}>
              <h1 style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'var(--font-mono)', color: 'var(--text-primary)' }}>
                {app.applicationNumber}
              </h1>
              <StatusBadge status={app.status} />
            </div>
            <div style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
              {app.product?.name} &bull; Submitted on {new Date(app.createdAt).toLocaleDateString()}
            </div>
          </div>

          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Requested Loan Amount</div>
            <div style={{ fontSize: '1.85rem', fontWeight: 800, color: '#38BDF8', fontFamily: 'var(--font-mono)' }}>
              ${app.amount.toLocaleString()}
            </div>
            {app.approvedAmount && app.status === 'APPROVED' && (
              <div style={{ fontSize: '0.85rem', color: '#34D399', fontWeight: 700 }}>
                Approved Amount: ${app.approvedAmount.toLocaleString()}
              </div>
            )}
          </div>
        </div>

        {/* Workflow Lifecycle Stepper */}
        <WorkflowStepper status={app.status} />

        {/* Action Banner for INFORMATION_REQUESTED */}
        {app.status === 'INFORMATION_REQUESTED' && (
          <div
            style={{
              padding: '1.25rem',
              background: 'rgba(234, 88, 12, 0.12)',
              border: '1px solid rgba(234, 88, 12, 0.35)',
              borderRadius: 'var(--radius-md)',
              marginTop: '1rem',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0.75rem', marginBottom: '1rem' }}>
              <AlertTriangle size={22} color="#FB923C" style={{ flexShrink: 0, marginTop: '2px' }} />
              <div>
                <h3 style={{ fontSize: '1rem', fontWeight: 700, color: '#FB923C', marginBottom: '0.25rem' }}>
                  Underwriter Requested Additional Information / Revisions
                </h3>
                <p style={{ fontSize: '0.9rem', color: 'var(--text-primary)' }}>
                  "{app.decisionReason}"
                </p>
              </div>
            </div>

            {!isEditing ? (
              <button
                className="btn btn-primary btn-sm"
                onClick={() => setIsEditing(true)}
              >
                Amend Information &amp; Resubmit
              </button>
            ) : (
              <div style={{ background: 'rgba(17, 24, 39, 0.6)', padding: '1.25rem', borderRadius: 'var(--radius-md)', marginTop: '1rem' }}>
                <h4 style={{ fontWeight: 700, marginBottom: '1rem' }}>Revise Financial Profile</h4>
                <div className="grid-cols-3">
                  <div className="form-group">
                    <label className="form-label">Loan Amount ($)</label>
                    <input
                      type="number"
                      className="form-input"
                      value={editAmount}
                      onChange={(e) => setEditAmount(Number(e.target.value))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Gross Monthly Income ($)</label>
                    <input
                      type="number"
                      className="form-input"
                      value={editIncome}
                      onChange={(e) => setEditIncome(Number(e.target.value))}
                    />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Monthly Debt ($)</label>
                    <input
                      type="number"
                      className="form-input"
                      value={editDebt}
                      onChange={(e) => setEditDebt(Number(e.target.value))}
                    />
                  </div>
                </div>

                <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
                  <button
                    className="btn btn-success btn-sm"
                    onClick={handleSaveAndResubmit}
                    disabled={submitting}
                  >
                    <Send size={14} /> {submitting ? 'Resubmitting...' : 'Save & Resubmit to Underwriter'}
                  </button>
                  <button
                    className="btn btn-secondary btn-sm"
                    onClick={() => setIsEditing(false)}
                  >
                    Cancel
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {error && (
        <div style={{ padding: '1rem', background: 'var(--danger-bg)', borderRadius: 'var(--radius-md)', color: '#F87171', marginBottom: '1.5rem' }}>
          {error}
        </div>
      )}

      {/* Financials & Applicant Snapshot */}
      <div className="grid-cols-2" style={{ marginBottom: '2rem' }}>
        <div className="card">
          <h3 className="card-title">Loan &amp; Financial Snapshot</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.85rem', marginTop: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Interest Rate (APR):</span>
              <span style={{ fontWeight: 600 }}>{app.product?.interestRate}%</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Repayment Tenure:</span>
              <span style={{ fontWeight: 600 }}>{app.tenureMonths} Months</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Estimated Monthly Installment:</span>
              <span style={{ fontWeight: 700, color: '#38BDF8', fontFamily: 'var(--font-mono)' }}>
                ${app.estimatedMonthlyPayment ? app.estimatedMonthlyPayment.toFixed(2) : '-'}
              </span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Gross Monthly Income:</span>
              <span style={{ fontWeight: 600 }}>${app.grossMonthlyIncome?.toLocaleString() || '-'}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Existing Monthly Debt:</span>
              <span style={{ fontWeight: 600 }}>${app.existingMonthlyDebt?.toLocaleString() || '-'}</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Calculated DTI Ratio:</span>
              <span
                className={`risk-pill ${
                  (app.calculatedDti || 0) < 36
                    ? 'risk-low'
                    : (app.calculatedDti || 0) <= 50
                    ? 'risk-medium'
                    : 'risk-high'
                }`}
              >
                {app.calculatedDti ? `${app.calculatedDti}%` : 'Pending Score'}
              </span>
            </div>
          </div>
        </div>

        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <h3 className="card-title" style={{ margin: 0 }}>Uploaded Documents</h3>
            <label className="btn btn-secondary btn-sm" style={{ cursor: uploadingDoc ? 'wait' : 'pointer' }}>
              <Upload size={13} /> {uploadingDoc ? 'Uploading...' : 'Add Document'}
              <input
                type="file"
                style={{ display: 'none' }}
                onChange={(e) => {
                  const f = e.target.files?.[0];
                  if (f) handleUploadAdditionalDoc('OTHER', f);
                }}
                disabled={uploadingDoc}
              />
            </label>
          </div>

          {app.documents.length === 0 ? (
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>No documents uploaded yet.</p>
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
              {app.documents.map((doc) => (
                <div
                  key={doc.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    padding: '0.75rem',
                    background: 'rgba(255, 255, 255, 0.02)',
                    borderRadius: 'var(--radius-sm)',
                    border: '1px solid var(--border-subtle)',
                  }}
                >
                  <div>
                    <div style={{ fontSize: '0.85rem', fontWeight: 600 }}>{doc.fileName}</div>
                    <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>
                      {doc.documentType} &bull; {(doc.fileSize / 1024).toFixed(1)} KB
                    </div>
                  </div>
                  <a
                    href={api.getDocumentDownloadUrl(doc.id)}
                    target="_blank"
                    rel="noreferrer"
                    className="btn btn-secondary btn-sm"
                  >
                    <Download size={13} /> View
                  </a>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Audit Trail Timeline */}
      <div className="card">
        <h3 className="card-title">
          <History size={18} color="#06B6D4" /> Complete Workflow Audit Log
        </h3>
        <p className="card-subtitle">
          Immutable event log of all actions, underwriter remarks, and automated state transitions.
        </p>

        {app.auditLogs.length === 0 ? (
          <p style={{ color: 'var(--text-muted)' }}>No audit events logged yet.</p>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Timestamp</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Status Transition</th>
                  <th>Remarks / Notes</th>
                </tr>
              </thead>
              <tbody>
                {app.auditLogs.map((log) => (
                  <tr key={log.id}>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                    <td style={{ fontWeight: 600 }}>{log.actorName}</td>
                    <td style={{ fontFamily: 'var(--font-mono)', fontSize: '0.8rem', color: '#38BDF8' }}>
                      {log.actionType}
                    </td>
                    <td>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                        {log.fromStatus || 'INIT'} &rarr; {log.toStatus}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.85rem' }}>{log.notes}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
