import { useEffect, useRef, type RefObject } from 'react';
import { Link, Navigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import {
  Zap,
  BarChart3,
  Globe,
  Shield,
  Database,
  GitBranch,
  ArrowRight,
  Activity,
  Terminal,
} from 'lucide-react';
import styles from './LandingPage.module.css';

/* ── Scroll Reveal Hook ─────────────────────────────────── */

function useScrollReveal(): RefObject<HTMLDivElement | null> {
  const ref = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting && styles.visible) {
            entry.target.classList.add(styles.visible);
          }
        });
      },
      { threshold: 0.15, rootMargin: '0px 0px -60px 0px' }
    );

    // Observe the container + all children with .reveal
    const revealElements = el.querySelectorAll(`.${styles.reveal}`);
    revealElements.forEach((child) => observer.observe(child));
    observer.observe(el);

    return () => observer.disconnect();
  }, []);

  return ref;
}

/* ── Data ────────────────────────────────────────────────── */

const features = [
  {
    icon: Zap,
    colorClass: 'featureIconBlue',
    title: 'Load & Stress Testing',
    desc: 'Simulate thousands of concurrent users with configurable virtual users, ramp-up strategies, and duration settings.',
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

const steps = [
  {
    number: '1',
    numberClass: 'stepNumber1',
    title: 'Configure Your Test',
    desc: 'Set your target URL, choose test type, define virtual users, and configure ramp-up strategies.',
  },
  {
    number: '2',
    numberClass: 'stepNumber2',
    title: 'Run & Monitor',
    desc: 'Execute your test and monitor real-time metrics — throughput, latency, errors, and active users.',
  },
  {
    number: '3',
    numberClass: 'stepNumber3',
    title: 'Analyze & Export',
    desc: 'Review detailed results with charts, percentiles, and status breakdowns. Export or compare runs.',
  },
];

const techStack = [
  { icon: '☕', name: 'Java 21' },
  { icon: '🍃', name: 'Spring Boot' },
  { icon: '⚛️', name: 'React' },
  { icon: '📊', name: 'Recharts' },
  { icon: '🔒', name: 'OAuth2' },
  { icon: '🧵', name: 'Virtual Threads' },
];

/* ── Component ───────────────────────────────────────────── */

/**
 * Public landing page — hero section, feature grid, how it works, and CTAs.
 * Redirects authenticated users to dashboard.
 */
export function LandingPage() {
  const { isAuthenticated, loading } = useAuth();
  const featuresRef = useScrollReveal();
  const stepsRef = useScrollReveal();
  const ctaRef = useScrollReveal();

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
        <ul className={styles.landingNavLinks}>
          <li><a href="#features" className={styles.navLink}>Features</a></li>
          <li><a href="#how-it-works" className={styles.navLink}>How It Works</a></li>
          <li><a href="#tech" className={styles.navLink}>Tech Stack</a></li>
        </ul>
        <div className={styles.landingNavActions}>
          <Link to="/login" className={styles.loginBtn}>
            Log in
          </Link>
          <Link to="/register" className={styles.getStartedBtn}>
            Get Started
          </Link>
        </div>
      </nav>

      {/* ── Hero ── */}
      <section className={styles.hero}>
        {/* Animated background orbs */}
        <div className={`${styles.heroOrb} ${styles.heroOrb1}`} aria-hidden="true" />
        <div className={`${styles.heroOrb} ${styles.heroOrb2}`} aria-hidden="true" />
        <div className={`${styles.heroOrb} ${styles.heroOrb3}`} aria-hidden="true" />

        <div className={styles.heroBadge}>
          <Activity size={14} />
          API Performance Testing Platform
        </div>
        <h1 className={styles.heroTitle}>
          <span className={styles.heroTitleLine1}>Break your APIs</span>
          <span className={styles.heroTitleLine2}>before your users do</span>
        </h1>
        <p className={styles.heroSubtitle}>
          StormAPI is a modern load testing platform built with Java 21 virtual threads.
          Build scenarios, simulate real traffic patterns, and identify bottlenecks — all from your browser.
        </p>
        <div className={styles.heroCtas}>
          <Link to="/register" className={styles.ctaPrimary}>
            Start Testing Free
            <ArrowRight size={18} />
          </Link>
          <a href="#features" className={styles.ctaSecondary}>
            <Terminal size={18} />
            Explore Features
          </a>
        </div>
      </section>

      {/* ── Metrics Strip ── */}
      <div className={styles.metricsStrip}>
        <div className={styles.metricItem}>
          <div className={styles.metricValue}>100K+</div>
          <div className={styles.metricLabel}>Virtual Users</div>
        </div>
        <div className={styles.metricItem}>
          <div className={styles.metricValue}>7</div>
          <div className={styles.metricLabel}>Test Types</div>
        </div>
        <div className={styles.metricItem}>
          <div className={styles.metricValue}>&lt;1ms</div>
          <div className={styles.metricLabel}>Overhead</div>
        </div>
        <div className={styles.metricItem}>
          <div className={styles.metricValue}>P99</div>
          <div className={styles.metricLabel}>Percentiles</div>
        </div>
      </div>

      {/* ── Features ── */}
      <section id="features" className={styles.section} ref={featuresRef}>
        <p className={`${styles.sectionLabel} ${styles.reveal}`}>Features</p>
        <h2 className={`${styles.sectionTitle} ${styles.reveal}`}>
          Everything you need to test at scale
        </h2>
        <p className={`${styles.sectionSubtitle} ${styles.reveal}`}>
          From simple load tests to complex multi-step scenarios, StormAPI
          gives you the tools to ensure your APIs perform under pressure.
        </p>
        <div className={styles.featureGrid}>
          {features.map((f) => (
            <div key={f.title} className={`${styles.featureCard} ${styles.reveal}`}>
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

      {/* ── How It Works ── */}
      <section id="how-it-works" className={styles.section} ref={stepsRef}>
        <p className={`${styles.sectionLabel} ${styles.reveal}`}>How It Works</p>
        <h2 className={`${styles.sectionTitle} ${styles.reveal}`}>
          Three steps to performance clarity
        </h2>
        <p className={`${styles.sectionSubtitle} ${styles.reveal}`}>
          Go from zero to actionable performance insights in minutes, not hours.
        </p>
        <div className={`${styles.stepsGrid} ${styles.reveal}`}>
          {steps.map((s) => (
            <div key={s.number} className={styles.stepCard}>
              <div className={`${styles.stepNumber} ${styles[s.numberClass]}`}>
                {s.number}
              </div>
              <h3 className={styles.stepTitle}>{s.title}</h3>
              <p className={styles.stepDesc}>{s.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── Tech Stack ── */}
      <section id="tech" className={styles.techSection}>
        <p className={styles.sectionLabel}>Built With</p>
        <div className={styles.techGrid}>
          {techStack.map((t) => (
            <div key={t.name} className={styles.techItem}>
              <span className={styles.techIcon}>{t.icon}</span>
              <span className={styles.techName}>{t.name}</span>
            </div>
          ))}
        </div>
      </section>

      {/* ── CTA Section ── */}
      <section className={styles.ctaSection} ref={ctaRef}>
        <div className={`${styles.ctaBox} ${styles.reveal}`}>
          <h2 className={styles.ctaTitle}>
            Ready to stress test your APIs?
          </h2>
          <p className={styles.ctaSubtitle}>
            Create a free account and run your first load test in under 2 minutes.
          </p>
          <Link to="/register" className={styles.ctaPrimary}>
            Get Started for Free
            <ArrowRight size={18} />
          </Link>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className={styles.footer}>
        <div className={styles.footerInner}>
          <span className={styles.footerCopyright}>
            © {new Date().getFullYear()} StormAPI. Built for developers.
          </span>
          <div className={styles.footerLinks}>
            <a href="#features" className={styles.footerLink}>Features</a>
            <a href="#how-it-works" className={styles.footerLink}>How It Works</a>
            <a href="https://github.com/z-lovejeet/stormapi" className={styles.footerLink} target="_blank" rel="noopener noreferrer">GitHub</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
