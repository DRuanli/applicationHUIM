// File: src/main/java/com/mining/io/DataLoader.java
package com.mining.io;

import com.google.common.base.Preconditions;
import com.mining.config.AlgorithmConstants;
import com.mining.config.FileFormat;
import com.mining.core.model.Transaction;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data loader for reading transaction databases and profit tables.
 * Supports multiple file formats and provides robust error handling.
 */
@Slf4j
public class DataLoader {

    private static final String DEFAULT_DELIMITER = "\\s+";
    private static final String CSV_DELIMITER = ",";
    private static final String ITEM_SEPARATOR = ":";

    /**
     * Reads profit table from file.
     * Format: item_id profit_value
     *
     * @param filename Path to profit file
     * @return Map of item IDs to profit values
     * @throws IOException if file reading fails
     */
    public Map<Integer, Double> readProfitTable(String filename) throws IOException {
        Preconditions.checkNotNull(filename, "Filename cannot be null");

        Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Profit file not found: " + filename);
        }

        log.info("Reading profit table from: {}", filename);

        FileFormat format = FileFormat.fromFilename(filename);
        Map<Integer, Double> profits = new HashMap<>();

        try {
            switch (format) {
                case CSV:
                    profits = readProfitTableCSV(path);
                    break;
                case JSON:
                    profits = readProfitTableJSON(path);
                    break;
                default:
                    profits = readProfitTableText(path);
            }
        } catch (Exception e) {
            log.error("Error reading profit table: {}", e.getMessage());
            throw new IOException("Failed to read profit table from " + filename, e);
        }

        validateProfitTable(profits);
        log.info("Loaded {} items with profit values", profits.size());

        return profits;
    }

    /**
     * Reads profit table in text format.
     */
    private Map<Integer, Double> readProfitTableText(Path path) throws IOException {
        Map<Integer, Double> profits = new HashMap<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (StringUtils.isBlank(line) || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                String[] parts = line.split(DEFAULT_DELIMITER);
                if (parts.length != 2) {
                    log.warn("Invalid profit entry at line {}: {}", lineNumber, line);
                    continue;
                }

                try {
                    int item = Integer.parseInt(parts[0]);
                    double profit = Double.parseDouble(parts[1]);

                    if (profits.containsKey(item)) {
                        log.warn("Duplicate item {} at line {}, overwriting", item, lineNumber);
                    }

                    profits.put(item, profit);
                } catch (NumberFormatException e) {
                    log.warn("Invalid number format at line {}: {}", lineNumber, line);
                }
            }
        }

        return profits;
    }

    /**
     * Reads profit table in CSV format.
     */
    private Map<Integer, Double> readProfitTableCSV(Path path) throws IOException, CsvException {
        Map<Integer, Double> profits = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(path.toFile()))) {
            List<String[]> records = reader.readAll();

            // Skip header if present
            int startIndex = 0;
            if (!records.isEmpty() && !StringUtils.isNumeric(records.get(0)[0])) {
                startIndex = 1;
            }

            for (int i = startIndex; i < records.size(); i++) {
                String[] record = records.get(i);
                if (record.length >= 2) {
                    try {
                        int item = Integer.parseInt(record[0].trim());
                        double profit = Double.parseDouble(record[1].trim());
                        profits.put(item, profit);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid CSV entry at row {}: {}", i + 1, Arrays.toString(record));
                    }
                }
            }
        }

        return profits;
    }

    /**
     * Reads profit table in JSON format.
     */
    private Map<Integer, Double> readProfitTableJSON(Path path) throws IOException {
        // Simplified JSON reading - in production, use Jackson
        String content = Files.readString(path, StandardCharsets.UTF_8);
        Map<Integer, Double> profits = new HashMap<>();

        // Basic JSON parsing for format: {"1": 10.5, "2": -5.0, ...}
        content = content.trim();
        if (content.startsWith("{") && content.endsWith("}")) {
            content = content.substring(1, content.length() - 1);
            String[] entries = content.split(",");

            for (String entry : entries) {
                String[] parts = entry.split(":");
                if (parts.length == 2) {
                    try {
                        int item = Integer.parseInt(parts[0].trim().replace("\"", ""));
                        double profit = Double.parseDouble(parts[1].trim());
                        profits.put(item, profit);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid JSON entry: {}", entry);
                    }
                }
            }
        }

        return profits;
    }

    /**
     * Validates profit table entries.
     */
    private void validateProfitTable(Map<Integer, Double> profits) {
        if (profits.isEmpty()) {
            throw new IllegalArgumentException("Profit table is empty");
        }

        long negativeCount = profits.values().stream()
            .filter(p -> p < 0)
            .count();

        if (negativeCount > 0) {
            log.info("Profit table contains {} items with negative utilities", negativeCount);
        }
    }

    /**
     * Reads transaction database from file.
     * Format: item:quantity:probability item:quantity:probability ...
     *
     * @param filename Path to database file
     * @return List of transactions
     * @throws IOException if file reading fails
     */
    public List<Transaction> readDatabase(String filename) throws IOException {
        Preconditions.checkNotNull(filename, "Filename cannot be null");

        Path path = Paths.get(filename);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Database file not found: " + filename);
        }

        log.info("Reading transaction database from: {}", filename);

        List<Transaction> database = new ArrayList<>();
        AtomicInteger tidCounter = new AtomicInteger(1);

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                if (StringUtils.isBlank(line) || line.startsWith("#")) {
                    continue; // Skip empty lines and comments
                }

                Transaction transaction = parseTransaction(line, tidCounter.getAndIncrement(), lineNumber);
                if (transaction != null) {
                    database.add(transaction);
                }
            }
        } catch (Exception e) {
            log.error("Error reading database: {}", e.getMessage());
            throw new IOException("Failed to read database from " + filename, e);
        }

        validateDatabase(database);
        log.info("Loaded {} transactions", database.size());

        return database;
    }

    /**
     * Parses a single transaction from a line.
     */
    private Transaction parseTransaction(String line, int tid, int lineNumber) {
        Map<Integer, Integer> items = new HashMap<>();
        Map<Integer, Double> probabilities = new HashMap<>();

        String[] entries = line.split(DEFAULT_DELIMITER);

        for (String entry : entries) {
            String[] parts = entry.split(ITEM_SEPARATOR);

            if (parts.length < 2 || parts.length > 3) {
                log.warn("Invalid entry '{}' in transaction at line {}", entry, lineNumber);
                continue;
            }

            try {
                int item = Integer.parseInt(parts[0]);
                int quantity = Integer.parseInt(parts[1]);
                double probability = parts.length == 3 ?
                    Double.parseDouble(parts[2]) : AlgorithmConstants.DEFAULT_PROBABILITY;

                if (quantity <= 0) {
                    log.warn("Invalid quantity {} for item {} at line {}", quantity, item, lineNumber);
                    continue;
                }

                if (probability < 0 || probability > 1) {
                    log.warn("Invalid probability {} for item {} at line {}", probability, item, lineNumber);
                    continue;
                }

                items.put(item, quantity);
                probabilities.put(item, probability);

            } catch (NumberFormatException e) {
                log.warn("Invalid number format in entry '{}' at line {}", entry, lineNumber);
            }
        }

        if (items.isEmpty()) {
            log.warn("Empty transaction at line {}", lineNumber);
            return null;
        }

        return Transaction.of(tid, items, probabilities);
    }

    /**
     * Validates the transaction database.
     */
    private void validateDatabase(List<Transaction> database) {
        if (database.isEmpty()) {
            throw new IllegalArgumentException("Transaction database is empty");
        }

        // Compute statistics
        IntSummaryStatistics itemStats = database.stream()
            .mapToInt(Transaction::size)
            .summaryStatistics();

        log.info("Database statistics: {} transactions, avg items per transaction: {:.2f}",
            database.size(), itemStats.getAverage());
    }
}