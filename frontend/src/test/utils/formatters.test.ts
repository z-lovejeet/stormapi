import { describe, it, expect } from 'vitest';
import {
  formatMs,
  formatRps,
  formatBytes,
  formatNumber,
  formatPercent,
  formatDuration,
} from '../../utils/formatters';

describe('formatMs', () => {
  it('handles negative', () => expect(formatMs(-1)).toBe('0ms'));
  it('handles sub-millisecond', () => expect(formatMs(0.5)).toBe('0.5ms'));
  it('handles normal ms', () => expect(formatMs(123)).toBe('123ms'));
  it('converts to seconds', () => expect(formatMs(1500)).toBe('1.50s'));
});

describe('formatRps', () => {
  it('handles negative', () => expect(formatRps(-1)).toBe('0 rps'));
  it('formats small', () => expect(formatRps(0.5)).toBe('0.50 rps'));
  it('formats medium', () => expect(formatRps(42.3)).toBe('42.3 rps'));
  it('formats large', () => expect(formatRps(1500)).toBe('1,500 rps'));
});

describe('formatBytes', () => {
  it('handles negative', () => expect(formatBytes(-1)).toBe('0 B'));
  it('formats bytes', () => expect(formatBytes(512)).toBe('512 B'));
  it('formats KB', () => expect(formatBytes(2048)).toBe('2.0 KB'));
  it('formats MB', () => expect(formatBytes(1048576)).toBe('1.00 MB'));
  it('formats GB', () => expect(formatBytes(1073741824)).toBe('1.00 GB'));
});

describe('formatNumber', () => {
  it('adds separators', () => expect(formatNumber(1234567)).toBe('1,234,567'));
});

describe('formatPercent', () => {
  it('formats with 1 decimal', () => expect(formatPercent(99.99)).toBe('100.0%'));
  it('formats zero', () => expect(formatPercent(0)).toBe('0.0%'));
});

describe('formatDuration', () => {
  it('handles negative', () => expect(formatDuration(-1)).toBe('0:00'));
  it('formats seconds', () => expect(formatDuration(45)).toBe('0:45'));
  it('formats minutes', () => expect(formatDuration(125)).toBe('2:05'));
  it('formats hours', () => expect(formatDuration(3665)).toBe('1:01:05'));
});
