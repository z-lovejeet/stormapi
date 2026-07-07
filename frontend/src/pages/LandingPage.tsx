import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import {
  Zap,
  BarChart3,
  Globe,
  Shield,
  Database,
  GitBranch,
} from 'lucide-react';
import styles from './LandingPage.module.css';

const features = [
  {
    icon: Zap,
    colorClass: 'featureIconBlue',
    title: 'Load & Stress Testing',
    desc: 'Simulate thousands of concurrent users with configurable virtual users, ramp-up, and duration settings.',
  },
  {
    icon: BarChart3,
    colorClass: 'featureIconPurple',
    title: 'Real-Time Metrics',
    desc: 'Watch requests per second, latency percentiles, and error rates update live as your test runs.',
  },
  {
    icon: Globe,
    colorClass: 'featureIconGreen',
    title: 'API Collections',
    desc: 'Organize your endpoints into collections with headers, auth, and body templates for easy reuse.',
  },
  {
    icon: Shield,
    colorClass: 'featureIconAmber',
    title: 'Scenario Builder',
    desc: 'Chain multiple endpoints into realistic user flows with think-time, assertions, and data extraction.',
  },
  {
    icon: Database,
    colorClass: 'featureIconRose',
    title: 'Data-Driven Tests',
    desc: 'Upload CSV datasets to parameterize your tests with real-world data combinations.',
  },
  {
    icon: GitBranch,
    colorClass: 'featureIconCyan',
    title: 'Export & Compare',
    desc: 'Export results as JSON/CSV, compare runs side-by-side, and track performance regressions.',
  },
];

/**
 * Public landing page — hero section, feature grid, and CTAs.
 * Redirects authenticated users to dashboard.
 */
export function LandingPage() {
  const { isAuthenticated, loading } = useAuth();

  if (!loading && isAuthenticated) {
    return <Navigate to="/dashboard" replace />;
  }

  return (
    <div className={styles.landingPage}>
      {/* ── Navbar ── */}
      <nav className={styles.landingNav}>
        <div className={styles.landingLogo}>
          <span className={styles.landingLogoIcon}>⚡</span>
          <span className={styles.landingLogoText}>StormAPI</span>
        </div>
        <div className={styles.landingNavActions}>
          <Link to="/login" className={styles.loginBtn}>
            Log in
          </Link>
          <Link to="/login" className={styles.getStartedBtn}>
            Get Started
          </Link>
        </div>
      </nav>

      {/* ── Hero ── */}
      <section className={styles.hero}>
        <div className={styles.heroGlow} aria-hidden="true" />
        <div className={styles.heroBadge}>
          ⚡ API Performance Testing Platform
        </div>
        <h1 className={styles.heroTitle}>
          <span className={styles.heroTitleGradient}>
            Break your APIs before your users do
          </span>
        </h1>
        <p className={styles.heroSubtitle}>
          StormAPI is a modern load testing platform. Build scenarios,
          simulate real traffic, and identify bottlenecks — all from your browser.
        </p>
        <div className={styles.heroCtas}>
          <Link to="/login" className={styles.ctaPrimary}>
            Start Testing Free →
          </Link>
          <a href="#features" className={styles.ctaSecondary}>
            Explore Features
          </a>
        </div>
      </section>

      {/* ── Features ── */}
      <section id="features" className={styles.features}>
        <p className={styles.sectionLabel}>Features</p>
        <h2 className={styles.sectionTitle}>
          Everything you need to test at scale
        </h2>
        <p className={styles.sectionSubtitle}>
          From simple load tests to complex multi-step scenarios, StormAPI
          gives you the tools to ensure your APIs perform under pressure.
        </p>
        <div className={styles.featureGrid}>
          {features.map((f) => (
            <div key={f.title} className={styles.featureCard}>
              <div
                className={`${styles.featureIcon} ${styles[f.colorClass]}`}
              >
                <f.icon size={24} />
              </div>
              <h3 className={styles.featureTitle}>{f.title}</h3>
              <p className={styles.featureDesc}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className={styles.footer}>
        <p>© {new Date().getFullYear()} StormAPI. Built for developers.</p>
      </footer>
    </div>
  );
}
