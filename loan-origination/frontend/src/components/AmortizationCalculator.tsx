import React, { useState, useMemo } from 'react';
import type { LoanProduct } from '../types';
import { Calculator, ArrowRight } from 'lucide-react';

interface AmortizationCalculatorProps {
  products: LoanProduct[];
  selectedProduct?: LoanProduct;
  onApplyWithTerms?: (productId: number, amount: number, tenure: number) => void;
}

export const AmortizationCalculator: React.FC<AmortizationCalculatorProps> = ({
  products,
  selectedProduct: initialProduct,
  onApplyWithTerms,
}) => {
  const [selectedProductId, setSelectedProductId] = useState<number>(
    initialProduct?.id || products[0]?.id || 1
  );

  const activeProduct = useMemo(() => {
    return products.find((p) => p.id === selectedProductId) || products[0];
  }, [products, selectedProductId]);

  const [amount, setAmount] = useState<number>(() => {
    if (!activeProduct) return 10000;
    return Math.min(Math.max(10000, activeProduct.minAmount), activeProduct.maxAmount);
  });

  const [tenure, setTenure] = useState<number>(() => {
    if (!activeProduct) return 24;
    return Math.min(Math.max(24, activeProduct.minTenureMonths), activeProduct.maxTenureMonths);
  });

  const calculations = useMemo(() => {
    if (!activeProduct) return { emi: 0, totalInterest: 0, totalPayment: 0 };
    const P = amount;
    const n = tenure;
    const annualRate = activeProduct.interestRate;
    const r = annualRate / (12 * 100);

    if (r === 0 || n <= 0) {
      const emi = P / n;
      return { emi, totalInterest: 0, totalPayment: P };
    }

    const factor = Math.pow(1 + r, n);
    const emi = (P * r * factor) / (factor - 1);
    const totalPayment = emi * n;
    const totalInterest = totalPayment - P;

    return {
      emi: Math.round(emi * 100) / 100,
      totalInterest: Math.round(totalInterest * 100) / 100,
      totalPayment: Math.round(totalPayment * 100) / 100,
    };
  }, [activeProduct, amount, tenure]);

  return (
    <div className="card" style={{ border: '1px solid var(--border-highlight)' }}>
      <div className="card-title">
        <Calculator size={20} color="#06B6D4" />
        Interactive Loan Repayment Calculator
      </div>
      <div className="card-subtitle">
        Simulate real-time monthly installments, interest breakdown, and amortization.
      </div>

      <div className="form-group">
        <label className="form-label">Selected Loan Product</label>
        <select
          className="form-select"
          value={selectedProductId}
          onChange={(e) => {
            const newId = Number(e.target.value);
            setSelectedProductId(newId);
            const p = products.find((prod) => prod.id === newId);
            if (p) {
              setAmount(Math.min(Math.max(amount, p.minAmount), p.maxAmount));
              setTenure(Math.min(Math.max(tenure, p.minTenureMonths), p.maxTenureMonths));
            }
          }}
        >
          {products.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name} — {p.interestRate}% APR (Max ${p.maxAmount.toLocaleString()})
            </option>
          ))}
        </select>
      </div>

      <div className="grid-cols-2" style={{ marginBottom: '1.5rem' }}>
        <div className="form-group">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
            <span className="form-label" style={{ margin: 0 }}>Borrowing Amount</span>
            <span style={{ fontWeight: 700, color: '#38BDF8', fontFamily: 'var(--font-mono)' }}>
              ${amount.toLocaleString()}
            </span>
          </div>
          <input
            type="range"
            min={activeProduct?.minAmount || 1000}
            max={activeProduct?.maxAmount || 50000}
            step={500}
            value={amount}
            onChange={(e) => setAmount(Number(e.target.value))}
            style={{ width: '100%', accentColor: '#0284C7' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            <span>Min: ${activeProduct?.minAmount.toLocaleString()}</span>
            <span>Max: ${activeProduct?.maxAmount.toLocaleString()}</span>
          </div>
        </div>

        <div className="form-group">
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.4rem' }}>
            <span className="form-label" style={{ margin: 0 }}>Tenure (Months)</span>
            <span style={{ fontWeight: 700, color: '#38BDF8', fontFamily: 'var(--font-mono)' }}>
              {tenure} Months ({Math.round((tenure / 12) * 10) / 10} yrs)
            </span>
          </div>
          <input
            type="range"
            min={activeProduct?.minTenureMonths || 6}
            max={activeProduct?.maxTenureMonths || 60}
            step={activeProduct?.code === 'HOME_EQUITY' ? 12 : 6}
            value={tenure}
            onChange={(e) => setTenure(Number(e.target.value))}
            style={{ width: '100%', accentColor: '#0284C7' }}
          />
          <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            <span>Min: {activeProduct?.minTenureMonths} mos</span>
            <span>Max: {activeProduct?.maxTenureMonths} mos</span>
          </div>
        </div>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(3, 1fr)',
          gap: '1rem',
          padding: '1.25rem',
          background: 'rgba(2, 132, 199, 0.08)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid rgba(2, 132, 199, 0.2)',
          marginBottom: '1.5rem',
        }}
      >
        <div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.2rem' }}>
            Estimated Monthly Payment
          </div>
          <div style={{ fontSize: '1.65rem', fontWeight: 800, color: '#38BDF8', fontFamily: 'var(--font-mono)' }}>
            ${calculations.emi.toFixed(2)}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Principal + Interest</div>
        </div>

        <div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.2rem' }}>
            Total Interest Payable
          </div>
          <div style={{ fontSize: '1.3rem', fontWeight: 700, color: '#FBBF24', fontFamily: 'var(--font-mono)' }}>
            ${calculations.totalInterest.toFixed(2)}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Over {tenure} months</div>
        </div>

        <div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-secondary)', marginBottom: '0.2rem' }}>
            Total Repayment Amount
          </div>
          <div style={{ fontSize: '1.3rem', fontWeight: 700, color: '#34D399', fontFamily: 'var(--font-mono)' }}>
            ${calculations.totalPayment.toFixed(2)}
          </div>
          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Principal + Total Interest</div>
        </div>
      </div>

      {onApplyWithTerms && (
        <button
          className="btn btn-primary"
          style={{ width: '100%' }}
          onClick={() => onApplyWithTerms(selectedProductId, amount, tenure)}
        >
          Apply for this Loan ({activeProduct?.name}) <ArrowRight size={16} />
        </button>
      )}
    </div>
  );
};
