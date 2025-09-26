// File: src/main/java/com/mining/config/MiningConfiguration.java
package com.mining.config;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/**
 * Configuration class for PTK-HUIM-U mining parameters.
 * This class encapsulates all configuration needed for the mining process.
 */
@Data
@Builder
public class MiningConfiguration {

    /**
     * Path to the transaction database file.
     */
    @NonNull
    private final String databaseFile;

    /**
     * Path to the profit table file.
     */
    @NonNull
    private final String profitFile;

    /**
     * Number of top-k itemsets to find.
     */
    private final int k;

    /**
     * Minimum probability threshold (0.0 to 1.0).
     */
    private final double minProbability;

    /**
     * Optional output file path for results export.
     */
    @Builder.Default
    private final String outputFile = null;

    /**
     * Flag to enable results export.
     */
    @Builder.Default
    private final boolean exportResults = false;

    /**
     * Number of threads for parallel processing.
     * Default: Available processor count.
     */
    @Builder.Default
    private final int numThreads = Runtime.getRuntime().availableProcessors();

    /**
     * Memory limit in MB for the mining process.
     * Default: 75% of max heap size.
     */
    @Builder.Default
    private final long memoryLimitMB = (Runtime.getRuntime().maxMemory() / 1024 / 1024) * 3 / 4;

    /**
     * Enable debug mode for verbose logging.
     */
    @Builder.Default
    private final boolean debugMode = false;

    /**
     * Validates the configuration parameters.
     *
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public void validate() {
        if (k <= 0) {
            throw new IllegalArgumentException("K must be positive");
        }
        if (minProbability < 0.0 || minProbability > 1.0) {
            throw new IllegalArgumentException("Minimum probability must be between 0 and 1");
        }
        if (numThreads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        if (memoryLimitMB <= 0) {
            throw new IllegalArgumentException("Memory limit must be positive");
        }
    }
}