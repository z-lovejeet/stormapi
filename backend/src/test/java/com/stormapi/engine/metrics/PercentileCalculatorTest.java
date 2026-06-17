package com.stormapi.engine.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PercentileCalculator Tests")
class PercentileCalculatorTest {

    private PercentileCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new PercentileCalculator();
    }

    @Test
    @DisplayName("records single value and returns correct P50")
    void recordAndGetP50_singleValue() {
        // 100ms in nanoseconds
        calculator.recordLatency(100_000_000L);

        double p50 = calculator.getPercentileMs(50.0);
        assertEquals(100.0, p50, 1.0, "P50 should be ~100ms");
    }

    @Test
    @DisplayName("records known distribution and returns accurate percentiles")
    void recordAndGetPercentiles_knownDistribution() {
        // Record 1ms through 100ms (100 values, 1ms step)
        for (int ms = 1; ms <= 100; ms++) {
            calculator.recordLatency(ms * 1_000_000L);
        }

        assertEquals(50.0, calculator.getPercentileMs(50.0), 2.0, "P50 should be ~50ms");
        assertEquals(90.0, calculator.getPercentileMs(90.0), 2.0, "P90 should be ~90ms");
        assertEquals(99.0, calculator.getPercentileMs(99.0), 2.0, "P99 should be ~99ms");
    }

    @Test
    @DisplayName("reports correct min and max values")
    void getMinMax_correctValues() {
        calculator.recordLatency(5_000_000L);    // 5ms
        calculator.recordLatency(500_000_000L);  // 500ms
        calculator.recordLatency(50_000_000L);   // 50ms

        assertEquals(5.0, calculator.getMinMs(), 1.0, "Min should be ~5ms");
        assertEquals(500.0, calculator.getMaxMs(), 1.0, "Max should be ~500ms");
    }

    @Test
    @DisplayName("reports correct mean value")
    void getMean_correctAverage() {
        // Record 10ms × 100 values
        for (int i = 0; i < 100; i++) {
            calculator.recordLatency(10_000_000L);
        }

        assertEquals(10.0, calculator.getMeanMs(), 1.0, "Mean should be ~10ms");
    }

    @Test
    @DisplayName("reset clears all data")
    void reset_clearsAllData() {
        calculator.recordLatency(50_000_000L);
        calculator.reset();

        assertEquals(0, calculator.getTotalCount());
        assertEquals(0.0, calculator.getPercentileMs(50.0));
        assertEquals(0.0, calculator.getMinMs());
        assertEquals(0.0, calculator.getMaxMs());
        assertEquals(0.0, calculator.getMeanMs());

        // Record new value after reset — old data should not affect
        calculator.recordLatency(200_000_000L);
        assertEquals(200.0, calculator.getPercentileMs(50.0), 1.0);
    }

    @Test
    @DisplayName("empty histogram returns zeros for all queries")
    void emptyHistogram_returnsZeros() {
        assertEquals(0, calculator.getTotalCount());
        assertEquals(0.0, calculator.getPercentileMs(50.0));
        assertEquals(0.0, calculator.getPercentileMs(99.0));
        assertEquals(0.0, calculator.getMinMs());
        assertEquals(0.0, calculator.getMaxMs());
        assertEquals(0.0, calculator.getMeanMs());
    }

    @Test
    @DisplayName("high latency is clipped to max trackable value")
    void highLatency_clippedToMax() {
        // 60 seconds in nanos (exceeds 30s max)
        calculator.recordLatency(60_000_000_000L);

        // Should be clipped to ~30s (HdrHistogram rounds to 3 significant digits)
        double max = calculator.getMaxMs();
        assertTrue(max <= 30_100.0 && max >= 29_900.0,
                "Should clip to ~30000ms, got: " + max);
    }

    @Test
    @DisplayName("100 threads recording concurrently — no data loss")
    void concurrentRecording_noDataLoss() throws InterruptedException {
        int threadCount = 100;
        int recordingsPerThread = 1000;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        IntStream.range(0, threadCount).forEach(i ->
                Thread.ofVirtual().start(() -> {
                    try {
                        startGate.await();
                        for (int j = 0; j < recordingsPerThread; j++) {
                            calculator.recordLatency(10_000_000L); // 10ms
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                })
        );

        startGate.countDown();
        doneLatch.await();

        assertEquals(threadCount * recordingsPerThread, calculator.getTotalCount(),
                "All recordings should be captured without data loss");
    }

}
