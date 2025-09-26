// File: src/main/java/com/mining/io/ResultWriter.java
package com.mining.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.mining.core.model.Itemset;
import com.mining.engine.statistics.MiningStatistics;
import com.mining.parallel.TopKManager;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Result writer for formatting and exporting mining results.
 * Supports multiple output formats and provides detailed statistics.
 */
@Slf4j
public class ResultWriter {

    private static final String SEPARATOR = "=".repeat(80);
    private static final String SUB_SEPARATOR = "-".repeat(60);
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Prints mining configuration summary.
     */
    public static void printConfiguration(int databaseSize, int itemCount, int k,
                                         double minProbability, long maxMemory, int threadCount) {
        System.out.println(SEPARATOR);
        System.out.println(centerText("MINING CONFIGURATION", 80));
        System.out.println(SEPARATOR);

        System.out.printf("Database Size:        %,d transactions%n", databaseSize);
        System.out.printf("Number of Items:      %,d items%n", itemCount);
        System.out.printf("Top-K Target:         %d itemsets%n", k);
        System.out.printf("Min Probability:      %.4f%n", minProbability);
        System.out.printf("Available Memory:     %,d MB%n", maxMemory / 1024 / 1024);
        System.out.printf("Thread Pool Size:     %d threads%n", threadCount);
        System.out.printf("Start Time:           %s%n", LocalDateTime.now().format(TIMESTAMP_FORMAT));

        System.out.println(SEPARATOR);
    }

    /**
     * Prints mining results and statistics.
     */
    public static void printResults(List<Itemset> topK, Duration executionTime,
                                   MiningStatistics statistics, long peakMemoryUsage,
                                   TopKManager topKManager, int k) {
        System.out.println();
        System.out.println(SEPARATOR);
        System.out.println(centerText("MINING RESULTS", 80));
        System.out.println(SEPARATOR);

        // Performance metrics
        System.out.println("\n" + centerText("Performance Metrics", 60));
        System.out.println(SUB_SEPARATOR);
        System.out.printf("Execution Time:       %,d ms (%.2f seconds)%n",
            executionTime.toMillis(), executionTime.toMillis() / 1000.0);
        System.out.printf("Peak Memory Usage:    %,d MB%n", peakMemoryUsage / 1024 / 1024);
        System.out.printf("Final Threshold:      %.6f%n", topKManager.getThreshold());
        System.out.printf("Itemsets Found:       %d / %d%n", topK.size(), k);

        // Mining statistics
        System.out.println("\n" + centerText("Mining Statistics", 60));
        System.out.println(SUB_SEPARATOR);
        statistics.printFormattedStatistics();

        // Concurrency statistics
        System.out.println("\n" + centerText("Concurrency Performance", 60));
        System.out.println(SUB_SEPARATOR);
        printConcurrencyStatistics(topKManager);

        // Top-K itemsets
        System.out.println("\n" + centerText(String.format("Top-%d Itemsets", k), 60));
        System.out.println(SUB_SEPARATOR);
        printTopKItemsets(topK);

        System.out.println(SEPARATOR);
    }

    /**
     * Prints concurrency statistics.
     */
    private static void printConcurrencyStatistics(TopKManager topKManager) {
        long successfulUpdates = topKManager.getSuccessfulUpdates().get();
        long casRetries = topKManager.getCasRetries().get();
        long totalOperations = successfulUpdates + casRetries;

        System.out.printf("Successful Updates:   %,d%n", successfulUpdates);
        System.out.printf("CAS Retries:          %,d%n", casRetries);

        if (totalOperations > 0) {
            double efficiency = (double) successfulUpdates / totalOperations * 100;
            System.out.printf("CAS Efficiency:       %.2f%%%n", efficiency);
        }
    }

    /**
     * Prints top-K itemsets in formatted table.
     */
    private static void printTopKItemsets(List<Itemset> topK) {
        if (topK.isEmpty()) {
            System.out.println("No itemsets found meeting the criteria.");
            return;
        }

        // Table header
        System.out.printf("%-5s %-30s %12s %12s %8s%n",
            "Rank", "Itemset", "Utility", "Probability", "Support");
        System.out.println("-".repeat(70));

        int rank = 1;
        for (Itemset itemset : topK) {
            String itemsStr = formatItemset(itemset.getItems(), 30);
            System.out.printf("%-5d %-30s %12.4f %12.4f %8d%n",
                rank++,
                itemsStr,
                itemset.getExpectedUtility(),
                itemset.getProbability(),
                itemset.getSupport()
            );
        }
    }

    /**
     * Formats itemset for display.
     */
    private static String formatItemset(Set<Integer> items, int maxLength) {
        String itemsStr = items.toString();
        if (itemsStr.length() > maxLength) {
            itemsStr = itemsStr.substring(0, maxLength - 3) + "...";
        }
        return itemsStr;
    }

    /**
     * Exports results to file in specified format.
     */
    public static void exportResults(List<Itemset> topK, MiningStatistics statistics,
                                    String outputFile) throws IOException {
        Path outputPath = Paths.get(outputFile);
        String extension = getFileExtension(outputFile);

        log.info("Exporting results to: {}", outputFile);

        switch (extension.toLowerCase()) {
            case "json":
                exportToJSON(topK, statistics, outputPath);
                break;
            case "csv":
                exportToCSV(topK, outputPath);
                break;
            default:
                exportToText(topK, statistics, outputPath);
        }
    }

    /**
     * Exports results to JSON format.
     */
    private static void exportToJSON(List<Itemset> topK, MiningStatistics statistics,
                                    Path outputPath) throws IOException {
        Map<String, Object> results = new HashMap<>();
        results.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FORMAT));
        results.put("topKItemsets", topK);
        results.put("statistics", statistics.toMap());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(outputPath.toFile(), results);

        log.info("Results exported to JSON: {}", outputPath);
    }

    /**
     * Exports results to CSV format.
     */
    private static void exportToCSV(List<Itemset> topK, Path outputPath) throws IOException {
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputPath.toFile()))) {
            // Header
            String[] header = {"Rank", "Itemset", "Utility", "Probability", "Support"};
            writer.writeNext(header);

            // Data rows
            int rank = 1;
            for (Itemset itemset : topK) {
                String[] row = {
                    String.valueOf(rank++),
                    itemset.getItems().toString(),
                    String.format("%.6f", itemset.getExpectedUtility()),
                    String.format("%.6f", itemset.getProbability()),
                    String.valueOf(itemset.getSupport())
                };
                writer.writeNext(row);
            }
        }

        log.info("Results exported to CSV: {}", outputPath);
    }

    /**
     * Exports results to text format.
     */
    private static void exportToText(List<Itemset> topK, MiningStatistics statistics,
                                    Path outputPath) throws IOException {
        List<String> lines = new ArrayList<>();

        lines.add("PTK-HUIM-U Mining Results");
        lines.add("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
        lines.add(SEPARATOR);
        lines.add("");
        lines.add("Top-K Itemsets:");
        lines.add(SUB_SEPARATOR);

        int rank = 1;
        for (Itemset itemset : topK) {
            lines.add(String.format("%d. %s", rank++, itemset.toFormattedString()));
        }

        lines.add("");
        lines.add("Statistics:");
        lines.add(SUB_SEPARATOR);
        lines.addAll(statistics.toStringList());

        Files.write(outputPath, lines);
        log.info("Results exported to text: {}", outputPath);
    }

    /**
     * Centers text within specified width.
     */
    private static String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return StringUtils.repeat(' ', Math.max(0, padding)) + text;
    }

    /**
     * Gets file extension from filename.
     */
    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }
}