import { useState, useCallback } from 'react';
import { Download, FileJson, FileSpreadsheet, FileText, FileDown } from 'lucide-react';
import { downloadJson, downloadCsv, downloadHtml, downloadPdf } from '../../api/exportApi';
import styles from './ExportDropdown.module.css';

interface ExportDropdownProps {
  resultId: number;
}

type ExportFormat = 'json' | 'csv' | 'html' | 'pdf';

export const ExportDropdown: React.FC<ExportDropdownProps> = ({ resultId }) => {
  const [open, setOpen] = useState(false);
  const [downloading, setDownloading] = useState<ExportFormat | null>(null);

  const handleDownload = useCallback(async (format: ExportFormat) => {
    setDownloading(format);
    try {
      switch (format) {
        case 'json':
          await downloadJson(resultId);
          break;
        case 'csv':
          await downloadCsv(resultId);
          break;
        case 'html':
          await downloadHtml(resultId);
          break;
        case 'pdf':
          await downloadPdf(resultId);
          break;
      }
    } catch (err) {
      console.error(`Export ${format} failed:`, err);
    } finally {
      setDownloading(null);
      setOpen(false);
    }
  }, [resultId]);

  return (
    <div className={styles.container}>
      <button
        className={styles.triggerBtn}
        onClick={() => setOpen(!open)}
        disabled={downloading !== null}
      >
        <Download size={16} />
        {downloading ? 'Downloading…' : 'Download Report'}
      </button>

      {open && (
        <>
          <div className={styles.backdrop} onClick={() => setOpen(false)} />
          <div className={styles.menu}>
            <button
              className={styles.menuItem}
              onClick={() => handleDownload('json')}
              disabled={downloading !== null}
            >
              <FileJson size={18} />
              <span className={styles.menuItemLabel}>
                JSON
                <span className={styles.menuItemDesc}>Full result data</span>
              </span>
            </button>
            <button
              className={styles.menuItem}
              onClick={() => handleDownload('csv')}
              disabled={downloading !== null}
            >
              <FileSpreadsheet size={18} />
              <span className={styles.menuItemLabel}>
                CSV
                <span className={styles.menuItemDesc}>Metric snapshots — Excel compatible</span>
              </span>
            </button>
            <button
              className={styles.menuItem}
              onClick={() => handleDownload('html')}
              disabled={downloading !== null}
            >
              <FileText size={18} />
              <span className={styles.menuItemLabel}>
                HTML Report
                <span className={styles.menuItemDesc}>Self-contained visual report</span>
              </span>
            </button>
            <button
              className={styles.menuItem}
              onClick={() => handleDownload('pdf')}
              disabled={downloading !== null}
            >
              <FileDown size={18} />
              <span className={styles.menuItemLabel}>
                PDF Report
                <span className={styles.menuItemDesc}>Professional shareable report</span>
              </span>
            </button>
          </div>
        </>
      )}
    </div>
  );
};
