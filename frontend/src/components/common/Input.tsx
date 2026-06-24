import { forwardRef, type InputHTMLAttributes, useId } from 'react';
import type { LucideIcon } from 'lucide-react';
import styles from './Input.module.css';

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  icon?: LucideIcon;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, error, helperText, icon: Icon, className = '', id: providedId, ...rest },
  ref,
) {
  const generatedId = useId();
  const inputId = providedId || generatedId;
  const errorId = error ? `${inputId}-error` : undefined;

  return (
    <div className={styles.wrapper}>
      {label && (
        <label htmlFor={inputId} className={styles.label}>
          {label}
        </label>
      )}
      <div className={styles.inputWrapper}>
        {Icon && <Icon size={16} className={styles.icon} />}
        <input
          ref={ref}
          id={inputId}
          className={`${styles.input} ${Icon ? styles.hasIcon : ''} ${error ? styles.inputError : ''} ${className}`}
          aria-invalid={!!error}
          aria-describedby={errorId}
          {...rest}
        />
      </div>
      {error && (
        <span id={errorId} className={styles.error} role="alert">
          {error}
        </span>
      )}
      {!error && helperText && <span className={styles.helperText}>{helperText}</span>}
    </div>
  );
});
