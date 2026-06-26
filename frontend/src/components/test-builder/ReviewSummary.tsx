import type { UseFormReturn } from 'react-hook-form';
import { Rocket, Pencil } from 'lucide-react';
import { Button } from '../common/Button';
import { MethodBadge } from '../common/MethodBadge';
import { TestTypeBadge } from '../common/TestTypeBadge';
import { TestType } from '../../types/test';
import type { TestBuilderFormData } from '../../schemas/testBuilderSchema';
import styles from './ReviewSummary.module.css';

interface ReviewSummaryProps {
  form: UseFormReturn<TestBuilderFormData>;
  onEditStep: (step: number) => void;
  onSubmit: () => void;
  submitting: boolean;
}

function Field({ label, value }: { label: string; value: React.ReactNode }) {
  return (
    <div className={styles.field}>
      <span className={styles.fieldLabel}>{label}</span>
      <span className={styles.fieldValue}>{value || '—'}</span>
    </div>
  );
}

export function ReviewSummary({ form, onEditStep, onSubmit, submitting }: ReviewSummaryProps) {
  const v = form.getValues();
  const headers = v.headers ? Object.entries(v.headers) : [];
  const testType = v.testType;

  const showStepFields = testType === TestType.STRESS || testType === TestType.SCALABILITY || testType === TestType.BREAKPOINT;
  const showStepDuration = testType === TestType.STRESS || testType === TestType.SCALABILITY;
  const showSpikeUsers = testType === TestType.SPIKE;

  return (
    <div className={styles.container} role="group" aria-label="Step 4: Review & Run">
      {/* Target Section */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>Target</h3>
          <Button variant="ghost" size="sm" icon={Pencil} onClick={() => onEditStep(0)} type="button">
            Edit
          </Button>
        </div>
        <div className={styles.grid}>
          <Field label="Name" value={v.name} />
          <Field label="Description" value={v.description} />
          <Field label="URL" value={v.targetUrl} />
          <Field label="Method" value={<MethodBadge method={v.httpMethod} />} />
          <Field label="Headers" value={headers.length > 0 ? `${headers.length} header(s)` : 'None'} />
          {v.requestBody && <Field label="Body" value={v.requestBody.substring(0, 80) + (v.requestBody.length > 80 ? '...' : '')} />}
        </div>
      </div>

      {/* Test Type Section */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>Test Type</h3>
          <Button variant="ghost" size="sm" icon={Pencil} onClick={() => onEditStep(1)} type="button">
            Edit
          </Button>
        </div>
        {testType && <TestTypeBadge type={testType} />}
      </div>

      {/* Configuration Section */}
      <div className={styles.section}>
        <div className={styles.sectionHeader}>
          <h3 className={styles.sectionTitle}>Configuration</h3>
          <Button variant="ghost" size="sm" icon={Pencil} onClick={() => onEditStep(2)} type="button">
            Edit
          </Button>
        </div>
        <div className={styles.grid}>
          <Field label="Virtual Users" value={v.virtualUsers} />
          <Field label="Duration" value={`${v.durationSeconds}s`} />
          <Field label="Ramp-up" value={`${v.rampUpSeconds}s`} />
          {showStepFields && <Field label="Step Size" value={`${v.stepSize} users`} />}
          {showStepDuration && <Field label="Step Duration" value={`${v.stepDurationSeconds}s`} />}
          {showSpikeUsers && <Field label="Spike Users" value={v.spikeUsers} />}
          <Field label="Max Retries" value={v.maxRetries} />
          <Field label="Timeout" value={`${v.timeoutMs}ms`} />
          <Field label="Think Time" value={`${v.thinkTimeMs}ms`} />
        </div>
      </div>

      {/* Submit */}
      <div className={styles.submitWrapper}>
        <Button
          size="lg"
          icon={Rocket}
          loading={submitting}
          onClick={onSubmit}
          type="button"
        >
          Start Test
        </Button>
      </div>
    </div>
  );
}
