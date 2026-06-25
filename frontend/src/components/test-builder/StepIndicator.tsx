import { Check } from 'lucide-react';
import styles from './StepIndicator.module.css';

interface Step {
  label: string;
}

interface StepIndicatorProps {
  steps: readonly Step[];
  currentStep: number;
  onStepClick?: (index: number) => void;
}

export function StepIndicator({ steps, currentStep, onStepClick }: StepIndicatorProps) {
  return (
    <div className={styles.container} role="navigation" aria-label="Wizard steps">
      {steps.map((step, i) => {
        const isActive = i === currentStep;
        const isCompleted = i < currentStep;
        const isClickable = isCompleted && !!onStepClick;

        return (
          <div className={styles.step} key={step.label}>
            {i > 0 && (
              <div
                className={`${styles.connector} ${isCompleted ? styles.completedConnector : ''}`}
              />
            )}
            <button
              type="button"
              className={`${styles.stepButton} ${isClickable ? styles.clickable : ''}`}
              onClick={isClickable ? () => onStepClick(i) : undefined}
              aria-current={isActive ? 'step' : undefined}
              aria-label={`Step ${i + 1}: ${step.label}${isCompleted ? ' (completed)' : ''}`}
              tabIndex={isClickable ? 0 : -1}
            >
              <div
                className={`${styles.circle} ${isActive ? styles.active : ''} ${isCompleted ? styles.completed : ''}`}
              >
                {isCompleted ? <Check size={16} /> : i + 1}
              </div>
              <span
                className={`${styles.label} ${isActive ? styles.activeLabel : ''} ${isCompleted ? styles.completedLabel : ''}`}
              >
                {step.label}
              </span>
            </button>
          </div>
        );
      })}
    </div>
  );
}
