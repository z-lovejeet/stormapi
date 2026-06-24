import { forwardRef, type SelectHTMLAttributes, useId } from 'react';
import { ChevronDown } from 'lucide-react';

interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps extends Omit<SelectHTMLAttributes<HTMLSelectElement>, 'children'> {
  label?: string;
  options: SelectOption[];
  error?: string;
  placeholder?: string;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { label, options, error, placeholder, id: providedId, ...rest },
  ref,
) {
  const generatedId = useId();
  const selectId = providedId || generatedId;
  const errorId = error ? `${selectId}-error` : undefined;

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--storm-space-1)' }}>
      {label && (
        <label
          htmlFor={selectId}
          style={{
            fontSize: 'var(--storm-text-sm)',
            fontWeight: 'var(--storm-weight-medium)',
            color: 'var(--storm-text-primary)',
          }}
        >
          {label}
        </label>
      )}
      <div style={{ position: 'relative' }}>
        <select
          ref={ref}
          id={selectId}
          aria-invalid={!!error}
          aria-describedby={errorId}
          style={{
            width: '100%',
            padding: '8px 32px 8px 12px',
            fontFamily: 'var(--storm-font-sans)',
            fontSize: 'var(--storm-text-sm)',
            color: 'var(--storm-text-primary)',
            background: 'var(--storm-bg-tertiary)',
            border: `1px solid ${error ? 'var(--storm-error)' : 'var(--storm-border-primary)'}`,
            borderRadius: 'var(--storm-radius-md)',
            appearance: 'none',
            outline: 'none',
            cursor: 'pointer',
          }}
          {...rest}
        >
          {placeholder && (
            <option value="" disabled>
              {placeholder}
            </option>
          )}
          {options.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
        <ChevronDown
          size={16}
          style={{
            position: 'absolute',
            right: 10,
            top: '50%',
            transform: 'translateY(-50%)',
            color: 'var(--storm-text-tertiary)',
            pointerEvents: 'none',
          }}
        />
      </div>
      {error && (
        <span id={errorId} role="alert" style={{ fontSize: 'var(--storm-text-xs)', color: 'var(--storm-error)' }}>
          {error}
        </span>
      )}
    </div>
  );
});
