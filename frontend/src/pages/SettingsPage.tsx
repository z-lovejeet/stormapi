import { useTheme } from '../hooks/useTheme';
import { Button } from '../components/common/Button';
import { Sun, Moon } from 'lucide-react';
import { gsap, useGSAP } from '../utils/gsap';
import styles from './SettingsPage.module.css';

export function SettingsPage() {
  const { theme, toggleTheme } = useTheme();

  useGSAP(() => {
    gsap.from(`.${styles.card}`, {
      y: 30,
      opacity: 0,
      duration: 0.6,
      stagger: 0.15,
      ease: 'power3.out',
    });
  });

  return (
    <div className={styles.page}>
      <h1 className={styles.title}>
        Settings
      </h1>

      <div className={styles.card}>
        <h2 className={styles.sectionTitle}>
          Appearance
        </h2>
        <div className={styles.row}>
          <div>
            <p className={styles.label}>Theme</p>
            <p className={styles.sublabel}>
              Current: {theme === 'dark' ? 'Dark' : 'Light'} mode
            </p>
          </div>
          <Button
            variant="secondary"
            icon={theme === 'dark' ? Sun : Moon}
            onClick={toggleTheme}
          >
            Switch to {theme === 'dark' ? 'Light' : 'Dark'}
          </Button>
        </div>
      </div>

      <div className={styles.card}>
        <h2 className={styles.sectionTitle}>
          About
        </h2>
        <p className={styles.text}>
          StormAPI v1.0.0 — API Performance Testing Platform
        </p>
        <p className={styles.subtext}>
          Built with Java 21 + Spring Boot 3.4 + React 19 + Virtual Threads
        </p>
      </div>
    </div>
  );
}
