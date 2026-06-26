import { useEffect, useRef } from 'react';
import type { UseFormReturn } from 'react-hook-form';
import { TestType } from '../../types/test';
import type { TestBuilderFormData } from '../../schemas/testBuilderSchema';
import { LoadConfigFields } from './LoadConfigFields';
import { StressConfigFields } from './StressConfigFields';
import { SpikeConfigFields } from './SpikeConfigFields';
import { SoakConfigFields } from './SoakConfigFields';
import { BreakpointConfigFields } from './BreakpointConfigFields';
import { ScalabilityConfigFields } from './ScalabilityConfigFields';
import styles from './TestConfigForm.module.css';

/** Type-specific default overrides applied when switching test type */
const TYPE_DEFAULTS: Partial<Record<TestType, Partial<TestBuilderFormData>>> = {
  [TestType.LOAD]: { virtualUsers: 100, durationSeconds: 60, rampUpSeconds: 10 },
  [TestType.STRESS]: { virtualUsers: 50, durationSeconds: 300, rampUpSeconds: 0, stepSize: 50, stepDurationSeconds: 30 },
  [TestType.SPIKE]: { virtualUsers: 50, durationSeconds: 120, rampUpSeconds: 5, spikeUsers: 500 },
  [TestType.SOAK]: { virtualUsers: 100, durationSeconds: 1800, rampUpSeconds: 30 },
  [TestType.BREAKPOINT]: { virtualUsers: 10, durationSeconds: 600, rampUpSeconds: 0, stepSize: 10 },
  [TestType.SCALABILITY]: { virtualUsers: 10, durationSeconds: 600, rampUpSeconds: 0, stepSize: 50, stepDurationSeconds: 60 },
};

interface TestConfigFormProps {
  form: UseFormReturn<TestBuilderFormData>;
}

export function TestConfigForm({ form }: TestConfigFormProps) {
  const testType = form.watch('testType');
  const prevType = useRef(testType);

  // Apply type-specific defaults when test type changes
  useEffect(() => {
    if (testType && testType !== prevType.current) {
      const defaults = TYPE_DEFAULTS[testType];
      if (defaults) {
        for (const [key, value] of Object.entries(defaults)) {
          form.setValue(key as keyof TestBuilderFormData, value as never, { shouldDirty: false });
        }
      }
      // Clear type-specific fields that don't apply
      if (testType !== TestType.SPIKE) form.setValue('spikeUsers', null);
      if (![TestType.STRESS, TestType.BREAKPOINT, TestType.SCALABILITY].includes(testType)) form.setValue('stepSize', null);
      if (![TestType.STRESS, TestType.SCALABILITY].includes(testType)) form.setValue('stepDurationSeconds', null);
      prevType.current = testType;
    }
  }, [testType, form]);

  const FieldsComponent = testType ? {
    [TestType.LOAD]: LoadConfigFields,
    [TestType.STRESS]: StressConfigFields,
    [TestType.SPIKE]: SpikeConfigFields,
    [TestType.SOAK]: SoakConfigFields,
    [TestType.BREAKPOINT]: BreakpointConfigFields,
    [TestType.SCALABILITY]: ScalabilityConfigFields,
  }[testType] : null;

  if (!FieldsComponent) return null;

  return (
    <div className={styles.container} role="group" aria-label="Step 3: Test Configuration">
      <h3 className={styles.heading}>Configure {testType} Test</h3>
      <div className={styles.fields}>
        <FieldsComponent form={form} />
      </div>
    </div>
  );
}
