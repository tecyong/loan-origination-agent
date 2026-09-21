import React, { createContext, useContext, useState } from 'react';
import type { User } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (token: string, user: User) => void;
  logout: () => void;
  isAuthenticated: boolean;
  isOfficer: boolean;
  isAdmin: boolean;
  isApplicant: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const stored = localStorage.getItem('los_auth_user');
    return stored ? JSON.parse(stored) : null;
  });
  const [token, setToken] = useState<string | null>(() => {
    return localStorage.getItem('los_auth_token');
  });

  const login = (newToken: string, newUser: User) => {
    localStorage.setItem('los_auth_token', newToken);
    localStorage.setItem('los_auth_user', JSON.stringify(newUser));
    setToken(newToken);
    setUser(newUser);
  };

  const logout = () => {
    localStorage.removeItem('los_auth_token');
    localStorage.removeItem('los_auth_user');
    setToken(null);
    setUser(null);
  };

  const isOfficer = user?.role === 'ROLE_OFFICER';
  const isAdmin = user?.role === 'ROLE_ADMIN';
  const isApplicant = user?.role === 'ROLE_APPLICANT';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        login,
        logout,
        isAuthenticated: !!token && !!user,
        isOfficer,
        isAdmin,
        isApplicant,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
