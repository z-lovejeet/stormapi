import { useState, useEffect } from 'react';
import axios from 'axios';

interface HealthResponse {
  status: string;
  components: {
    db: { status: string; details: { database: string } };
    diskSpace: { status: string };
    ping: { status: string };
  };
}

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios
      .get<HealthResponse>('/actuator/health')
      .then((res) => {
        setHealth(res.data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, []);

  return (
    <div className="app">
      <div className="card">
        <div className="logo">⚡</div>
        <h1>StormAPI</h1>
        <p className="subtitle">API Performance Testing Platform</p>

        <div className="status-section">
          <h2>System Status</h2>
          {loading && <p className="loading">Connecting to backend...</p>}
          {error && (
            <div className="status-item error">
              <span className="dot red" />
              <span>Backend Offline — {error}</span>
            </div>
          )}
          {health && (
            <>
              <div className="status-item">
                <span className={`dot ${health.status === 'UP' ? 'green' : 'red'}`} />
                <span>Backend: {health.status}</span>
              </div>
              <div className="status-item">
                <span className={`dot ${health.components.db.status === 'UP' ? 'green' : 'red'}`} />
                <span>Database: {health.components.db.details.database} — {health.components.db.status}</span>
              </div>
              <div className="status-item">
                <span className={`dot ${health.components.ping.status === 'UP' ? 'green' : 'red'}`} />
                <span>Ping: {health.components.ping.status}</span>
              </div>
            </>
          )}
        </div>

        <p className="phase-tag">Phase 1 Complete ✓</p>
      </div>
    </div>
  );
}

export default App;
