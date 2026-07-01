import { NavLink } from 'react-router-dom';
import { LayoutDashboard, Plus, History, FolderOpen, Workflow, Settings } from 'lucide-react';
import { motion } from 'framer-motion';
import { ROUTES } from '../../utils/constants';
import styles from './Sidebar.module.css';

const navItems = [
  { to: ROUTES.DASHBOARD, icon: LayoutDashboard, label: 'Dashboard' },
  { to: ROUTES.TEST_BUILDER, icon: Plus, label: 'New Test' },
  { to: ROUTES.HISTORY, icon: History, label: 'History' },
  { to: ROUTES.COLLECTIONS, icon: FolderOpen, label: 'Collections' },
  { to: ROUTES.SCENARIOS, icon: Workflow, label: 'Scenarios' },
  { to: ROUTES.SETTINGS, icon: Settings, label: 'Settings' },
] as const;

export function Sidebar() {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.logo}>
        <span className={styles.logoIcon}>⚡</span>
        <span className={styles.logoText}>StormAPI</span>
      </div>

      <nav className={styles.nav} aria-label="Main navigation">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `${styles.navItem} ${isActive ? styles.navItemActive : ''}`
            }
            aria-current={undefined}
          >
            {({ isActive }) => (
              <motion.div
                style={{ display: 'flex', alignItems: 'center', gap: 'var(--storm-space-3)', width: '100%' }}
                whileHover={{ x: 2 }}
                transition={{ duration: 0.15 }}
              >
                <Icon className={styles.navIcon} strokeWidth={isActive ? 2.5 : 2} />
                <span>{label}</span>
              </motion.div>
            )}
          </NavLink>
        ))}
      </nav>

      <div className={styles.footer}>
        <span className={styles.version}>StormAPI v1.0.0</span>
      </div>
    </aside>
  );
}
