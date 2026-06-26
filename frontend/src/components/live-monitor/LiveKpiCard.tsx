import { memo, useEffect, useRef, useState } from 'react';
import { animate } from 'framer-motion';
import type { LucideIcon } from 'lucide-react';
import { KpiCard } from '../common/KpiCard';

interface LiveKpiCardProps {
  icon: LucideIcon;
  label: string;
  value: number;
  formatter: (n: number) => string;
  loading?: boolean;
}

export const LiveKpiCard = memo(function LiveKpiCard({
  icon,
  label,
  value,
  formatter,
  loading = false,
}: LiveKpiCardProps) {
  const prevValue = useRef(value);
  const [displayValue, setDisplayValue] = useState(formatter(value));

  useEffect(() => {
    if (loading) return;

    const from = prevValue.current;
    const to = value;
    prevValue.current = value;

    if (from === to) {
      setDisplayValue(formatter(to));
      return;
    }

    const controls = animate(from, to, {
      duration: 0.3,
      onUpdate: (v) => setDisplayValue(formatter(v)),
      onComplete: () => setDisplayValue(formatter(to)),
    });

    return () => controls.stop();
  }, [value, formatter, loading]);

  return (
    <KpiCard
      icon={icon}
      label={label}
      value={displayValue}
      loading={loading}
    />
  );
});
