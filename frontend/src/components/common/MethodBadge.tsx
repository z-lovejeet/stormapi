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
        padding: '1px 8px',
        borderRadius: 'var(--storm-radius-sm)',
        fontSize: 'var(--storm-text-xs)',
        fontWeight: 'var(--storm-weight-bold)',
        fontFamily: 'var(--storm-font-mono)',
        color,
        background: `color-mix(in srgb, ${color} 12%, transparent)`,
        lineHeight: '1.6',
      }}
    >
      {method}
    </span>
  );
}
