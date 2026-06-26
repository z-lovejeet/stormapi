import type { UseFormReturn } from 'react-hook-form';
import { Input } from '../common/Input';
import type { TestBuilderFormData } from '../../schemas/testBuilderSchema';

interface ConfigFieldsProps {
  form: UseFormReturn<TestBuilderFormData>;
}

export function LoadConfigFields({ form }: ConfigFieldsProps) {
  const { register, formState: { errors } } = form;

  return (
    <>
      <Input label="Virtual Users" type="number" error={errors.virtualUsers?.message} {...register('virtualUsers')} />
      <Input label="Duration (seconds)" type="number" error={errors.durationSeconds?.message} {...register('durationSeconds')} />
      <Input label="Ramp-up (seconds)" type="number" helperText="Time to reach full user count" error={errors.rampUpSeconds?.message} {...register('rampUpSeconds')} />
      <Input label="Max Retries" type="number" error={errors.maxRetries?.message} {...register('maxRetries')} />
      <Input label="Timeout (ms)" type="number" error={errors.timeoutMs?.message} {...register('timeoutMs')} />
      <Input label="Think Time (ms)" type="number" helperText="Delay between requests per user" error={errors.thinkTimeMs?.message} {...register('thinkTimeMs')} />
    </>
  );
}
