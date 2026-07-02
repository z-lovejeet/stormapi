import React, { memo, useCallback, useRef, useState } from 'react';
import { Upload, FileText, X } from 'lucide-react';
import type { DataFormat } from '../../types/data';
import styles from './DataUpload.module.css';

interface DataUploadProps {
  format: DataFormat;
  onFormatChange: (format: DataFormat) => void;
  content: string;
  onContentChange: (content: string) => void;
}

/**
 * Data upload component — supports file drag-and-drop, file picker, or paste.
 */
const DataUpload: React.FC<DataUploadProps> = ({
  format,
  onFormatChange,
  content,
  onContentChange,
}) => {
  const [fileName, setFileName] = useState<string | null>(null);
  const [isDragActive, setIsDragActive] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFile = useCallback(
    (file: File) => {
      const reader = new FileReader();
      reader.onload = (e) => {
        const text = e.target?.result as string;
        onContentChange(text);
        setFileName(file.name);

        // Auto-detect format from extension
        if (file.name.endsWith('.csv')) {
          onFormatChange('CSV');
        } else if (file.name.endsWith('.json')) {
          onFormatChange('JSON');
        }
      };
      reader.readAsText(file);
    },
    [onContentChange, onFormatChange],
  );

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      setIsDragActive(false);
      if (e.dataTransfer.files.length > 0) {
        const file = e.dataTransfer.files[0];
        if (file) handleFile(file);
      }
    },
    [handleFile],
  );

  const handleClear = useCallback(() => {
    onContentChange('');
    setFileName(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, [onContentChange]);

  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <FileText size={16} />
        <span className={styles.title}>Test Data</span>
        <div className={styles.formatToggle}>
          {(['CSV', 'JSON'] as DataFormat[]).map((f) => (
            <button
              key={f}
              className={`${styles.formatBtn} ${format === f ? styles.formatBtnActive : ''}`}
              onClick={() => onFormatChange(f)}
              type="button"
            >
              {f}
            </button>
          ))}
        </div>
      </div>

      <div
        className={`${styles.dropzone} ${isDragActive ? styles.dropzoneActive : ''}`}
        onDragOver={(e) => { e.preventDefault(); setIsDragActive(true); }}
        onDragLeave={() => setIsDragActive(false)}
        onDrop={handleDrop}
        onClick={() => fileInputRef.current?.click()}
        role="button"
        tabIndex={0}
      >
        <div className={styles.dropzoneIcon}>
          <Upload size={32} />
        </div>
        <div className={styles.dropzoneText}>
          Drop a {format} file here, or click to browse
        </div>
        <div className={styles.dropzoneHint}>
          Supports .csv and .json files (max 1000 rows)
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept={format === 'CSV' ? '.csv' : '.json'}
          style={{ display: 'none' }}
          onChange={(e) => {
            if (e.target.files?.[0]) handleFile(e.target.files[0]);
          }}
        />
      </div>

      {fileName && (
        <div className={styles.fileInfo}>
          <span className={styles.fileName}>{fileName}</span>
          <span className={styles.fileSize}>{content.length} chars</span>
          <button className={styles.clearBtn} onClick={handleClear} type="button" title="Clear">
            <X size={14} />
          </button>
        </div>
      )}

      <textarea
        className={styles.textarea}
        placeholder={
          format === 'CSV'
            ? 'username,password,expected\nalice,pass123,200\nbob,secret,200'
            : '[{"username":"alice","password":"pass123"},{"username":"bob","password":"secret"}]'
        }
        value={content}
        onChange={(e) => {
          onContentChange(e.target.value);
          setFileName(null);
        }}
      />
    </div>
  );
};

export default memo(DataUpload);
