import { useState, useCallback } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, ArrowRight } from 'lucide-react';
import { Button } from '../components/common/Button';
import { useToast } from '../components/common/Toast';
import { StepIndicator } from '../components/test-builder/StepIndicator';
import { TargetConfig } from '../components/test-builder/TargetConfig';
import { TestTypeSelector } from '../components/test-builder/TestTypeSelector';
import { TestConfigForm } from '../components/test-builder/TestConfigForm';
import { ReviewSummary } from '../components/test-builder/ReviewSummary';
import { createTest } from '../api/testApi';
import { testBuilderSchema, STEP_FIELDS, WIZARD_STEPS } from '../schemas/testBuilderSchema';
import type { TestBuilderFormData } from '../schemas/testBuilderSchema';
import { HttpMethod, TestType } from '../types/test';
import type { CreateTestRequest } from '../types/test';
import styles from './TestBuilderPage.module.css';

function parseTypeParam(param: string | null): TestType | undefined {
  if (!param) return undefined;
  const upper = param.toUpperCase();
  return Object.values(TestType).includes(upper as TestType) ? (upper as TestType) : undefined;
}

export function TestBuilderPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { showToast } = useToast();

  const preselectedType = parseTypeParam(searchParams.get('type'));

  const form = useForm<TestBuilderFormData>({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    resolver: zodResolver(testBuilderSchema) as any,
    mode: 'onTouched',
    defaultValues: {
      name: '',
      description: '',
      targetUrl: '',
      httpMethod: HttpMethod.GET,
      headers: {},
      requestBody: '',
      testType: preselectedType,
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
    },
  });

  const [currentStep, setCurrentStep] = useState(preselectedType ? 1 : 0);
  const [submitting, setSubmitting] = useState(false);
  const [direction, setDirection] = useState(1); // 1=forward, -1=back

  const goForward = useCallback(async () => {
    const fields = STEP_FIELDS[currentStep];
    if (fields && fields.length > 0) {
      const valid = await form.trigger(fields);
      if (!valid) return;
    }
    setDirection(1);
    setCurrentStep((s) => Math.min(s + 1, WIZARD_STEPS.length - 1));
  }, [currentStep, form]);

  const goBack = useCallback(() => {
    setDirection(-1);
    setCurrentStep((s) => Math.max(s - 1, 0));
  }, []);

  const jumpToStep = useCallback((step: number) => {
    setDirection(step < currentStep ? -1 : 1);
    setCurrentStep(step);
  }, [currentStep]);

  const handleSubmit = useCallback(async () => {
    const valid = await form.trigger();
    if (!valid) {
      showToast('error', 'Please fix validation errors before submitting');
      return;
    }

    setSubmitting(true);
    try {
      const data = form.getValues();
      // Clean headers — remove empty keys
      const cleanHeaders: Record<string, string> = {};
      if (data.headers) {
        for (const [k, v] of Object.entries(data.headers)) {
          if (k.trim()) cleanHeaders[k.trim()] = v;
        }
      }

      const request: CreateTestRequest = {
        name: data.name,
        description: data.description || undefined,
        targetUrl: data.targetUrl,
        httpMethod: data.httpMethod,
        headers: Object.keys(cleanHeaders).length > 0 ? cleanHeaders : undefined,
        requestBody: data.requestBody || undefined,
        testType: data.testType,
        virtualUsers: Number(data.virtualUsers),
        durationSeconds: Number(data.durationSeconds),
        rampUpSeconds: Number(data.rampUpSeconds),
        stepSize: data.stepSize ? Number(data.stepSize) : undefined,
        stepDurationSeconds: data.stepDurationSeconds ? Number(data.stepDurationSeconds) : undefined,
        spikeUsers: data.spikeUsers ? Number(data.spikeUsers) : undefined,
        maxRetries: Number(data.maxRetries),
        timeoutMs: Number(data.timeoutMs),
        thinkTimeMs: Number(data.thinkTimeMs),
        autoStart: true,
      };

      const response = await createTest(request);
      showToast('success', `Test "${data.name}" started successfully!`);
      navigate(`/tests/${response.id}/live`);
    } catch (err: unknown) {
      const message = err && typeof err === 'object' && 'message' in err
        ? (err as { message: string }).message
        : 'Failed to start test';
      showToast('error', message);
    } finally {
      setSubmitting(false);
    }
  }, [form, navigate, showToast]);

  const slideVariants = {
    enter: (d: number) => ({ x: d > 0 ? 40 : -40, opacity: 0 }),
    center: { x: 0, opacity: 1 },
    exit: (d: number) => ({ x: d > 0 ? -40 : 40, opacity: 0 }),
  };

  return (
    <div className={styles.page}>
      <div className={styles.header}>
        <h1>Create Test</h1>
      </div>

      <StepIndicator
        steps={WIZARD_STEPS}
        currentStep={currentStep}
        onStepClick={(i) => { if (i < currentStep) jumpToStep(i); }}
      />

      <div className={styles.stepContent} aria-live="polite">
        <AnimatePresence mode="wait" custom={direction}>
          <motion.div
            key={currentStep}
            custom={direction}
            variants={slideVariants}
            initial="enter"
            animate="center"
            exit="exit"
            transition={{ duration: 0.2 }}
          >
            {currentStep === 0 && <TargetConfig form={form} />}
            {currentStep === 1 && (
              <TestTypeSelector
                selectedType={form.watch('testType')}
                onSelect={(type) => form.setValue('testType', type, { shouldValidate: true })}
              />
            )}
            {currentStep === 2 && <TestConfigForm form={form} />}
            {currentStep === 3 && (
              <ReviewSummary
                form={form}
                onEditStep={jumpToStep}
                onSubmit={handleSubmit}
                submitting={submitting}
              />
            )}
          </motion.div>
        </AnimatePresence>
      </div>

      {/* Navigation — hidden on Review step (submit is in ReviewSummary) */}
      {currentStep < 3 && (
        <div className={styles.navigation}>
          {currentStep > 0 ? (
            <Button variant="secondary" icon={ArrowLeft} onClick={goBack} type="button">
              Back
            </Button>
          ) : (
            <div />
          )}
          <div className={styles.navRight}>
            <Button icon={ArrowRight} onClick={goForward} type="button">
              Next
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
