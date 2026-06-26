import { Zap, TrendingUp, Activity, Timer, Target, BarChart3 } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { TestType } from '../../types/test';
import styles from './TestTypeSelector.module.css';

interface TypeCard {
  type: TestType;
  icon: LucideIcon;
  title: string;
  description: string;
  color: string;
}

const TYPE_CARDS: TypeCard[] = [
  { type: TestType.LOAD, icon: Zap, title: 'Load Test', description: 'Constant virtual users for set duration — baseline performance', color: 'var(--storm-chart-1)' },
  { type: TestType.STRESS, icon: TrendingUp, title: 'Stress Test', description: 'Incrementally increase load — find degradation point', color: 'var(--storm-chart-2)' },
  { type: TestType.SPIKE, icon: Activity, title: 'Spike Test', description: 'Sudden traffic spike — test recovery capability', color: 'var(--storm-chart-3)' },
  { type: TestType.SOAK, icon: Timer, title: 'Soak Test', description: 'Extended duration — detect memory leaks and degradation', color: 'var(--storm-chart-4)' },
  { type: TestType.BREAKPOINT, icon: Target, title: 'Breakpoint Test', description: 'Binary search for exact failure threshold', color: 'var(--storm-chart-5)' },
  { type: TestType.SCALABILITY, icon: BarChart3, title: 'Scalability Test', description: 'Measure throughput at predefined user steps', color: 'var(--storm-chart-6)' },
];

interface TestTypeSelectorProps {
  selectedType: TestType | undefined;
  onSelect: (type: TestType) => void;
}

export function TestTypeSelector({ selectedType, onSelect }: TestTypeSelectorProps) {
  return (
    <div className={styles.grid} role="group" aria-label="Step 2: Select Test Type">
      {TYPE_CARDS.map((card) => {
        const Icon = card.icon;
        const isSelected = selectedType === card.type;
        return (
          <div
            key={card.type}
            className={`${styles.card} ${isSelected ? styles.selected : ''}`}
            role="radio"
            aria-checked={isSelected}
            aria-label={card.title}
            tabIndex={0}
            onClick={() => onSelect(card.type)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onSelect(card.type);
              }
            }}
          >
            <div
              className={styles.iconWrapper}
              style={{ background: `color-mix(in srgb, ${card.color} 12%, transparent)` }}
            >
              <Icon size={24} style={{ color: card.color }} />
            </div>
            <span className={styles.title}>{card.title}</span>
            <span className={styles.description}>{card.description}</span>
          </div>
        );
      })}
    </div>
  );
}
