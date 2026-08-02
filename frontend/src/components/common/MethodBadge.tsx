import { HttpMethod } from '../../types/test';

const METHOD_COLORS: Record<HttpMethod, string> = {
  [HttpMethod.GET]: 'var(--storm-method-get)',
  [HttpMethod.POST]: 'var(--storm-method-post)',
  [HttpMethod.PUT]: 'var(--storm-method-put)',
  [HttpMethod.DELETE]: 'var(--storm-method-delete)',
  [HttpMethod.PATCH]: 'var(--storm-method-patch)',
  [HttpMethod.HEAD]: 'var(--storm-method-head)',
  [HttpMethod.OPTIONS]: 'var(--storm-method-options)',
};

interface MethodBadgeProps {
  method: HttpMethod;
}

export function MethodBadge({ method }: MethodBadgeProps) {
  const color = METHOD_COLORS[method];

  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 'var(--storm-radius-full)',
        fontSize: 'var(--storm-text-xs)',
        fontWeight: 'var(--storm-weight-bold)',
        fontFamily: 'var(--storm-font-mono)',
        color,
        background: `color-mix(in srgb, ${color} 14%, transparent)`,
        backdropFilter: 'blur(8px)',
        WebkitBackdropFilter: 'blur(8px)',
        border: `1px solid color-mix(in srgb, ${color} 30%, transparent)`,
        lineHeight: '1.6',
      }}
    >
      {method}
    </span>
  );
}
