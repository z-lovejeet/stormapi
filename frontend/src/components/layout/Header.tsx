import { useMemo } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { Sun, Moon, ChevronRight } from 'lucide-react';
import { useTheme } from '../../hooks/useTheme';
import styles from './Header.module.css';

/** Maps route segments to display labels. */
const SEGMENT_LABELS: Record<string, string> = {
  dashboard: 'Dashboard',
  tests: 'Tests',
  new: 'New Test',
  live: 'Live Monitor',
  result: 'Result',
  history: 'History',
  collections: 'Collections',
  settings: 'Settings',
};

export function Header() {
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();

  const breadcrumbs = useMemo(() => {
    const segments = location.pathname.split('/').filter(Boolean);
    return segments.map((seg, i) => {
      const path = '/' + segments.slice(0, i + 1).join('/');
      const label = SEGMENT_LABELS[seg] || (seg.match(/^\d+$/) ? `#${seg}` : seg);
      const isLast = i === segments.length - 1;
      return { path, label, isLast };
    });
  }, [location.pathname]);

  return (
    <header className={styles.header}>
      <nav className={styles.breadcrumbs} aria-label="Breadcrumb">
        {breadcrumbs.map((crumb, i) => (
          <span key={crumb.path} style={{ display: 'flex', alignItems: 'center', gap: 'var(--storm-space-2)' }}>
            {i > 0 && <ChevronRight size={14} className={styles.separator} />}
            {crumb.isLast ? (
              <span className={styles.breadcrumbCurrent}>{crumb.label}</span>
            ) : (
              <Link to={crumb.path} className={styles.breadcrumbLink}>
                {crumb.label}
              </Link>
            )}
          </span>
        ))}
      </nav>

      <div className={styles.actions}>
        <button
          className={styles.themeToggle}
          onClick={toggleTheme}
          aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
          type="button"
        >
          {theme === 'dark' ? <Sun size={18} /> : <Moon size={18} />}
        </button>
      </div>
    </header>
  );
}
