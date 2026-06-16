package com.stormapi.engine.user;

import com.stormapi.test.model.TestConfig;
import com.stormapi.test.model.HttpMethod;
import com.stormapi.test.model.TestType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ThinkTimeStrategyTest {

    // --- NoThinkTimeStrategy ---

    @Test
    void noThinkTime_returnsZero() {
        assertThat(NoThinkTimeStrategy.INSTANCE.getDelayMs()).isZero();
    }

    @Test
    void noThinkTime_applyReturnsImmediately() throws InterruptedException {
        long start = System.currentTimeMillis();
        NoThinkTimeStrategy.INSTANCE.apply();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(50); // should be ~0ms
    }

    // --- ConstantThinkTimeStrategy ---

    @Test
    void constantThinkTime_returnsConfiguredDelay() {
        var strategy = new ConstantThinkTimeStrategy(500);
        assertThat(strategy.getDelayMs()).isEqualTo(500);
    }

    @Test
    void constantThinkTime_apply_sleepsCorrectDuration() throws InterruptedException {
        var strategy = new ConstantThinkTimeStrategy(100);

        long start = System.currentTimeMillis();
        strategy.apply();
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isBetween(80L, 200L); // 100ms ± tolerance
    }

    @Test
    void constantThinkTime_negativeDelay_throws() {
        assertThatThrownBy(() -> new ConstantThinkTimeStrategy(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    // --- RandomThinkTimeStrategy ---

    @Test
    void randomThinkTime_withinBounds() {
        var strategy = new RandomThinkTimeStrategy(100, 200);

        for (int i = 0; i < 100; i++) {
            long delay = strategy.getDelayMs();
            assertThat(delay).isBetween(100L, 200L);
        }
    }

    @Test
    void randomThinkTime_invalidBounds_throws() {
        assertThatThrownBy(() -> new RandomThinkTimeStrategy(200, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be >=");
    }

    @Test
    void randomThinkTime_negativeMin_throws() {
        assertThatThrownBy(() -> new RandomThinkTimeStrategy(-1, 100))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    @Test
    void randomThinkTime_equalBounds_returnsExactValue() {
        var strategy = new RandomThinkTimeStrategy(150, 150);
        assertThat(strategy.getDelayMs()).isEqualTo(150);
    }

    // --- Factory method ---

    @Test
    void fromConfig_zeroThinkTime_returnsNoThinkTime() {
        TestConfig config = TestConfig.builder()
                .name("test").targetUrl("https://example.com")
                .httpMethod(HttpMethod.GET).testType(TestType.LOAD)
                .virtualUsers(1).durationSeconds(10)
                .thinkTimeMs(0).build();

        ThinkTimeStrategy strategy = ThinkTimeStrategy.fromConfig(config);

        assertThat(strategy).isInstanceOf(NoThinkTimeStrategy.class);
    }

    @Test
    void fromConfig_positiveThinkTime_returnsConstant() {
        TestConfig config = TestConfig.builder()
                .name("test").targetUrl("https://example.com")
                .httpMethod(HttpMethod.GET).testType(TestType.LOAD)
                .virtualUsers(1).durationSeconds(10)
                .thinkTimeMs(500).build();

        ThinkTimeStrategy strategy = ThinkTimeStrategy.fromConfig(config);

        assertThat(strategy).isInstanceOf(ConstantThinkTimeStrategy.class);
        assertThat(strategy.getDelayMs()).isEqualTo(500);
    }

}
