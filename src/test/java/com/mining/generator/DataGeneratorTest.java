// File: src/test/java/com/mining/generator/DataGeneratorTest.java
package com.mining.generator;

import com.mining.core.model.Transaction;
import com.mining.io.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for DataGenerator.
 */
class DataGeneratorTest {
    
    @TempDir
    Path tempDir;
    
    private DataGenerator generator;
    private DataLoader dataLoader;
    
    @BeforeEach
    void setUp() {
        generator = new DataGenerator(
            DataGenerator.GeneratorConfig.builder()
                .numTransactions(50)
                .numItems(10)
                .outputDirectory(tempDir.toString())
                .build(),
            42L // Fixed seed for reproducibility
        );
        
        dataLoader = new DataLoader();
    }
    
    @Test
    void shouldGenerateValidDataset() throws Exception {
        // Generate dataset
        var files = generator.generateDataset("test");
        
        // Load and validate database
        List<Transaction> database = dataLoader.readDatabase(files.getDatabaseFile());
        assertThat(database).hasSize(50);
        
        // Validate transactions
        for (Transaction t : database) {
            assertThat(t.size()).isBetween(2, 10);
            
            for (int item : t.getItemIds()) {
                assertThat(item).isBetween(1, 10);
                assertThat(t.getQuantity(item)).isBetween(1, 5);
                assertThat(t.getProbability(item)).isBetween(0.3, 1.0);
            }
        }
        
        // Load and validate profit table
        Map<Integer, Double> profits = dataLoader.readProfitTable(files.getProfitFile());
        assertThat(profits).hasSize(10);
        
        // Check profit range
        for (double profit : profits.values()) {
            assertThat(profit).isBetween(-10.0, 50.0);
        }
        
        // Check negative utility ratio
        long negativeCount = profits.values().stream()
            .filter(p -> p < 0)
            .count();
        double ratio = (double) negativeCount / profits.size();
        assertThat(ratio).isCloseTo(0.1, within(0.2)); // Allow some variance
    }
}