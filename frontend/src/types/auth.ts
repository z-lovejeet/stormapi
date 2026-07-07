/**
 * Auth-related types for the frontend.
 */

export interface AuthUser {
  id: number;
  email: string;
  name: string;
  avatarUrl: string | null;
  provider: 'GOOGLE' | 'GITHUB';
}

export interface AuthStatus {
  authenticated: boolean;
  user: AuthUser | null;
}
