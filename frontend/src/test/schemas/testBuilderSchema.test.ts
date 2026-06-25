import { describe, it, expect } from 'vitest';
import { testBuilderSchema } from '../../schemas/testBuilderSchema';
import { TestType, HttpMethod } from '../../types/test';

const validPayload = {
  name: 'Load Test',
  description: '',
  targetUrl: 'https://api.example.com/health',
  httpMethod: HttpMethod.GET,
  headers: {},
  requestBody: '',
  testType: TestType.LOAD,
  virtualUsers: 100,
  durationSeconds: 60,
  rampUpSeconds: 10,
  stepSize: null,
  stepDurationSeconds: null,
  spikeUsers: null,
  maxRetries: 0,
  timeoutMs: 5000,
  thinkTimeMs: 0,
  autoStart: true,
};

describe('testBuilderSchema', () => {
  it('accepts a valid LOAD payload', () => {
    const result = testBuilderSchema.safeParse(validPayload);
    expect(result.success).toBe(true);
  });

  it('rejects missing name', () => {
    const result = testBuilderSchema.safeParse({ ...validPayload, name: '' });
    expect(result.success).toBe(false);
  });

  it('rejects invalid URL', () => {
    const result = testBuilderSchema.safeParse({ ...validPayload, targetUrl: 'not-a-url' });
    expect(result.success).toBe(false);
  });

  it('rejects virtualUsers out of range', () => {
    const result = testBuilderSchema.safeParse({ ...validPayload, virtualUsers: 20000 });
    expect(result.success).toBe(false);
  });

  it('requires spikeUsers for SPIKE type', () => {
    const result = testBuilderSchema.safeParse({
      ...validPayload, testType: TestType.SPIKE, spikeUsers: null,
    });
    expect(result.success).toBe(false);
  });

  it('rejects rampUpSeconds > durationSeconds', () => {
    const result = testBuilderSchema.safeParse({
      ...validPayload, rampUpSeconds: 120, durationSeconds: 60,
    });
    expect(result.success).toBe(false);
  });

  it('rejects spikeUsers <= virtualUsers for SPIKE type', () => {
    const result = testBuilderSchema.safeParse({
      ...validPayload, testType: TestType.SPIKE, spikeUsers: 50, virtualUsers: 100,
    });
    expect(result.success).toBe(false);
  });

  it('requires stepSize for STRESS type', () => {
    const result = testBuilderSchema.safeParse({
      ...validPayload, testType: TestType.STRESS, stepSize: null, stepDurationSeconds: 30,
    });
    expect(result.success).toBe(false);
  });
});
