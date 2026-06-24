import { forwardRef, type ButtonHTMLAttributes } from 'react';
import type { LucideIcon } from 'lucide-react';
import { LoadingSpinner } from './LoadingSpinner';
import styles from './Button.module.css';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  icon?: LucideIcon;
  iconOnly?: boolean;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    variant = 'primary',
    size = 'md',
    loading = false,
    icon: Icon,
    iconOnly = false,
    disabled,
    children,
    className = '',
    ...rest
  },
  ref,
) {
  const iconSize = size === 'sm' ? 14 : size === 'lg' ? 18 : 16;

  return (
    <button
      ref={ref}
      className={`${styles.button} ${styles[variant]} ${styles[size]} ${iconOnly ? styles.iconOnly : ''} ${className}`}
      disabled={disabled || loading}
      {...rest}
    >
      {loading ? (
        <LoadingSpinner size="sm" />
      ) : (
        <>
          {Icon && <Icon size={iconSize} />}
          {!iconOnly && children}
        </>
      )}
    </button>
  );
});
