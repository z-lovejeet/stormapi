import React, { memo, useMemo } from 'react';
import styles from './DataPreviewTable.module.css';

interface DataPreviewTableProps {
  /** Parsed data rows. Each row is a key-value map. */
  rows: Record<string, string>[];
}

/**
 * Preview table for parsed data rows. Shows column headers and up to 50 rows.
 */
const DataPreviewTable: React.FC<DataPreviewTableProps> = ({ rows }) => {
  const columns = useMemo(() => {
    if (rows.length === 0 || !rows[0]) return [];
    return Object.keys(rows[0]);
  }, [rows]);

  const displayRows = useMemo(() => rows.slice(0, 50), [rows]);

  if (rows.length === 0) {
    return (
      <div className={styles.container}>
        <div className={styles.empty}>No data to preview. Upload or paste data above.</div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <span className={styles.title}>Data Preview</span>
        <span className={styles.count}>
          {rows.length} row{rows.length !== 1 ? 's' : ''}
          {rows.length > 50 ? ' (showing first 50)' : ''}
        </span>
      </div>
      <div className={styles.tableWrapper}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th className={styles.rowIndex}>#</th>
              {columns.map((col) => (
                <th key={col}>{col}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {displayRows.map((row, index) => (
              <tr key={index}>
                <td className={styles.rowIndex}>{index}</td>
                {columns.map((col) => (
                  <td key={col} title={row[col]}>
                    {row[col]}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default memo(DataPreviewTable);
