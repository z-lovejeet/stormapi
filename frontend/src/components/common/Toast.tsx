import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { X, CheckCircle, AlertCircle, Info } from 'lucide-react';

// ── Types ─────────────────────────────────────────────────

type ToastType = 'success' | 'error' | 'info';

interface Toast {
  id: string;
  type: ToastType;
  message: string;
}

interface ToastContextValue {
  showToast: (type: ToastType, message: string, duration?: number) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

export function useToast(): ToastContextValue {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
}

// ── Config ────────────────────────────────────────────────

const ICONS: Record<ToastType, typeof CheckCircle> = {
  success: CheckCircle,
  error: AlertCircle,
  info: Info,
};

const COLORS: Record<ToastType, { bg: string; border: string; color: string }> = {
  success: {
    bg: 'var(--storm-success-subtle)',
    border: 'var(--storm-success)',
    color: 'var(--storm-success)',
  },
  error: {
    bg: 'var(--storm-error-subtle)',
    border: 'var(--storm-error)',
    color: 'var(--storm-error)',
  },
  info: {
    bg: 'var(--storm-info-subtle)',
    border: 'var(--storm-info)',
    color: 'var(--storm-info)',
  },
};

// ── Provider ──────────────────────────────────────────────

export function ToastProvider({ children }: { children: ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const showToast = useCallback((type: ToastType, message: string, duration = 4000) => {
    const id = crypto.randomUUID();
    setToasts((prev) => [...prev, { id, type, message }]);
    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, duration);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ showToast }}>
      {children}
      {createPortal(
        <div
          style={{
            position: 'fixed',
            top: 16,
            right: 16,
            display: 'flex',
            flexDirection: 'column',
            gap: 8,
            zIndex: 9999,
            pointerEvents: 'none',
          }}
        >
          <AnimatePresence>
            {toasts.map((toast) => {
              const Icon = ICONS[toast.type];
              const colors = COLORS[toast.type];
              return (
                <motion.div
                  key={toast.id}
                  role="alert"
                  initial={{ opacity: 0, x: 50, scale: 0.95 }}
                  animate={{ opacity: 1, x: 0, scale: 1 }}
                  exit={{ opacity: 0, x: 50, scale: 0.95 }}
                  transition={{ duration: 0.2 }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                    padding: '10px 14px',
                    background: colors.bg,
                    border: `1px solid ${colors.border}`,
                    borderRadius: 'var(--storm-radius-md)',
                    boxShadow: 'var(--storm-shadow-lg)',
                    fontSize: 'var(--storm-text-sm)',
                    color: 'var(--storm-text-primary)',
                    pointerEvents: 'auto',
                    maxWidth: 360,
                  }}
                >
                  <Icon size={18} style={{ color: colors.color, flexShrink: 0 }} />
                  <span style={{ flex: 1 }}>{toast.message}</span>
                  <button
                    onClick={() => removeToast(toast.id)}
                    aria-label="Dismiss"
                    type="button"
                    style={{
                      display: 'flex',
                      background: 'none',
                      border: 'none',
                      color: 'var(--storm-text-tertiary)',
                      cursor: 'pointer',
                      padding: 2,
                      flexShrink: 0,
                    }}
                  >
                    <X size={14} />
                  </button>
                </motion.div>
              );
            })}
          </AnimatePresence>
        </div>,
        document.body,
      )}
    </ToastContext.Provider>
  );
}
