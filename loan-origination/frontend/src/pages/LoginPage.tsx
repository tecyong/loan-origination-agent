import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';
import { ShieldCheck, LogIn, Sparkles, User, UserCheck } from 'lucide-react';

interface LoginPageProps {
  onNavigate: (view: string) => void;
}

export const LoginPage: React.FC<LoginPageProps> = ({ onNavigate }) => {
  const { login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.login({ email, password });
      login(res.token, res.user);
      if (res.user.role === 'ROLE_OFFICER' || res.user.role === 'ROLE_ADMIN') {
        onNavigate('officer-dashboard');
      } else {
        onNavigate('applicant-dashboard');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid email or password credentials');
    } finally {
      setLoading(false);
    }
  };

  const handleQuickLogin = async (demoEmail: string) => {
    setEmail(demoEmail);
    setPassword('password123');
    setError(null);
    setLoading(true);
    try {
      const res = await api.login({ email: demoEmail, password: 'password123' });
      login(res.token, res.user);
      if (res.user.role === 'ROLE_OFFICER' || res.user.role === 'ROLE_ADMIN') {
        onNavigate('officer-dashboard');
      } else {
        onNavigate('applicant-dashboard');
      }
    } catch (err: any) {
      setError('Unable to log in with demo account. Ensure backend is running.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: '440px', margin: '3rem auto' }}>
      <div className="card" style={{ boxShadow: 'var(--shadow-md)' }}>
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <div
            style={{
              width: '54px',
              height: '54px',
              borderRadius: '50%',
              background: 'rgba(2, 132, 199, 0.15)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 1rem',
              border: '1px solid rgba(2, 132, 199, 0.3)',
            }}
          >
            <ShieldCheck size={30} color="#06B6D4" />
          </div>
          <h2 style={{ fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            Welcome to Eximee LOS
          </h2>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Sign in to access your loan origination portal
          </p>
        </div>

        {error && (
          <div
            style={{
              padding: '0.75rem 1rem',
              background: 'var(--danger-bg)',
              border: '1px solid rgba(239, 68, 68, 0.3)',
              borderRadius: 'var(--radius-md)',
              color: '#F87171',
              fontSize: '0.85rem',
              marginBottom: '1.25rem',
            }}
          >
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label">Email Address</label>
            <input
              type="email"
              className="form-input"
              placeholder="name@example.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: '0.5rem' }}
            disabled={loading}
          >
            <LogIn size={16} /> {loading ? 'Signing In...' : 'Sign In'}
          </button>
        </form>

        <div style={{ marginTop: '2rem', borderTop: '1px solid var(--border-subtle)', paddingTop: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
            <Sparkles size={14} color="#F59E0B" />
            <span style={{ textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: 700 }}>
              1-Click Demo Accounts
            </span>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ justifyContent: 'flex-start', padding: '0.5rem 0.8rem' }}
              onClick={() => handleQuickLogin('applicant@demo.com')}
            >
              <User size={14} color="#38BDF8" />
              <div style={{ textAlign: 'left', lineHeight: 1.2 }}>
                <div style={{ fontWeight: 600 }}>Alex Morgan (Applicant)</div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>applicant@demo.com</div>
              </div>
            </button>

            <button
              type="button"
              className="btn btn-secondary btn-sm"
              style={{ justifyContent: 'flex-start', padding: '0.5rem 0.8rem' }}
              onClick={() => handleQuickLogin('officer@demo.com')}
            >
              <UserCheck size={14} color="#FBBF24" />
              <div style={{ textAlign: 'left', lineHeight: 1.2 }}>
                <div style={{ fontWeight: 600 }}>Sarah Jenkins (Loan Officer / Underwriter)</div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)' }}>officer@demo.com</div>
              </div>
            </button>
          </div>
        </div>

        <div style={{ textAlign: 'center', marginTop: '1.5rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
          Don't have an account?{' '}
          <a href="#register" onClick={(e) => { e.preventDefault(); onNavigate('register'); }}>
            Create one here
          </a>
        </div>
      </div>
    </div>
  );
};
