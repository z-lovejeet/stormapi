import { z } from 'zod';
import { TestType, HttpMethod } from '../types/test';

// ── Base field schemas ────────────────────────────────────

const urlSchema = z.string().min(1, 'URL is required').url('Must be a valid URL');

const nameSchema = z.string().min(1, 'Name is required').max(255, 'Name must be 255 characters or less');

const descriptionSchema = z.string().max(1000, 'Description must be 1000 characters or less').optional().or(z.literal(''));

// ── Full schema ───────────────────────────────────────────

export const testBuilderSchema = z.object({
  name: nameSchema,
  description: descriptionSchema,
  targetUrl: urlSchema,
  httpMethod: z.nativeEnum(HttpMethod),
  headers: z.record(z.string(), z.string()).optional(),
  requestBody: z.string().optional().or(z.literal('')),
  testType: z.nativeEnum(TestType, { message: 'Select a test type' }),
  virtualUsers: z.coerce.number().int().min(1, 'Min 1').max(10000, 'Max 10,000'),
  durationSeconds: z.coerce.number().int().min(1, 'Min 1s').max(86400, 'Max 86,400s'),
  rampUpSeconds: z.coerce.number().int().min(0, 'Min 0'),
  stepSize: z.coerce.number().int().min(1).max(1000).optional().nullable(),
  stepDurationSeconds: z.coerce.number().int().min(1).max(3600).optional().nullable(),
  spikeUsers: z.coerce.number().int().min(1).max(10000).optional().nullable(),
  maxRetries: z.coerce.number().int().min(0, 'Min 0').max(5, 'Max 5'),
  timeoutMs: z.coerce.number().int().min(100, 'Min 100ms').max(60000, 'Max 60,000ms'),
  thinkTimeMs: z.coerce.number().int().min(0, 'Min 0').max(60000, 'Max 60,000ms'),
  autoStart: z.boolean(),
}).superRefine((data, ctx) => {
  // Cross-field: rampUpSeconds <= durationSeconds
  if (data.rampUpSeconds > data.durationSeconds) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: 'Ramp-up cannot exceed duration',
      path: ['rampUpSeconds'],
    });
  }

  // Conditional: SPIKE requires spikeUsers
  if (data.testType === TestType.SPIKE) {
    if (!data.spikeUsers || data.spikeUsers < 1) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Spike users is required for Spike tests',
        path: ['spikeUsers'],
      });
    } else if (data.spikeUsers <= data.virtualUsers) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Spike users must exceed virtual users',
        path: ['spikeUsers'],
      });
    }
  }

  // Conditional: STRESS/SCALABILITY require stepSize + stepDurationSeconds
  if (data.testType === TestType.STRESS || data.testType === TestType.SCALABILITY) {
    if (!data.stepSize || data.stepSize < 1) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Step size is required',
        path: ['stepSize'],
      });
    }
    if (!data.stepDurationSeconds || data.stepDurationSeconds < 1) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Step duration is required',
        path: ['stepDurationSeconds'],
      });
    }
  }

  // Conditional: BREAKPOINT requires stepSize
  if (data.testType === TestType.BREAKPOINT) {
    if (!data.stepSize || data.stepSize < 1) {
      ctx.addIssue({
        code: z.ZodIssueCode.custom,
        message: 'Step size is required for Breakpoint tests',
        path: ['stepSize'],
      });
    }
  }
});

export type TestBuilderFormData = z.infer<typeof testBuilderSchema>;

// ── Per-step field arrays for trigger() validation ────────

export const STEP_FIELDS: Record<number, (keyof TestBuilderFormData)[]> = {
  0: ['name', 'targetUrl', 'httpMethod'],
  1: ['testType'],
  2: ['virtualUsers', 'durationSeconds', 'rampUpSeconds', 'maxRetries', 'timeoutMs', 'thinkTimeMs',
      'stepSize', 'stepDurationSeconds', 'spikeUsers'],
  3: [], // Full schema validated on submit
};

// ── Wizard step metadata ──────────────────────────────────

export const WIZARD_STEPS = [
  { label: 'Target' },
  { label: 'Test Type' },
  { label: 'Configuration' },
  { label: 'Review & Run' },
] as const;
