import React, { useState, useEffect } from 'react';
import { api } from '../api/client';
import type { LoanApplication, LoanProduct } from '../types';
import { StatusBadge } from '../components/StatusBadge';
import { AmortizationCalculator } from '../components/AmortizationCalculator';
import { PlusCircle, FileText, ArrowRight, CheckCircle2, Clock, DollarSign, AlertCircle } from 'lucide-react';

interface ApplicantDashboardProps {
  onNavigate: (view: string, params?: { applicationId?: number; productId?: number; amount?: number; tenure?: number }) => void;
}

export const ApplicantDashboard: React.FC<ApplicantDashboardProps> = ({ onNavigate }) => {
  const [applications, setApplications] = useState<LoanApplication[]>([]);
  const [products, setProducts] = useState<LoanProduct[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [appsData, prodsData] = await Promise.all([
        api.getMyApplications(),
        api.getProducts(),
      ]);
      setApplications(appsData);
      setProducts(prodsData);
    } catch (err: any) {
      setError('Unable to load application dashboard data. Please make sure backend is running.');
    } finally {
      setLoading(false);
    }
  };

  const approvedTotal = applications
    .filter((a) => a.status === 'APPROVED' || a.status === 'ACCEPTED')
    .reduce((sum, a) => sum + (a.approvedAmount || a.amount), 0);

  const pendingReviewCount = applications.filter(
    (a) => a.status === 'SUBMITTED' || a.status === 'IN_REVIEW' || a.status === 'INFORMATION_REQUESTED'
  ).length;

  return (
    <div>
      {/* Welcome Banner */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '2rem',
          flexWrap: 'wrap',
          gap: '1rem',
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            Applicant Dashboard
          </h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.95rem' }}>
            Monitor your loan applications, calculate repayments, or start a new loan request.
          </p>
        </div>

        <button
          className="btn btn-primary"
          onClick={() => onNavigate('application-wizard')}
        >
          <PlusCircle size={18} /> Apply for New Loan
        </button>
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

      {/* Metrics Row */}
      <div className="grid-cols-4" style={{ marginBottom: '2rem' }}>
        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
            <FileText size={16} color="#06B6D4" /> Total Applications
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, fontFamily: 'var(--font-mono)' }}>
            {applications.length}
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
            <Clock size={16} color="#FBBF24" /> In Review / Pending
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#FBBF24', fontFamily: 'var(--font-mono)' }}>
            {pendingReviewCount}
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
            <CheckCircle2 size={16} color="#34D399" /> Approved Loans
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#34D399', fontFamily: 'var(--font-mono)' }}>
            {applications.filter((a) => a.status === 'APPROVED' || a.status === 'ACCEPTED').length}
          </div>
        </div>

        <div className="card" style={{ padding: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '0.5rem' }}>
            <DollarSign size={16} color="#38BDF8" /> Approved Credit
          </div>
          <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#38BDF8', fontFamily: 'var(--font-mono)' }}>
            ${approvedTotal.toLocaleString()}
          </div>
        </div>
      </div>

      {/* Calculator Section */}
      {products.length > 0 && (
        <div style={{ marginBottom: '2.5rem' }}>
          <AmortizationCalculator
            products={products}
            onApplyWithTerms={(productId, amount, tenure) => {
              onNavigate('application-wizard', { productId, amount, tenure });
            }}
          />
        </div>
      )}

      {/* Applications Table */}
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div>
            <h2 className="card-title">My Loan Applications</h2>
            <p className="card-subtitle" style={{ margin: 0 }}>
              Track lifecycle states, underwriter remarks, and documents.
            </p>
          </div>
        </div>

        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
            Loading your applications...
          </div>
        ) : applications.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', border: '1px dashed var(--border-subtle)', borderRadius: 'var(--radius-md)' }}>
            <FileText size={40} color="#6B7280" style={{ margin: '0 auto 1rem' }} />
            <h3 style={{ fontSize: '1.1rem', fontWeight: 700, marginBottom: '0.5rem' }}>
              No applications yet
            </h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.25rem' }}>
              You have not submitted any loan applications. Use the calculator above or click Apply Now.
            </p>
            <button className="btn btn-primary btn-sm" onClick={() => onNavigate('application-wizard')}>
              <PlusCircle size={15} /> Start Application
            </button>
          </div>
        ) : (
          <div className="table-container">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Application Ref</th>
                  <th>Product</th>
                  <th>Amount</th>
                  <th>Tenure</th>
                  <th>Monthly Payment</th>
                  <th>Status</th>
                  <th>Created Date</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {applications.map((app) => (
                  <tr key={app.id}>
                    <td style={{ fontWeight: 700, fontFamily: 'var(--font-mono)', color: '#38BDF8' }}>
                      {app.applicationNumber}
                    </td>
                    <td>{app.product?.name}</td>
                    <td style={{ fontWeight: 600 }}>${app.amount.toLocaleString()}</td>
                    <td>{app.tenureMonths} mos</td>
                    <td style={{ fontFamily: 'var(--font-mono)' }}>
                      ${app.estimatedMonthlyPayment ? app.estimatedMonthlyPayment.toFixed(2) : '-'}
                    </td>
                    <td>
                      <StatusBadge status={app.status} />
                    </td>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.825rem' }}>
                      {new Date(app.createdAt).toLocaleDateString()}
                    </td>
                    <td>
                      <button
                        className="btn btn-secondary btn-sm"
                        onClick={() => onNavigate('application-detail', { applicationId: app.id })}
                      >
                        {app.status === 'DRAFT' ? 'Continue Draft' : 'View Status'} <ArrowRight size={13} />
                      </button>
                    </td>
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
