package com.stormapi.engine.context;

import com.stormapi.engine.http.RequestResult;
import com.stormapi.engine.http.RequestSpec;
import com.stormapi.engine.user.NoThinkTimeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionContextTest {

    private RequestSpec testSpec;
    private ExecutionContext context;

    @BeforeEach
    void setUp() {
        testSpec = new RequestSpec("https://example.com", "GET", null, null,
                Duration.ofSeconds(5));
        context = new ExecutionContext(testSpec, NoThinkTimeStrategy.INSTANCE, result -> {});
    }

    @Test
    void isRunning_defaultFalse() {
        assertThat(context.isRunning()).isFalse();
    }

    @Test
    void startAndStop_togglesRunning() {
        context.start();
        assertThat(context.isRunning()).isTrue();

        context.stop();
        assertThat(context.isRunning()).isFalse();
    }

    @Test
    void recordResult_callsConsumer() {
        AtomicInteger counter = new AtomicInteger(0);
        ExecutionContext ctx = new ExecutionContext(testSpec, NoThinkTimeStrategy.INSTANCE,
                result -> counter.incrementAndGet());

        ctx.recordResult(RequestResult.success(200, 1000, 10, Instant.now()));
        ctx.recordResult(RequestResult.success(200, 2000, 20, Instant.now()));

        assertThat(counter.get()).isEqualTo(2);
    }

    @Test
    void activeUsers_threadSafe() throws InterruptedException {
        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    startLatch.await();
                    context.incrementActiveUsers();
                    // small pause to overlap
                    Thread.sleep(1);
                    context.decrementActiveUsers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads at once
        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);

        assertThat(completed).isTrue();
        assertThat(context.getActiveUsers()).isZero();
    }

    @Test
    void concurrentStopAndRead_noRace() throws InterruptedException {
        context.start();
        int readerCount = 50;
        CountDownLatch doneLatch = new CountDownLatch(readerCount + 1);

        // 50 readers checking isRunning()
        for (int i = 0; i < readerCount; i++) {
            Thread.ofVirtual().start(() -> {
                try {
                    for (int j = 0; j < 1000; j++) {
                        context.isRunning(); // should never throw
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 1 writer calling stop()
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(5);
                context.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(context.isRunning()).isFalse();
    }

}
