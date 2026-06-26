import { memo, useRef, useEffect } from 'react';
import { Pause, Play } from 'lucide-react';
import { Button } from '../common/Button';
import { formatMs, formatBytes } from '../../utils/formatters';
import type { RequestLogEntry } from '../../types/websocket';
import styles from './RequestLogTable.module.css';

interface RequestLogTableProps {
  entries: RequestLogEntry[];
  paused: boolean;
  onTogglePause: () => void;
}

function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleTimeString(undefined, {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
}

export const RequestLogTable = memo(function RequestLogTable({
  entries,
  paused,
  onTogglePause,
}: RequestLogTableProps) {
  const scrollRef = useRef<HTMLDivElement>(null);
  const displayEntries = entries.slice(-50).reverse();

  useEffect(() => {
    if (!paused && scrollRef.current) {
      scrollRef.current.scrollTop = 0;
    }
  }, [entries.length, paused]);

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>Request Log ({entries.length})</span>
        <Button
          variant="ghost"
          size="sm"
          icon={paused ? Play : Pause}
          onClick={onTogglePause}
          type="button"
          aria-label={paused ? 'Resume auto-scroll' : 'Pause auto-scroll'}
        >
          {paused ? 'Resume' : 'Pause'}
        </Button>
      </div>
      <div className={styles.scrollArea} ref={scrollRef}>
        <table className={styles.table} aria-label="Request log">
          <thead>
            <tr>
              <th>Time</th>
              <th>Method</th>
              <th>Status</th>
              <th>Response</th>
              <th>Size</th>
            </tr>
          </thead>
          <tbody>
            {displayEntries.length === 0 ? (
              <tr>
                <td colSpan={5} className={styles.emptyRow}>
                  No requests yet…
                </td>
              </tr>
            ) : (
              displayEntries.map((entry, i) => (
                <tr key={`${entry.timestamp}-${i}`}>
                  <td>{formatTimestamp(entry.timestamp)}</td>
                  <td>{entry.method}</td>
                  <td className={entry.success ? styles.statusSuccess : styles.statusError}>
                    {entry.statusCode}
                  </td>
                  <td>{formatMs(entry.responseTimeMs)}</td>
                  <td>{formatBytes(entry.responseSize)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
});
