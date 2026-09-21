import React from 'react';
import { useAuth } from '../context/AuthContext';
import { ShieldCheck, UserCheck, LogOut, PlusCircle, LayoutDashboard, FileText } from 'lucide-react';

interface NavbarProps {
  currentView: string;
  onNavigate: (view: string) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentView, onNavigate }) => {
  const { user, isAuthenticated, logout, isOfficer, isAdmin, isApplicant } = useAuth();

  return (
    <header className="navbar">
      <div className="brand-logo" style={{ cursor: 'pointer' }} onClick={() => onNavigate(isOfficer ? 'officer-dashboard' : 'applicant-dashboard')}>
        <ShieldCheck size={28} color="#06B6D4" />
        <span>Eximee <span style={{ color: '#06B6D4' }}>LOS</span></span>
        <span className="brand-badge">BPMN 2.0</span>
      </div>

      {isAuthenticated && (
        <nav className="nav-links">
          {isApplicant && (
            <>
              <button
                className={`btn btn-sm ${currentView === 'applicant-dashboard' ? 'btn-secondary' : 'btn-secondary'}`}
                style={{ background: currentView === 'applicant-dashboard' ? 'rgba(255,255,255,0.1)' : 'transparent', border: 'none' }}
                onClick={() => onNavigate('applicant-dashboard')}
              >
                <LayoutDashboard size={16} /> My Applications
              </button>
              <button
                className="btn btn-primary btn-sm"
                onClick={() => onNavigate('application-wizard')}
              >
                <PlusCircle size={16} /> Apply for Loan
              </button>
            </>
          )}

          {(isOfficer || isAdmin) && (
            <button
              className={`btn btn-sm ${currentView === 'officer-dashboard' ? 'btn-secondary' : 'btn-secondary'}`}
              style={{ background: currentView === 'officer-dashboard' ? 'rgba(255,255,255,0.1)' : 'transparent', border: 'none' }}
              onClick={() => onNavigate('officer-dashboard')}
            >
              <FileText size={16} /> Underwriting Queue
            </button>
          )}

          <div className="user-badge">
            <UserCheck size={16} color="#9CA3AF" />
            <span style={{ fontWeight: 600 }}>{user?.fullName}</span>
            <span className={`role-chip ${isOfficer ? 'officer' : isAdmin ? 'admin' : 'applicant'}`}>
              {isOfficer ? 'Officer' : isAdmin ? 'Admin' : 'Applicant'}
            </span>
          </div>

          <button
            className="btn btn-secondary btn-sm"
            onClick={logout}
            title="Log Out"
          >
            <LogOut size={16} />
          </button>
        </nav>
      )}
    </header>
  );
};
