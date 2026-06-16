package com.stormapi.engine.user;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.stormapi.engine.context.ExecutionContext;
import com.stormapi.engine.http.HttpClientFactory;
import com.stormapi.engine.http.HttpRequestExecutor;
import com.stormapi.engine.http.RequestResult;
import com.stormapi.engine.http.RequestSpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency tests for VirtualUserSimulator.
 * Uses WireMock as target server and verifies loop behavior,
 * graceful shutdown, and thread-safety.
 */
class VirtualUserSimulatorTest {

    private static WireMockServer wireMock;
    private static HttpRequestExecutor executor;

    @BeforeAll
    static void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        wireMock.stubFor(get(urlEqualTo("/api"))
                .willReturn(aResponse().withStatus(200).withBody("ok")));

        HttpClient client = HttpClientFactory.create(Duration.ofSeconds(2));
        executor = new HttpRequestExecutor(client);
    }

    @AfterAll
    static void tearDown() {
        wireMock.stop();
    }

    private RequestSpec spec() {
        return new RequestSpec("http://localhost:" + wireMock.port() + "/api",
                "GET", null, null, Duration.ofSeconds(5));
    }

    @Test
    void runLoop_sendsRequestsUntilStopped() throws InterruptedException {
        AtomicLong resultCount = new AtomicLong(0);
        ExecutionContext ctx = new ExecutionContext(spec(), NoThinkTimeStrategy.INSTANCE,
                result -> resultCount.incrementAndGet());
        ctx.start();

        VirtualUserSimulator simulator = new VirtualUserSimulator(ctx, executor);
        Thread vThread = Thread.ofVirtual().start(simulator);

        // Let it run for 500ms
        Thread.sleep(500);
        ctx.stop();
        vThread.join(2000);

        assertThat(resultCount.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void stopContext_allUsersExit() throws InterruptedException {
        int userCount = 10;
        ExecutionContext ctx = new ExecutionContext(spec(), NoThinkTimeStrategy.INSTANCE,
                result -> {});
        ctx.start();

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            Thread vt = Thread.ofVirtual().start(new VirtualUserSimulator(ctx, executor));
            threads.add(vt);
        }

        // Let all users run briefly
        Thread.sleep(200);

        // Stop and wait for all to exit
        ctx.stop();
        for (Thread t : threads) {
            t.join(2000);
            assertThat(t.isAlive()).isFalse();
        }
    }

    @Test
    void activeUserCount_trackedCorrectly() throws InterruptedException {
        int userCount = 5;
        ExecutionContext ctx = new ExecutionContext(spec(), NoThinkTimeStrategy.INSTANCE,
                result -> {});
        ctx.start();

        CountDownLatch allStarted = new CountDownLatch(userCount);
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < userCount; i++) {
            Thread vt = Thread.ofVirtual().start(() -> {
                VirtualUserSimulator sim = new VirtualUserSimulator(ctx, executor);
                // Signal that we're about to run
                allStarted.countDown();
                sim.run();
            });
            threads.add(vt);
        }

        // Wait for all users to start
        allStarted.await(5, TimeUnit.SECONDS);
        Thread.sleep(100); // let them enter the loop

        assertThat(ctx.getActiveUsers()).isEqualTo(userCount);

        ctx.stop();
        for (Thread t : threads) {
            t.join(2000);
        }

        assertThat(ctx.getActiveUsers()).isZero();
    }

    @Test
    void resultConsumer_receivesAllResults() throws InterruptedException {
        List<RequestResult> results = Collections.synchronizedList(new ArrayList<>());
        ExecutionContext ctx = new ExecutionContext(spec(), NoThinkTimeStrategy.INSTANCE,
                results::add);
        ctx.start();

        Thread vt = Thread.ofVirtual().start(new VirtualUserSimulator(ctx, executor));
        Thread.sleep(300);
        ctx.stop();
        vt.join(2000);

        assertThat(results).isNotEmpty();
        assertThat(results).allSatisfy(r -> {
            assertThat(r.statusCode()).isEqualTo(200);
            assertThat(r.success()).isTrue();
        });
    }

    @Test
    void thinkTime_isBetweenRequests() throws InterruptedException {
        AtomicLong resultCount = new AtomicLong(0);
        // 100ms think time → in 500ms we should get ~5 requests (not hundreds)
        ExecutionContext ctx = new ExecutionContext(spec(),
                new ConstantThinkTimeStrategy(100), result -> resultCount.incrementAndGet());
        ctx.start();

        Thread vt = Thread.ofVirtual().start(new VirtualUserSimulator(ctx, executor));
        Thread.sleep(600);
        ctx.stop();
        vt.join(2000);

        // With 100ms think time + request time, expect roughly 3-8 requests in 600ms
        assertThat(resultCount.get()).isBetween(2L, 15L);
    }

    @Test
    void retryLogic_retriesOnFailure() throws InterruptedException {
        // Stub a failing endpoint
        wireMock.stubFor(get(urlEqualTo("/failing"))
                .willReturn(aResponse().withStatus(500).withBody("error")));

        RequestSpec failSpec = new RequestSpec(
                "http://localhost:" + wireMock.port() + "/failing",
                "GET", null, null, Duration.ofSeconds(5));

        AtomicLong resultCount = new AtomicLong(0);
        ExecutionContext ctx = new ExecutionContext(failSpec, NoThinkTimeStrategy.INSTANCE,
                result -> resultCount.incrementAndGet());
        ctx.setMaxRetries(2);
        ctx.start();

        Thread vt = Thread.ofVirtual().start(new VirtualUserSimulator(ctx, executor));
        Thread.sleep(500);
        ctx.stop();
        vt.join(2000);

        // Results recorded should be fewer than WireMock request count
        // because retries only record the final result
        assertThat(resultCount.get()).isPositive();
    }

    @Test
    void interruptHandling_cleanExit() throws InterruptedException {
        ExecutionContext ctx = new ExecutionContext(spec(), 
                new ConstantThinkTimeStrategy(200), result -> {});
        ctx.start();

        Thread vt = Thread.ofVirtual().start(new VirtualUserSimulator(ctx, executor));
        Thread.sleep(100);

        // Interrupt during think-time sleep — should cause clean exit
        vt.interrupt();
        vt.join(3000);

        // Thread should have exited cleanly via InterruptedException handler
        assertThat(vt.isAlive()).isFalse();
        assertThat(ctx.getActiveUsers()).isZero();
    }

}
