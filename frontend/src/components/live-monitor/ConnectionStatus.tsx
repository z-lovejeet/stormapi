import { memo } from 'react';
import type { WebSocketConnectionState } from '../../types/websocket';

const STATE_CONFIG: Record<WebSocketConnectionState, { color: string; label: string }> = {
  CONNECTING: { color: 'var(--storm-warning)', label: 'Connecting…' },
  CONNECTED: { color: 'var(--storm-success)', label: 'Connected' },
  DISCONNECTED: { color: 'var(--storm-text-tertiary)', label: 'Disconnected' },
  ERROR: { color: 'var(--storm-error)', label: 'Error' },
};

interface ConnectionStatusProps {
  state: WebSocketConnectionState;
}

export const ConnectionStatus = memo(function ConnectionStatus({ state }: ConnectionStatusProps) {
  const config = STATE_CONFIG[state];

  return (
    <div
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '6px',
        fontSize: 'var(--storm-text-xs)',
        color: config.color,
        fontWeight: 'var(--storm-weight-medium)',
      }}
      role="status"
      aria-label={`WebSocket ${config.label}`}
    >
      <span
        style={{
          width: 8,
          height: 8,
          borderRadius: '50%',
          background: config.color,
          animation: state === 'CONNECTING' ? 'pulse 1.5s ease-in-out infinite' : undefined,
        }}
      />
      {config.label}
    </div>
  );
});
