import { useEffect, useRef, useState, useCallback } from 'react';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type {
  LiveMetricsMessage,
  RequestLogMessage,
  RequestLogEntry,
  TestEventMessage,
  WebSocketConnectionState,
  UseWebSocketOptions,
  UseWebSocketReturn,
} from '../types/websocket';

/**
 * React hook for subscribing to real-time WebSocket streams for a specific test.
 *
 * Subscribes to three STOMP destinations:
 *   - /topic/metrics/{testId}  → live metrics snapshots (~1/s)
 *   - /topic/logs/{testId}     → batched request logs (~1/s)
 *   - /topic/events/{testId}   → lifecycle events (start, progress, complete)
 *
 * Features:
 *   - Auto-reconnect with exponential backoff (2s, 4s, 8s, max 30s)
 *   - Clean subscription teardown on unmount or testId change
 *   - Rolling log window (last 200 entries)
 *   - Callback-based API for parent component integration
 *
 * @param options.testId     The test config ID to subscribe to
 * @param options.enabled    Whether to connect (default: true)
 * @param options.onMetrics  Callback for each metrics snapshot
 * @param options.onLogBatch Callback for each log batch
 * @param options.onEvent    Callback for each lifecycle event
 */
export function useWebSocket(options: UseWebSocketOptions): UseWebSocketReturn {
  const {
    testId,
    enabled = true,
    onMetrics,
    onLogBatch,
    onEvent,
    onConnectionChange,
  } = options;

  const [connectionState, setConnectionState] = useState<WebSocketConnectionState>('DISCONNECTED');
  const [latestMetrics, setLatestMetrics] = useState<LiveMetricsMessage | null>(null);
  const [latestLogs, setLatestLogs] = useState<RequestLogEntry[]>([]);
  const [latestEvent, setLatestEvent] = useState<TestEventMessage | null>(null);

  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<StompSubscription[]>([]);
  const maxLogEntries = 200;

  const updateConnectionState = useCallback(
    (state: WebSocketConnectionState) => {
      setConnectionState(state);
      onConnectionChange?.(state);
    },
    [onConnectionChange]
  );

  const disconnect = useCallback(() => {
    // Unsubscribe from all topics
    subscriptionsRef.current.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch {
        // Ignore unsubscribe errors during teardown
      }
    });
    subscriptionsRef.current = [];

    // Deactivate STOMP client
    if (clientRef.current?.active) {
      clientRef.current.deactivate();
    }
    clientRef.current = null;
    updateConnectionState('DISCONNECTED');
  }, [updateConnectionState]);

  useEffect(() => {
    if (!enabled || !testId) {
      disconnect();
      return;
    }

    updateConnectionState('CONNECTING');

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 2000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,

      onConnect: () => {
        updateConnectionState('CONNECTED');

        // Subscribe to metrics
        const metricsSub = client.subscribe(
          `/topic/metrics/${testId}`,
          (message: IMessage) => {
            try {
              const metrics: LiveMetricsMessage = JSON.parse(message.body);
              setLatestMetrics(metrics);
              onMetrics?.(metrics);
            } catch {
              console.warn('Failed to parse metrics message');
            }
          }
        );

        // Subscribe to logs
        const logsSub = client.subscribe(
          `/topic/logs/${testId}`,
          (message: IMessage) => {
            try {
              const logBatch: RequestLogMessage = JSON.parse(message.body);
              setLatestLogs((prev) => {
                const updated = [...prev, ...logBatch.entries];
                return updated.slice(-maxLogEntries);
              });
              onLogBatch?.(logBatch);
            } catch {
              console.warn('Failed to parse log message');
            }
          }
        );

        // Subscribe to events
        const eventsSub = client.subscribe(
          `/topic/events/${testId}`,
          (message: IMessage) => {
            try {
              const event: TestEventMessage = JSON.parse(message.body);
              setLatestEvent(event);
              onEvent?.(event);
            } catch {
              console.warn('Failed to parse event message');
            }
          }
        );

        subscriptionsRef.current = [metricsSub, logsSub, eventsSub];
      },

      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message']);
        updateConnectionState('ERROR');
      },

      onWebSocketError: () => {
        updateConnectionState('ERROR');
      },

      onDisconnect: () => {
        updateConnectionState('DISCONNECTED');
      },
    });

    clientRef.current = client;
    client.activate();

    // Cleanup on unmount or testId change
    return () => {
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [testId, enabled]);

  return {
    connectionState,
    latestMetrics,
    latestLogs,
    latestEvent,
    disconnect,
  };
}
