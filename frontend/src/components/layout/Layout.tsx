import { Outlet } from 'react-router-dom';
import { Navbar } from './Navbar';
import styles from './Layout.module.css';

/**
 * Root application layout — top navbar + scrollable content outlet.
 */
export function Layout() {
  return (
    <div className={styles.layout}>
      <Navbar />
      <main className={styles.content} id="main-content">
        <Outlet />
      </main>
    </div>
  );
}
