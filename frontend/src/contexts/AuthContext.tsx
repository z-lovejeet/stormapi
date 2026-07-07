import React, { createContext, useEffect, useState, useCallback, useMemo } from 'react';
import type { AuthUser } from '../types/auth';
import { getAuthStatus, logout as logoutApi } from '../api/authApi';

export interface AuthContextType {
  user: AuthUser | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (provider: 'google' | 'github') => void;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
}

export const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [loading, setLoading] = useState(true);

  const refreshAuth = useCallback(async () => {
    try {
      const status = await getAuthStatus();
      setUser(status.authenticated && status.user ? status.user : null);
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refreshAuth();
  }, [refreshAuth]);

  const login = useCallback((provider: 'google' | 'github') => {
    // Redirect to Spring Security OAuth2 authorization endpoint
    window.location.href = `/oauth2/authorization/${provider}`;
  }, []);

  const logout = useCallback(async () => {
    try {
      await logoutApi();
    } finally {
      setUser(null);
      window.location.href = '/';
    }
  }, []);

  const value = useMemo<AuthContextType>(() => ({
    user,
    loading,
    isAuthenticated: user !== null,
    login,
    logout,
    refreshAuth,
  }), [user, loading, login, logout, refreshAuth]);

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
}
