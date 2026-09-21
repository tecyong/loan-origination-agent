import React, { useState } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { ApplicantDashboard } from './pages/ApplicantDashboard';
import { ApplicationWizard } from './pages/ApplicationWizard';
import { ApplicationDetail } from './pages/ApplicationDetail';
import { OfficerDashboard } from './pages/OfficerDashboard';

const MainApp: React.FC = () => {
  const { isAuthenticated, isOfficer } = useAuth();
  const [currentView, setCurrentView] = useState<string>('login');
  const [navigationParams, setNavigationParams] = useState<{
    applicationId?: number;
    productId?: number;
    amount?: number;
    tenure?: number;
  }>({});

  const handleNavigate = (view: string, params?: typeof navigationParams) => {
    setCurrentView(view);
    if (params) {
      setNavigationParams(params);
    }
  };

  // If not logged in, only allow login or register
  if (!isAuthenticated) {
    if (currentView === 'register') {
      return (
        <div className="app-container">
          <Navbar currentView={currentView} onNavigate={handleNavigate} />
          <main className="main-content">
            <RegisterPage onNavigate={handleNavigate} />
          </main>
        </div>
      );
    }
    return (
      <div className="app-container">
        <Navbar currentView={currentView} onNavigate={handleNavigate} />
        <main className="main-content">
          <LoginPage onNavigate={handleNavigate} />
        </main>
      </div>
    );
  }

  // Set default view based on role if on login page
  let effectiveView = currentView;
  if (effectiveView === 'login' || effectiveView === 'register') {
    effectiveView = isOfficer ? 'officer-dashboard' : 'applicant-dashboard';
  }

  return (
    <div className="app-container">
      <Navbar currentView={effectiveView} onNavigate={handleNavigate} />
      <main className="main-content">
        {effectiveView === 'applicant-dashboard' && (
          <ApplicantDashboard onNavigate={handleNavigate} />
        )}
        {effectiveView === 'application-wizard' && (
          <ApplicationWizard
            initialProductId={navigationParams.productId}
            initialAmount={navigationParams.amount}
            initialTenure={navigationParams.tenure}
            onNavigate={handleNavigate}
          />
        )}
        {effectiveView === 'application-detail' && navigationParams.applicationId && (
          <ApplicationDetail
            applicationId={navigationParams.applicationId}
            onNavigate={handleNavigate}
          />
        )}
        {effectiveView === 'officer-dashboard' && <OfficerDashboard />}
      </main>
    </div>
  );
};

export default function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}
