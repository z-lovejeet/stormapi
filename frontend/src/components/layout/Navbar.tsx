import { useState, useEffect, useCallback } from 'react';
import { NavLink, Link, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Plus,
  History,
  FolderOpen,
  Workflow,
  Database,
  Settings,
  Sun,
  Moon,
  Menu,
  X,
  LogOut,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTheme } from '../../hooks/useTheme';
import { useAuth } from '../../hooks/useAuth';
import { ROUTES } from '../../utils/constants';
import styles from './Navbar.module.css';

const navItems = [
  { to: ROUTES.DASHBOARD, icon: LayoutDashboard, label: 'Dashboard' },
  { to: ROUTES.TEST_BUILDER, icon: Plus, label: 'New Test' },
  { to: ROUTES.HISTORY, icon: History, label: 'History' },
  { to: ROUTES.COLLECTIONS, icon: FolderOpen, label: 'Collections' },
  { to: ROUTES.SCENARIOS, icon: Workflow, label: 'Scenarios' },
  { to: ROUTES.DATA_DRIVEN, icon: Database, label: 'Data-Driven' },
  { to: ROUTES.SETTINGS, icon: Settings, label: 'Settings' },
] as const;

/**
 * Top navbar with desktop horizontal links and mobile hamburger menu.
 * Replaces the old sidebar-based navigation.
 */
export function Navbar() {
  const { theme, toggleTheme } = useTheme();
  const { user, isAuthenticated, logout } = useAuth();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  // Close mobile menu on route change
  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  // Close mobile menu on Escape key
  useEffect(() => {
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setMobileOpen(false);
    };
    if (mobileOpen) {
      document.addEventListener('keydown', handleKey);
      // Prevent body scroll when menu is open
      document.body.style.overflow = 'hidden';
    }
    return () => {
      document.removeEventListener('keydown', handleKey);
      document.body.style.overflow = '';
    };
  }, [mobileOpen]);

  const toggleMobile = useCallback(() => {
    setMobileOpen((prev) => !prev);
  }, []);

  return (
    <>
      <nav className={styles.navbar} aria-label="Main navigation">
        {/* Logo */}
        <Link to="/dashboard" className={styles.logo}>
          <span className={styles.logoIcon}>⚡</span>
          <span className={styles.logoText}>StormAPI</span>
        </Link>

        {/* Desktop Nav Links */}
        <div className={styles.navLinks}>
          {navItems.map(({ to, icon: Icon, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.navLinkActive : ''}`
              }
              title={label}
            >
              <Icon className={styles.navIcon} strokeWidth={2} />
              <span className={styles.navLabel}>{label}</span>
            </NavLink>
          ))}
        </div>

        {/* Right Actions */}
        <div className={styles.actions}>
          <button
            className={styles.themeToggle}
            onClick={toggleTheme}
            aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
            type="button"
          >
            {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
          </button>

          {/* User Menu */}
          {isAuthenticated && user ? (
            <div className={styles.userMenu}>
              {user.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.name} className={styles.avatar} />
              ) : (
                <div className={styles.avatarFallback}>
                  {user.name?.charAt(0)?.toUpperCase() || 'U'}
                </div>
              )}
              <button
                className={styles.logoutBtn}
                onClick={logout}
                title="Logout"
                type="button"
              >
                <LogOut size={16} />
              </button>
            </div>
          ) : null}

          {/* Hamburger (mobile only) */}
          <button
            className={styles.hamburger}
            onClick={toggleMobile}
            aria-label={mobileOpen ? 'Close menu' : 'Open menu'}
            aria-expanded={mobileOpen}
            type="button"
          >
            {mobileOpen ? <X size={20} /> : <Menu size={20} />}
          </button>
        </div>
      </nav>

      {/* Mobile Menu */}
      <AnimatePresence>
        {mobileOpen && (
          <>
            {/* Backdrop */}
            <motion.div
              className={styles.mobileOverlay}
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              onClick={() => setMobileOpen(false)}
              aria-hidden="true"
            />

            {/* Menu Panel */}
            <motion.div
              className={styles.mobileMenu}
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2, ease: [0.4, 0, 0.2, 1] }}
            >
              {navItems.map(({ to, icon: Icon, label }) => (
                <NavLink
                  key={to}
                  to={to}
                  className={({ isActive }) =>
                    `${styles.mobileNavLink} ${isActive ? styles.mobileNavLinkActive : ''}`
                  }
                  onClick={() => setMobileOpen(false)}
                >
                  <Icon className={styles.mobileNavIcon} strokeWidth={2} />
                  <span>{label}</span>
                </NavLink>
              ))}
            </motion.div>
          </>
        )}
      </AnimatePresence>
    </>
  );
}
