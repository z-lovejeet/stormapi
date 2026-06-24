import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import styles from './Layout.module.css';

/**
 * Root application layout — sidebar + header + scrollable content outlet.
 */
export function Layout() {
  return (
    <div className={styles.layout}>
      <Sidebar />
      <div className={styles.main}>
        <Header />
        <main className={styles.content} id="main-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
