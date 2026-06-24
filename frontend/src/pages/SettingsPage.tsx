import { Settings } from 'lucide-react';
import { useTheme } from '../hooks/useTheme';
import { Button } from '../components/common/Button';
import { Sun, Moon } from 'lucide-react';

export function SettingsPage() {
  const { theme, toggleTheme } = useTheme();

  return (
    <div>
      <h1 style={{ fontSize: 'var(--storm-text-2xl)', marginBottom: 'var(--storm-space-6)' }}>
        Settings
      </h1>

      <div
        style={{
          background: 'var(--storm-bg-secondary)',
          border: '1px solid var(--storm-border-primary)',
          borderRadius: 'var(--storm-radius-lg)',
          padding: 'var(--storm-space-6)',
        }}
      >
        <h2 style={{ fontSize: 'var(--storm-text-lg)', marginBottom: 'var(--storm-space-4)' }}>
          Appearance
        </h2>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <p style={{ fontWeight: 'var(--storm-weight-medium)' }}>Theme</p>
            <p style={{ fontSize: 'var(--storm-text-sm)', color: 'var(--storm-text-secondary)' }}>
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

      <div
        style={{
          background: 'var(--storm-bg-secondary)',
          border: '1px solid var(--storm-border-primary)',
          borderRadius: 'var(--storm-radius-lg)',
          padding: 'var(--storm-space-6)',
          marginTop: 'var(--storm-space-4)',
        }}
      >
        <h2 style={{ fontSize: 'var(--storm-text-lg)', marginBottom: 'var(--storm-space-4)' }}>
          About
        </h2>
        <p style={{ fontSize: 'var(--storm-text-sm)', color: 'var(--storm-text-secondary)' }}>
          StormAPI v1.0.0 — API Performance Testing Platform
        </p>
        <p style={{ fontSize: 'var(--storm-text-sm)', color: 'var(--storm-text-tertiary)', marginTop: 'var(--storm-space-1)' }}>
          Built with Java 21 + Spring Boot 3.4 + React 19 + Virtual Threads
        </p>
      </div>
    </div>
  );
}
