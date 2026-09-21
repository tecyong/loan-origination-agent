import React, { useState, useEffect, useMemo } from 'react';
import { api } from '../api/client';
import type { LoanApplication } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import {
  Search,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Download,
  Eye,
  X,
  AlertCircle,
} from 'lucide-react';

export const OfficerDashboard: React.FC = () => {
  const [applications, setApplications] = useState<LoanApplication[]>([]);
  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchTerm, setSearchTerm] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Review Modal State
  const [activeApp, setActiveApp] = useState<LoanApplication | null>(null);
  const [decisionAction, setDecisionAction] = useState<'APPROVE' | 'REJECT' | 'REQUEST_INFO' | null>(null);
  const [approvedAmount, setApprovedAmount] = useState<number>(0);
  const [decisionReason, setDecisionReason] = useState<string>('');
  const [submittingDecision, setSubmittingDecision] = useState(false);

  useEffect(() => {
    loadQueue();
  }, [selectedStatus]);

  const loadQueue = async () => {
    setLoading(true);
    setError(null);
    try {
      const statusParam = selectedStatus === 'ALL' ? undefined : selectedStatus;
      const data = await api.getOfficerQueue(statusParam);
      setApplications(data);
    } catch (err: any) {
      setError('Failed to load underwriting queue. Ensure backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const filteredApps = useMemo(() => {
    return applications.filter((app) => {
      const matchesSearch =
        app.applicationNumber.toLowerCase().includes(searchTerm.toLowerCase()) ||
        app.applicant.fullName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        app.applicant.email.toLowerCase().includes(searchTerm.toLowerCase());
      return matchesSearch;
    });
  }, [applications, searchTerm]);

  const handleOpenReview = (app: LoanApplication) => {
    setActiveApp(app);
    setApprovedAmount(app.amount);
    setDecisionReason('');
    setDecisionAction(null);
  };

  const handleProcessDecision = async () => {
    if (!activeApp || !decisionAction) return;
    setSubmittingDecision(true);
    setError(null);
    try {
      await api.submitOfficerDecision(activeApp.id, {
        action: decisionAction,
        approvedAmount: decisionAction === 'APPROVE' ? approvedAmount : undefined,
        decisionReason: decisionAction !== 'REQUEST_INFO' ? decisionReason : undefined,
        revisionNotes: decisionAction === 'REQUEST_INFO' ? decisionReason : undefined,
      });

      setActiveApp(null);
      setDecisionAction(null);
      await loadQueue();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Error processing decision');
    } finally {
      setSubmittingDecision(false);
    }
  };

  return (
    <div>
      <div style={{ marginBottom: '2rem' }}>
        <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: 'var(--text-primary)' }}>
          Loan Officer Underwriting Console
        </h1>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
          Inspect submitted cases, analyze Debt-to-Income (DTI) metrics, and issue workflow decisions via EximeeBPMS.
        </p>
      </div>

      {error && (
        <div style={{ padding: '1rem', background: 'var(--danger-bg)', borderRadius: 'var(--radius-md)', color: '#F87171', marginBottom: '1.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <AlertCircle size={18} /> {error}
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid-cols-4" style={{ marginBottom: '2rem' }}>
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginBottom: '0.4rem' }}>
            Total In Queue
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'var(--font-mono)' }}>
            {applications.length}
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '0.85rem', color: '#FBBF24', marginBottom: '0.4rem' }}>
            Pending Underwriting
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#FBBF24', fontFamily: 'var(--font-mono)' }}>
            {applications.filter((a) => a.status === 'IN_REVIEW' || a.status === 'SUBMITTED').length}
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '0.85rem', color: '#FB923C', marginBottom: '0.4rem' }}>
            Revisions Requested
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#FB923C', fontFamily: 'var(--font-mono)' }}>
            {applications.filter((a) => a.status === 'INFORMATION_REQUESTED').length}
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '0.85rem', color: '#34D399', marginBottom: '0.4rem' }}>
            Approved Applications
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#34D399', fontFamily: 'var(--font-mono)' }}>
            {applications.filter((a) => a.status === 'APPROVED' || a.status === 'ACCEPTED').length}
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ marginBottom: '1.5rem', padding: '1.25rem' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '1rem', flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
            {['ALL', 'IN_REVIEW', 'INFORMATION_REQUESTED', 'APPROVED', 'REJECTED'].map((st) => (
              <button
                key={st}
                className={`btn btn-sm ${selectedStatus === st ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => setSelectedStatus(st)}
              >
                {st.replace('_', ' ')}
              </button>
            ))}
          </div>

          <div style={{ position: 'relative', minWidth: '280px' }}>
            <Search size={16} color="#9CA3AF" style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }} />
            <input
              type="text"
              className="form-input"
              style={{ paddingLeft: '36px' }}
              placeholder="Search reference, applicant..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>
      </div>

      {/* Applications Table */}
      <div className="card">
        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            Loading queue...
          </div>
        ) : filteredApps.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            No applications found matching the selected criteria.
          </div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Ref Number</th>
                  <th>Applicant</th>
                  <th>Product</th>
                  <th>Amount</th>
                  <th>DTI Ratio</th>
                  <th>Status</th>
                  <th>Submission</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filteredApps.map((app) => (
                  <tr key={app.id}>
                    <td style={{ fontWeight: 700, fontFamily: 'var(--font-mono)', color: '#38BDF8' }}>
                      {app.applicationNumber}
                    </td>
                    <td>
                      <div style={{ fontWeight: 600 }}>{app.applicant.fullName}</div>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>{app.applicant.email}</div>
                    </td>
                    <td>{app.product?.name}</td>
                    <td style={{ fontWeight: 600 }}>${app.amount.toLocaleString()}</td>
                    <td>
                      <span
                        className={`risk-pill ${
                          (app.calculatedDti || 0) < 36
                            ? 'risk-low'
                            : (app.calculatedDti || 0) <= 50
                            ? 'risk-medium'
                            : 'risk-high'
                        }`}
                      >
                        {app.calculatedDti ? `${app.calculatedDti}%` : 'N/A'}
                      </span>
                    </td>
                    <td>
                      <StatusBadge status={app.status} />
                    </td>
                    <td style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                      {new Date(app.createdAt).toLocaleDateString()}
                    </td>
                    <td>
                      <button
                        className="btn btn-primary btn-sm"
                        onClick={() => handleOpenReview(app)}
                      >
                        <Eye size={13} /> Underwrite
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Underwriting Case Modal */}
      {activeApp && (
        <div className="modal-overlay" onClick={() => setActiveApp(null)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()} style={{ maxWidth: '750px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '0.75rem' }}>
              <div>
                <h2 style={{ fontSize: '1.25rem', fontWeight: 800 }}>
                  Case Review: {activeApp.applicationNumber}
                </h2>
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  {activeApp.product?.name} &bull; Applicant: {activeApp.applicant.fullName}
                </span>
              </div>
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => setActiveApp(null)}
                style={{ padding: '0.4rem', border: 'none', background: 'transparent' }}
              >
                <X size={20} />
              </button>
            </div>

            {/* Financial Risk Assessment Summary */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(3, 1fr)',
                gap: '1rem',
                padding: '1rem',
                background: 'rgba(255, 255, 255, 0.02)',
                border: '1px solid var(--border-subtle)',
                borderRadius: 'var(--radius-md)',
                marginBottom: '1.25rem',
              }}
            >
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Monthly Income</div>
                <div style={{ fontWeight: 700, fontSize: '1.1rem' }}>
                  ${activeApp.grossMonthlyIncome?.toLocaleString() || '-'}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Monthly Debt</div>
                <div style={{ fontWeight: 700, fontSize: '1.1rem' }}>
                  ${activeApp.existingMonthlyDebt?.toLocaleString() || '-'}
                </div>
              </div>
              <div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Calculated DTI</div>
                <div style={{ fontWeight: 800, fontSize: '1.1rem', color: (activeApp.calculatedDti || 0) <= 50 ? '#34D399' : '#F87171' }}>
                  {activeApp.calculatedDti ? `${activeApp.calculatedDti}%` : 'N/A'}
                </div>
              </div>
            </div>

            {/* Uploaded Documents */}
            <div style={{ marginBottom: '1.5rem' }}>
              <h4 style={{ fontSize: '0.9rem', fontWeight: 700, marginBottom: '0.5rem' }}>
                Supporting Verification Documents ({activeApp.documents.length})
              </h4>
              {activeApp.documents.length === 0 ? (
                <p style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>No documents uploaded.</p>
              ) : (
                <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap' }}>
                  {activeApp.documents.map((d) => (
                    <a
                      key={d.id}
                      href={api.getDocumentDownloadUrl(d.id)}
                      target="_blank"
                      rel="noreferrer"
                      className="btn btn-secondary btn-sm"
                    >
                      <Download size={12} /> {d.documentType}: {d.fileName}
                    </a>
                  ))}
                </div>
              )}
            </div>

            {/* Decision Action Selector */}
            <div style={{ marginBottom: '1.5rem' }}>
              <label className="form-label">Select Underwriting Action</label>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '0.75rem' }}>
                <button
                  type="button"
                  className={`btn ${decisionAction === 'APPROVE' ? 'btn-success' : 'btn-secondary'}`}
                  onClick={() => setDecisionAction('APPROVE')}
                >
                  <CheckCircle2 size={16} /> Approve Loan
                </button>
                <button
                  type="button"
                  className={`btn ${decisionAction === 'REQUEST_INFO' ? 'btn-primary' : 'btn-secondary'}`}
                  style={{ background: decisionAction === 'REQUEST_INFO' ? '#EA580C' : undefined }}
                  onClick={() => setDecisionAction('REQUEST_INFO')}
                >
                  <AlertTriangle size={16} /> Request Info
                </button>
                <button
                  type="button"
                  className={`btn ${decisionAction === 'REJECT' ? 'btn-danger' : 'btn-secondary'}`}
                  onClick={() => setDecisionAction('REJECT')}
                >
                  <XCircle size={16} /> Reject Loan
                </button>
              </div>
            </div>

            {/* Dynamic Decision Fields */}
            {decisionAction === 'APPROVE' && (
              <div className="form-group">
                <label className="form-label">Approved Credit Amount ($)</label>
                <input
                  type="number"
                  className="form-input"
                  value={approvedAmount}
                  onChange={(e) => setApprovedAmount(Number(e.target.value))}
                />
                <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  Requested amount was ${activeApp.amount.toLocaleString()}. You may adjust downwards if needed.
                </span>
              </div>
            )}

            {decisionAction && (
              <div className="form-group">
                <label className="form-label">
                  {decisionAction === 'APPROVE'
                    ? 'Approval Conditions & Remarks'
                    : decisionAction === 'REQUEST_INFO'
                    ? 'Clarifications / Missing Information Required from Applicant'
                    : 'Formal Rejection Reason & Policy Rationale'}
                </label>
                <textarea
                  className="form-textarea"
                  rows={3}
                  placeholder="Enter detailed underwriter assessment remarks..."
                  value={decisionReason}
                  onChange={(e) => setDecisionReason(e.target.value)}
                  required
                />
              </div>
            )}

            {decisionAction && (
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button className="btn btn-secondary" onClick={() => setDecisionAction(null)}>
                  Cancel
                </button>
                <button
                  className="btn btn-primary"
                  onClick={handleProcessDecision}
                  disabled={submittingDecision || !decisionReason.trim()}
                >
                  {submittingDecision ? 'Submitting to EximeeBPMS...' : 'Confirm Decision'}
                </button>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
