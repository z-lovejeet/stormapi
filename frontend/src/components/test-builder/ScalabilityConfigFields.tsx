import type { UseFormReturn } from 'react-hook-form';
import { Input } from '../common/Input';
import type { TestBuilderFormData } from '../../schemas/testBuilderSchema';

interface ConfigFieldsProps {
  form: UseFormReturn<TestBuilderFormData>;
}

export function ScalabilityConfigFields({ form }: ConfigFieldsProps) {
  const { register, formState: { errors } } = form;

  return (
    <>
      <Input label="Virtual Users (start)" type="number" error={errors.virtualUsers?.message} {...register('virtualUsers')} />
      <Input label="Step Size (users per step)" type="number" helperText="Users added at each step" error={errors.stepSize?.message} {...register('stepSize')} />
      <Input label="Step Duration (seconds)" type="number" helperText="Time at each step level" error={errors.stepDurationSeconds?.message} {...register('stepDurationSeconds')} />
      <Input label="Duration (seconds)" type="number" error={errors.durationSeconds?.message} {...register('durationSeconds')} />
      <Input label="Ramp-up (seconds)" type="number" error={errors.rampUpSeconds?.message} {...register('rampUpSeconds')} />
      <Input label="Max Retries" type="number" error={errors.maxRetries?.message} {...register('maxRetries')} />
      <Input label="Timeout (ms)" type="number" error={errors.timeoutMs?.message} {...register('timeoutMs')} />
      <Input label="Think Time (ms)" type="number" error={errors.thinkTimeMs?.message} {...register('thinkTimeMs')} />
    </>
  );
}
