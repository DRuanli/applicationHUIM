// File: src/main/java/com/mining/generator/DataGenerator.java
package com.mining.generator;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Synthetic data generator for creating test datasets.
 * Generates transaction databases and profit tables with configurable parameters.
 */
@Slf4j
public class DataGenerator {

    private final Random random;
    private final GeneratorConfig config;

    @Data
    @Builder
    public static class GeneratorConfig {
        @Builder.Default
        private int numTransactions = 1000;

        @Builder.Default
        private int numItems = 100;

        @Builder.Default
        private int minItemsPerTransaction = 2;

        @Builder.Default
        private int maxItemsPerTransaction = 10;

        @Builder.Default
        private int minQuantity = 1;

        @Builder.Default
        private int maxQuantity = 5;

        @Builder.Default
        private double minProbability = 0.3;

        @Builder.Default
        private double maxProbability = 1.0;

        @Builder.Default
        private double minProfit = -10.0;

        @Builder.Default
        private double maxProfit = 50.0;

        @Builder.Default
        private double negativeUtilityRatio = 0.1; // 10% negative utilities

        @Builder.Default
        private boolean useZipfDistribution = false;

        @Builder.Default
        private double zipfExponent = 1.5;

        @Builder.Default
        private String outputDirectory = "data/generated";
    }

    public DataGenerator(GeneratorConfig config) {
        this(config, new Random());
    }

    public DataGenerator(GeneratorConfig config, long seed) {
        this(config, new Random(seed));
    }

    public DataGenerator(GeneratorConfig config, Random random) {
        this.config = config;
        this.random = random;
    }

    /**
     * Generate complete dataset (database + profit table).
     */
    public DatasetFiles generateDataset(String name) throws IOException {
        log.info("Generating dataset '{}' with {} transactions and {} items",
            name, config.numTransactions, config.numItems);

        // Create output directory
        Path outputDir = Paths.get(config.outputDirectory);
        Files.createDirectories(outputDir);

        // Generate files
        String dbFile = generateDatabase(outputDir.resolve(name + "_db.txt"));
        String profitFile = generateProfitTable(outputDir.resolve(name + "_profits.txt"));

        log.info("Dataset '{}' generated successfully", name);

        return new DatasetFiles(dbFile, profitFile);
    }

    /**
     * Generate transaction database.
     */
    public String generateDatabase(Path outputPath) throws IOException {
        log.debug("Generating transaction database: {}", outputPath);

        List<Integer> itemPool = createItemPool();

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("# Transaction Database");
            writer.newLine();
            writer.write(String.format("# Transactions: %d, Items: %d",
                config.numTransactions, config.numItems));
            writer.newLine();
            writer.write("# Format: item:quantity:probability ...");
            writer.newLine();
            writer.newLine();

            for (int tid = 1; tid <= config.numTransactions; tid++) {
                String transaction = generateTransaction(itemPool);
                writer.write(transaction);
                writer.newLine();

                if (tid % 1000 == 0) {
                    log.debug("Generated {} transactions", tid);
                }
            }
        }

        log.info("Generated {} transactions to {}",
            config.numTransactions, outputPath.getFileName());

        return outputPath.toString();
    }

    /**
     * Generate profit table.
     */
    public String generateProfitTable(Path outputPath) throws IOException {
        log.debug("Generating profit table: {}", outputPath);

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            writer.write("# Profit Table");
            writer.newLine();
            writer.write(String.format("# Items: %d, Negative ratio: %.2f",
                config.numItems, config.negativeUtilityRatio));
            writer.newLine();
            writer.write("# Format: item profit");
            writer.newLine();
            writer.newLine();

            for (int item = 1; item <= config.numItems; item++) {
                double profit = generateProfit(item);
                writer.write(String.format(java.util.Locale.US,"%d %.2f", item, profit));
                writer.newLine();
            }
        }

        log.info("Generated profits for {} items to {}",
            config.numItems, outputPath.getFileName());

        return outputPath.toString();
    }

    /**
     * Generate a single transaction.
     */
    private String generateTransaction(List<Integer> itemPool) {
        int numItems = random.nextInt(
            config.maxItemsPerTransaction - config.minItemsPerTransaction + 1
        ) + config.minItemsPerTransaction;

        // Select items (with or without Zipf distribution)
        Set<Integer> selectedItems = new HashSet<>();
        while (selectedItems.size() < numItems) {
            int item = config.useZipfDistribution ?
                selectItemZipf(itemPool) : selectItemUniform(itemPool);
            selectedItems.add(item);
        }

        // Build transaction string
        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Integer item : selectedItems) {
            if (!first) {
                sb.append(" ");
            }

            int quantity = random.nextInt(
                config.maxQuantity - config.minQuantity + 1
            ) + config.minQuantity;

            double probability = config.minProbability +
                (config.maxProbability - config.minProbability) * random.nextDouble();

            sb.append(String.format(java.util.Locale.US,"%d:%d:%.3f", item, quantity, probability));
            first = false;
        }

        return sb.toString();
    }

    /**
     * Generate profit value for an item.
     */
    private double generateProfit(int item) {
        // Determine if this should be negative
        boolean isNegative = random.nextDouble() < config.negativeUtilityRatio;

        if (isNegative) {
            // Generate negative profit
            return config.minProfit + (0 - config.minProfit) * random.nextDouble();
        } else {
            // Generate positive profit
            return random.nextDouble() * config.maxProfit;
        }
    }

    /**
     * Create item pool for selection.
     */
    private List<Integer> createItemPool() {
        List<Integer> pool = new ArrayList<>();
        for (int i = 1; i <= config.numItems; i++) {
            pool.add(i);
        }
        return pool;
    }

    /**
     * Select item with uniform distribution.
     */
    private int selectItemUniform(List<Integer> itemPool) {
        return itemPool.get(random.nextInt(itemPool.size()));
    }

    /**
     * Select item with Zipf distribution (power law).
     */
    private int selectItemZipf(List<Integer> itemPool) {
        double[] probabilities = new double[itemPool.size()];
        double sum = 0.0;

        // Calculate Zipf probabilities
        for (int i = 0; i < itemPool.size(); i++) {
            probabilities[i] = 1.0 / Math.pow(i + 1, config.zipfExponent);
            sum += probabilities[i];
        }

        // Normalize
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= sum;
        }

        // Select based on cumulative probability
        double rand = random.nextDouble();
        double cumulative = 0.0;

        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (rand <= cumulative) {
                return itemPool.get(i);
            }
        }

        return itemPool.get(itemPool.size() - 1);
    }

    /**
     * Generate multiple datasets with different characteristics.
     */
    public static void generateStandardDatasets() throws IOException {
        log.info("Generating standard test datasets...");

        // Small dataset
        DataGenerator smallGen = new DataGenerator(
            GeneratorConfig.builder()
                .numTransactions(100)
                .numItems(20)
                .maxItemsPerTransaction(5)
                .outputDirectory("data/test")
                .build()
        );
        smallGen.generateDataset("small");

        // Medium dataset
        DataGenerator mediumGen = new DataGenerator(
            GeneratorConfig.builder()
                .numTransactions(10000)
                .numItems(100)
                .maxItemsPerTransaction(10)
                .outputDirectory("data/test")
                .build()
        );
        mediumGen.generateDataset("medium");

        // Large dataset with Zipf distribution
        DataGenerator largeGen = new DataGenerator(
            GeneratorConfig.builder()
                .numTransactions(100000)
                .numItems(500)
                .maxItemsPerTransaction(15)
                .useZipfDistribution(true)
                .zipfExponent(1.5)
                .outputDirectory("data/test")
                .build()
        );
        largeGen.generateDataset("large");

        // Dense dataset
        DataGenerator denseGen = new DataGenerator(
            GeneratorConfig.builder()
                .numTransactions(5000)
                .numItems(50)
                .minItemsPerTransaction(15)
                .maxItemsPerTransaction(25)
                .outputDirectory("data/test")
                .build()
        );
        denseGen.generateDataset("dense");

        // Sparse dataset
        DataGenerator sparseGen = new DataGenerator(
            GeneratorConfig.builder()
                .numTransactions(10000)
                .numItems(1000)
                .minItemsPerTransaction(2)
                .maxItemsPerTransaction(5)
                .outputDirectory("data/test")
                .build()
        );
        sparseGen.generateDataset("sparse");

        log.info("All standard datasets generated successfully");
    }

    /**
     * Dataset file paths.
     */
    @Data
    public static class DatasetFiles {
        private final String databaseFile;
        private final String profitFile;
    }

    /**
     * Main method for standalone execution.
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                // Generate standard datasets
                generateStandardDatasets();
            } else if (args.length == 3) {
                // Custom dataset: name numTransactions numItems
                String name = args[0];
                int numTrans = Integer.parseInt(args[1]);
                int numItems = Integer.parseInt(args[2]);

                DataGenerator generator = new DataGenerator(
                    GeneratorConfig.builder()
                        .numTransactions(numTrans)
                        .numItems(numItems)
                        .build()
                );

                generator.generateDataset(name);
            } else {
                System.err.println("Usage: DataGenerator [name numTransactions numItems]");
                System.err.println("   or: DataGenerator (generates standard datasets)");
                System.exit(1);
            }
        } catch (Exception e) {
            log.error("Error generating data", e);
            System.exit(1);
        }
    }
}