package com.stormapi.engine.analysis;

import java.util.List;

/**
 * Detects performance degradation using simple linear regression.
 *
 * Given a list of (elapsed_seconds, latency_ms) data points collected
 * during a soak test, computes the regression slope and R² value to
 * determine if latency is increasing over time.
 *
 * Algorithm: Ordinary Least Squares (OLS) linear regression.
 *   slope = (n·Σxy − Σx·Σy) / (n·Σx² − (Σx)²)
 *   intercept = (Σy − slope·Σx) / n
 *   R² = 1 − SS_res / SS_tot
 *
 * Degradation is flagged when BOTH conditions are met:
 *   1. slope > degradationThreshold (latency is increasing)
 *   2. R² > 0.3 (the trend is statistically meaningful, not noise)
 *
 * Thread-safety: stateless utility class — all static methods.
 * Performance: O(n) single-pass computation.
 */
public final class TrendAnalyzer {

    private TrendAnalyzer() {
        // utility class
    }

    /**
     * A single data point for regression: (x = time, y = metric value).
     */
    public record DataPoint(double x, double y) {}

    /**
     * The result of a trend analysis.
     *
     * @param slope              latency change per unit time (ms/sec)
     * @param intercept          y-intercept of the regression line
     * @param rSquared           coefficient of determination (0.0–1.0)
     * @param degradationDetected true if sustained upward drift was detected
     */
    public record TrendResult(
            double slope,
            double intercept,
            double rSquared,
            boolean degradationDetected
    ) {}

    /**
     * Analyzes a list of data points for upward drift.
     *
     * @param points                 list of (x, y) data points
     * @param degradationThresholdMs slope above which degradation is flagged (ms/sec)
     * @return the trend analysis result
     */
    public static TrendResult analyze(List<DataPoint> points, double degradationThresholdMs) {
        if (points == null || points.size() < 2) {
            return new TrendResult(0.0, 0.0, 0.0, false);
        }

        int n = points.size();

        // Single-pass accumulation
        double sumX = 0.0;
        double sumY = 0.0;
        double sumXY = 0.0;
        double sumX2 = 0.0;

        for (DataPoint p : points) {
            sumX += p.x();
            sumY += p.y();
            sumXY += p.x() * p.y();
            sumX2 += p.x() * p.x();
        }

        // Slope and intercept
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-12) {
            // All x values are identical — no trend computable
            double meanY = sumY / n;
            return new TrendResult(0.0, meanY, 0.0, false);
        }

        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;

        // R² — coefficient of determination
        double meanY = sumY / n;
        double ssTot = 0.0;
        double ssRes = 0.0;

        for (DataPoint p : points) {
            double predicted = slope * p.x() + intercept;
            ssRes += (p.y() - predicted) * (p.y() - predicted);
            ssTot += (p.y() - meanY) * (p.y() - meanY);
        }

        double rSquared = (Math.abs(ssTot) < 1e-12) ? 0.0 : 1.0 - (ssRes / ssTot);

        // Degradation detection: slope above threshold AND trend is real (R² > 0.3)
        boolean degradationDetected = slope > degradationThresholdMs && rSquared > 0.3;

        return new TrendResult(slope, intercept, rSquared, degradationDetected);
    }

}
