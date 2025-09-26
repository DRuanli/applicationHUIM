package com.mining;

import com.mining.config.MiningConfiguration;
import com.mining.core.model.Itemset;
import com.mining.core.model.Transaction;
import com.mining.engine.MiningEngine;
import com.mining.engine.statistics.MiningStatistics;
import com.mining.io.DataLoader;
import com.mining.io.ResultWriter;
import com.mining.util.MemoryMonitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PTK-HUIM-U±: Enhanced Parallel Top-K High-Utility Itemset Mining
 * from Uncertain Databases with Positive and Negative Utilities
 *
 * This application implements an optimized version of the PTK-HUIM-U algorithm
 * with support for both positive and negative utilities, parallel processing,
 * and advanced pruning strategies.
 *
 * @author Mining Research Team
 * @version 2.0.0
 * @since 2024
 */
@Slf4j
public class PTKHuimApplication {

    private static final String VERSION = "2.0.0";
    private static final String APPLICATION_NAME = "PTK-HUIM-U± Optimized";

    /**
     * Main entry point for the PTK-HUIM-U application.
     *
     * @param args Command line arguments:
     *             [0] - Database file path
     *             [1] - Profit table file path
     *             [2] - K value (number of top itemsets to find)
     *             [3] - Minimum probability threshold
     * @throws IOException if file reading fails
     */
    public static void main(String[] args) {
        try {
            // Validate and parse arguments
            MiningConfiguration config = parseArguments(args);

            // Print application header
            printApplicationHeader();

            // Execute mining process
            executeMining(config);

        } catch (IllegalArgumentException e) {
            log.error("Invalid arguments: {}", e.getMessage());
            printUsage();
            System.exit(1);
        } catch (IOException e) {
            log.error("IO Error during mining: {}", e.getMessage(), e);
            System.exit(2);
        } catch (Exception e) {
            log.error("Unexpected error during mining: {}", e.getMessage(), e);
            System.exit(3);
        }
    }

    /**
     * Parses and validates command line arguments.
     *
     * @param args Command line arguments
     * @return MiningConfiguration object with validated parameters
     * @throws IllegalArgumentException if arguments are invalid
     */
    private static MiningConfiguration parseArguments(String[] args) {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                "Expected 4 arguments, received " + args.length
            );
        }

        String dbFile = args[0];
        String profitFile = args[1];

        // Validate file existence
        validateFile(dbFile, "Database file");
        validateFile(profitFile, "Profit table file");

        // Parse numeric parameters
        int k = parsePositiveInteger(args[2], "K value");
        double minProbability = parseProbability(args[3], "Minimum probability");

        return MiningConfiguration.builder()
            .databaseFile(dbFile)
            .profitFile(profitFile)
            .k(k)
            .minProbability(minProbability)
            .build();
    }

    /**
     * Validates that a file exists and is readable.
     *
     * @param filePath Path to the file
     * @param description Description of the file for error messages
     * @throws IllegalArgumentException if file doesn't exist or isn't readable
     */
    private static void validateFile(String filePath, String description) {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException(description + " path cannot be empty");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException(
                description + " not found: " + filePath
            );
        }
        if (!file.canRead()) {
            throw new IllegalArgumentException(
                description + " is not readable: " + filePath
            );
        }
    }

    /**
     * Parses a positive integer from string.
     *
     * @param value String value to parse
     * @param description Description for error messages
     * @return Parsed positive integer
     * @throws IllegalArgumentException if value is not a positive integer
     */
    private static int parsePositiveInteger(String value, String description) {
        try {
            int intValue = Integer.parseInt(value);
            if (intValue <= 0) {
                throw new IllegalArgumentException(
                    description + " must be positive: " + value
                );
            }
            return intValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                description + " must be a valid integer: " + value
            );
        }
    }

    /**
     * Parses a probability value from string.
     *
     * @param value String value to parse
     * @param description Description for error messages
     * @return Parsed probability value
     * @throws IllegalArgumentException if value is not a valid probability [0,1]
     */
    private static double parseProbability(String value, String description) {
        try {
            double doubleValue = Double.parseDouble(value);
            if (doubleValue < 0.0 || doubleValue > 1.0) {
                throw new IllegalArgumentException(
                    description + " must be between 0 and 1: " + value
                );
            }
            return doubleValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                description + " must be a valid decimal number: " + value
            );
        }
    }

    /**
     * Executes the main mining process.
     *
     * @param config Mining configuration
     * @throws IOException if data loading fails
     */
    private static void executeMining(MiningConfiguration config) throws IOException {
        log.info("Starting mining process with configuration: {}", config);

        // Start memory monitoring
        MemoryMonitor memoryMonitor = new MemoryMonitor();
        memoryMonitor.startMonitoring();

        // Phase 1: Data Loading
        log.info("Loading data files...");
        Instant loadStart = Instant.now();

        DataLoader dataLoader = new DataLoader();
        Map<Integer, Double> profits = dataLoader.readProfitTable(config.getProfitFile());
        List<Transaction> database = dataLoader.readDatabase(config.getDatabaseFile());

        Duration loadTime = Duration.between(loadStart, Instant.now());
        log.info("Data loading completed in {} ms", loadTime.toMillis());
        log.info("Loaded {} transactions with {} items",
            database.size(), profits.size());

        // Phase 2: Mining Engine Initialization
        log.info("Initializing mining engine...");
        MiningEngine miningEngine = MiningEngine.builder()
            .itemProfits(profits)
            .k(config.getK())
            .minProbability(config.getMinProbability())
            .build();

        // Print configuration summary
        ResultWriter.printConfiguration(
            database.size(),
            profits.size(),
            config.getK(),
            config.getMinProbability(),
            Runtime.getRuntime().maxMemory(),
            miningEngine.getParallelism()
        );

        // Phase 3: Execute Mining Algorithm
        log.info("Executing PTK-HUIM-U algorithm...");
        Instant miningStart = Instant.now();

        List<Itemset> topKItemsets = miningEngine.mine(database);

        Duration miningTime = Duration.between(miningStart, Instant.now());
        log.info("Mining completed in {} ms", miningTime.toMillis());

        // Phase 4: Results and Cleanup
        memoryMonitor.stopMonitoring();

        MiningStatistics statistics = miningEngine.getStatistics();
        AtomicLong peakMemory = memoryMonitor.getPeakMemoryUsage();

        // Print results
        ResultWriter.printResults(
            topKItemsets,
            miningTime,
            statistics,
                peakMemory.get(),
            miningEngine.getTopKManager(),
            config.getK()
        );

        // Export results if needed
        if (config.isExportResults()) {
            String outputFile = config.getOutputFile() != null ?
                config.getOutputFile() : "results_" + System.currentTimeMillis() + ".json";
            ResultWriter.exportResults(topKItemsets, statistics, outputFile);
            log.info("Results exported to: {}", outputFile);
        }

        // Cleanup resources
        miningEngine.shutdown();
        log.info("Mining engine shutdown complete");
    }

    /**
     * Prints application header with version information.
     */
    private static void printApplicationHeader() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println(centerText(APPLICATION_NAME, 60));
        System.out.println(centerText("Version " + VERSION, 60));
        System.out.println("=".repeat(60) + "\n");
    }

    /**
     * Centers text within a given width.
     *
     * @param text Text to center
     * @param width Total width
     * @return Centered text string
     */
    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    /**
     * Prints usage instructions.
     */
    private static void printUsage() {
        System.err.println("\nUsage: java -jar ptk-huim-u.jar <database_file> <profit_file> <k> <min_probability>");
        System.err.println("\nParameters:");
        System.err.println("  database_file    - Path to transaction database file");
        System.err.println("  profit_file      - Path to item profit table file");
        System.err.println("  k                - Number of top itemsets to find (positive integer)");
        System.err.println("  min_probability  - Minimum probability threshold (0.0 to 1.0)");
        System.err.println("\nExample:");
        System.err.println("  java -jar ptk-huim-u.jar data/transactions.txt data/profits.txt 10 0.3");
    }
}