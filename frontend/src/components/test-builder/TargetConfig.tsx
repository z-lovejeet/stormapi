import type { UseFormReturn } from 'react-hook-form';
import { Globe } from 'lucide-react';
import { Input } from '../common/Input';
import { Select } from '../common/Select';
import { HeadersEditor } from './HeadersEditor';
import { HttpMethod } from '../../types/test';
import type { TestBuilderFormData } from '../../schemas/testBuilderSchema';
import styles from './TargetConfig.module.css';

const METHOD_OPTIONS = Object.values(HttpMethod).map((m) => ({ value: m, label: m }));
const BODY_METHODS = new Set([HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH]);

interface TargetConfigProps {
  form: UseFormReturn<TestBuilderFormData>;
}

export function TargetConfig({ form }: TargetConfigProps) {
  const { register, formState: { errors }, watch, setValue, getValues } = form;
  const method = watch('httpMethod');

  return (
    <div className={styles.container} role="group" aria-label="Step 1: Target Configuration">
      <Input
        label="Test Name"
        placeholder="e.g. Homepage Load Test"
        error={errors.name?.message}
        autoFocus
        {...register('name')}
      />

      <Input
        label="Description (optional)"
        placeholder="Brief description of what this test validates"
        error={errors.description?.message}
        {...register('description')}
      />

      <div className={styles.row}>
        <Input
          label="Target URL"
          placeholder="https://api.example.com/endpoint"
          icon={Globe}
          error={errors.targetUrl?.message}
          {...register('targetUrl')}
        />
        <Select
          label="HTTP Method"
          options={METHOD_OPTIONS}
          error={errors.httpMethod?.message}
          {...register('httpMethod')}
        />
      </div>

      <HeadersEditor
        value={getValues('headers') || {}}
        onChange={(headers) => setValue('headers', headers, { shouldDirty: true })}
      />

      {BODY_METHODS.has(method) && (
        <div>
          <label
            htmlFor="requestBody"
            style={{
              display: 'block',
              fontSize: 'var(--storm-text-sm)',
              fontWeight: 'var(--storm-weight-medium)',
              color: 'var(--storm-text-primary)',
              marginBottom: 'var(--storm-space-1)',
            }}
          >
            Request Body
          </label>
          <textarea
            id="requestBody"
            className={styles.bodyTextarea}
            placeholder='{"key": "value"}'
            {...register('requestBody')}
          />
        </div>
      )}
    </div>
  );
}
