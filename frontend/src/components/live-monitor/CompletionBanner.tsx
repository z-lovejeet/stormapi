import { memo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { CheckCircle, XCircle, AlertTriangle, BarChart3 } from 'lucide-react';
import { Button } from '../common/Button';
import styles from './CompletionBanner.module.css';

type CompletionStatus = 'completed' | 'failed' | 'cancelled';

interface CompletionBannerProps {
  testId: number;
  status: CompletionStatus;
  visible: boolean;
}

const STATUS_CONFIG: Record<CompletionStatus, {
  icon: typeof CheckCircle;
  color: string;
  title: string;
  subtitle: string;
  className: string;
}> = {
  completed: {
    icon: CheckCircle,
    color: 'var(--storm-success)',
    title: '🎉 Test Completed!',
    subtitle: 'All results have been collected and saved.',
    className: styles.completed,
  },
  failed: {
    icon: XCircle,
    color: 'var(--storm-error)',
    title: 'Test Failed',
    subtitle: 'The test encountered an error. Partial results may be available.',
    className: styles.failed,
  },
  cancelled: {
    icon: AlertTriangle,
    color: 'var(--storm-warning)',
    title: 'Test Stopped',
    subtitle: 'The test was stopped. Results collected so far have been saved.',
    className: styles.cancelled,
  },
};

export const CompletionBanner = memo(function CompletionBanner({
  testId,
  status,
  visible,
}: CompletionBannerProps) {
  const navigate = useNavigate();
  const config = STATUS_CONFIG[status];
  const Icon = config.icon;

  return (
    <AnimatePresence>
      {visible && (
        <motion.div
          className={`${styles.banner} ${config.className}`}
          initial={{ opacity: 0, y: -20, scale: 0.97 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: -20, scale: 0.97 }}
          transition={{ type: 'spring', stiffness: 120, damping: 14 }}
          role="alert"
        >
          <div className={styles.content}>
            <Icon size={28} color={config.color} className={styles.icon} />
            <div className={styles.message}>
              <span className={styles.title}>{config.title}</span>
              <span className={styles.subtitle}>{config.subtitle}</span>
            </div>
          </div>
          <div className={styles.actions}>
            <Button
              icon={BarChart3}
              onClick={() => navigate(`/tests/${testId}/result`)}
            >
              View Results
            </Button>
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
});
