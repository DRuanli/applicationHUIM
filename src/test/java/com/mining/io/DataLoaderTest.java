// File: src/test/java/com/mining/io/DataLoaderTest.java
package com.mining.io;

import com.mining.core.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for DataLoader.
 */
@DisplayName("DataLoader Tests")
class DataLoaderTest {
    
    private DataLoader dataLoader;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        dataLoader = new DataLoader();
    }
    
    @Test
    @DisplayName("Should read profit table correctly")
    void shouldReadProfitTable() throws IOException {
        // Given
        Path profitFile = tempDir.resolve("profits.txt");
        List<String> lines = List.of(
            "1 5.0",
            "2 10.5",
            "3 -2.0",
            "# Comment line",
            "4 8.3"
        );
        Files.write(profitFile, lines);
        
        // When
        Map<Integer, Double> profits = dataLoader.readProfitTable(profitFile.toString());
        
        // Then
        assertThat(profits).hasSize(4);
        assertThat(profits.get(1)).isEqualTo(5.0);
        assertThat(profits.get(2)).isEqualTo(10.5);
        assertThat(profits.get(3)).isEqualTo(-2.0);
        assertThat(profits.get(4)).isEqualTo(8.3);
    }
    
    @Test
    @DisplayName("Should read transaction database correctly")
    void shouldReadTransactionDatabase() throws IOException {
        // Given
        Path dbFile = tempDir.resolve("database.txt");
        List<String> lines = List.of(
            "1:2:0.9 2:4:0.8",
            "1:1:0.7 3:3:0.9 4:1:0.6",
            "# Comment",
            "2:2:0.8 3:2:0.7"
        );
        Files.write(dbFile, lines);
        
        // When
        List<Transaction> database = dataLoader.readDatabase(dbFile.toString());
        
        // Then
        assertThat(database).hasSize(3);
        
        // Verify first transaction
        Transaction t1 = database.get(0);
        assertThat(t1.getTid()).isEqualTo(1);
        assertThat(t1.getQuantity(1)).isEqualTo(2);
        assertThat(t1.getQuantity(2)).isEqualTo(4);
        assertThat(t1.getProbability(1)).isEqualTo(0.9);
        assertThat(t1.getProbability(2)).isEqualTo(0.8);
    }
    
    @Test
    @DisplayName("Should handle missing files gracefully")
    void shouldHandleMissingFiles() {
        // Given
        String nonExistentFile = tempDir.resolve("missing.txt").toString();
        
        // When/Then
        assertThatThrownBy(() -> dataLoader.readProfitTable(nonExistentFile))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("not found");
        
        assertThatThrownBy(() -> dataLoader.readDatabase(nonExistentFile))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("not found");
    }
}