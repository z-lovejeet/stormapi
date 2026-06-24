import { Routes, Route, Navigate } from 'react-router-dom';
import { Layout } from './components/layout/Layout';
import { DashboardPage } from './pages/DashboardPage';
import { TestBuilderPage } from './pages/TestBuilderPage';
import { LiveMonitorPage } from './pages/LiveMonitorPage';
import { TestResultPage } from './pages/TestResultPage';
import { HistoryPage } from './pages/HistoryPage';
import { CollectionsPage } from './pages/CollectionsPage';
import { SettingsPage } from './pages/SettingsPage';

function App() {
  return (
    <>
      <a href="#main-content" className="skip-link">
        Skip to main content
      </a>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="tests/new" element={<TestBuilderPage />} />
          <Route path="tests/:id/live" element={<LiveMonitorPage />} />
          <Route path="tests/:id/result" element={<TestResultPage />} />
          <Route path="history" element={<HistoryPage />} />
          <Route path="collections" element={<CollectionsPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Routes>
    </>
  );
}

export default App;
