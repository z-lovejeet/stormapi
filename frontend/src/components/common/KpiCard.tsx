import { memo } from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { motion } from 'framer-motion';
import styles from './KpiCard.module.css';

interface KpiCardProps {
  icon: LucideIcon;
  label: string;
  value: string;
  trend?: number;
  trendDirection?: 'up' | 'down' | 'neutral';
  loading?: boolean;
}

export const KpiCard = memo(function KpiCard({
  icon: Icon,
  label,
  value,
  trend,
  trendDirection = 'neutral',
  loading = false,
}: KpiCardProps) {
  const TrendIcon = trendDirection === 'up' ? TrendingUp : trendDirection === 'down' ? TrendingDown : Minus;
  const trendClass = trendDirection === 'up' ? styles.trendUp : trendDirection === 'down' ? styles.trendDown : styles.trendNeutral;

  return (
    <motion.div
      className={styles.card}
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      <div className={styles.iconWrapper}>
        <Icon size={22} />
      </div>
      <div className={styles.content}>
        <div className={styles.label}>{label}</div>
        {loading ? (
          <div className={styles.loading} />
        ) : (
          <>
            <div className={styles.value}>{value}</div>
            {trend !== undefined && (
              <div className={`${styles.trend} ${trendClass}`}>
                <TrendIcon size={12} />
                <span>{Math.abs(trend).toFixed(1)}%</span>
              </div>
            )}
          </>
        )}
      </div>
    </motion.div>
  );
});
