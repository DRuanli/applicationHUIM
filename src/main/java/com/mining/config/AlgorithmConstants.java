// File: src/main/java/com/mining/config/AlgorithmConstants.java
package com.mining.config;

/**
 * Global constants for the PTK-HUIM-U algorithm.
 * These constants control various aspects of the mining process.
 */
public final class AlgorithmConstants {

    // ====== Numerical Constants ======

    /**
     * Epsilon value for floating-point comparisons.
     */
    public static final double EPSILON = 1e-10;

    /**
     * Minimum value for log probabilities to avoid underflow.
     */
    public static final double LOG_EPSILON = -700.0;

    /**
     * Default probability value when not specified.
     */
    public static final double DEFAULT_PROBABILITY = 1.0;

    // ====== Performance Tuning ======

    /**
     * Minimum number of items for parallel processing.
     */
    public static final int PARALLEL_THRESHOLD = 30;

    /**
     * Task granularity for fork-join decomposition.
     */
    public static final int TASK_GRANULARITY = 7;

    /**
     * Initial capacity for utility list collections.
     */
    public static final int INITIAL_CAPACITY = 16;

    /**
     * Load factor for hash-based collections.
     */
    public static final float LOAD_FACTOR = 0.75f;

    /**
     * Maximum number of CAS retries before fallback.
     */
    public static final int MAX_CAS_RETRIES = 100;

    // ====== Memory Management ======

    /**
     * Memory check interval in milliseconds.
     */
    public static final long MEMORY_CHECK_INTERVAL = 1000L;

    /**
     * Memory usage threshold for triggering GC (percentage).
     */
    public static final double MEMORY_THRESHOLD = 0.85;

    /**
     * Buffer size for file I/O operations.
     */
    public static final int IO_BUFFER_SIZE = 8192;

    // ====== Pruning Strategies ======

    /**
     * Aggressive pruning factor for early termination.
     */
    public static final double AGGRESSIVE_PRUNING_FACTOR = 0.1;

    /**
     * Minimum support count for itemsets.
     */
    public static final int MIN_SUPPORT_COUNT = 1;

    /**
     * Maximum depth for recursive search.
     */
    public static final int MAX_SEARCH_DEPTH = 100;

    // ====== Logging and Monitoring ======

    /**
     * Progress report interval (number of items).
     */
    public static final int PROGRESS_REPORT_INTERVAL = 10;

    /**
     * Statistics collection interval in milliseconds.
     */
    public static final long STATS_COLLECTION_INTERVAL = 5000L;

    // Private constructor to prevent instantiation
    private AlgorithmConstants() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    /**
     * Checks if two double values are equal within epsilon tolerance.
     *
     * @param a First value
     * @param b Second value
     * @return true if values are equal within epsilon
     */
    public static boolean equalWithinEpsilon(double a, double b) {
        return Math.abs(a - b) < EPSILON;
    }

    /**
     * Checks if a value is greater than another within epsilon tolerance.
     *
     * @param a First value
     * @param b Second value
     * @return true if a > b considering epsilon
     */
    public static boolean greaterThan(double a, double b) {
        return a - b > EPSILON;
    }

    /**
     * Checks if a value is less than another within epsilon tolerance.
     *
     * @param a First value
     * @param b Second value
     * @return true if a < b considering epsilon
     */
    public static boolean lessThan(double a, double b) {
        return b - a > EPSILON;
    }
}