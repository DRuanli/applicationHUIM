// File: src/test/java/com/mining/core/model/TransactionTest.java
package com.mining.core.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Transaction class.
 */
@DisplayName("Transaction Tests")
class TransactionTest {

    @Nested
    @DisplayName("Construction Tests")
    class ConstructionTests {
        
        @Test
        @DisplayName("Should create valid transaction")
        void shouldCreateValidTransaction() {
            // Given
            Map<Integer, Integer> items = Map.of(1, 2, 2, 3);
            Map<Integer, Double> probabilities = Map.of(1, 0.8, 2, 0.9);
            
            // When
            Transaction transaction = Transaction.of(1, items, probabilities);
            
            // Then
            assertThat(transaction.getTid()).isEqualTo(1);
            assertThat(transaction.getItems()).containsExactlyInAnyOrderEntriesOf(items);
            assertThat(transaction.getProbabilities()).containsExactlyInAnyOrderEntriesOf(probabilities);
        }
        
        @Test
        @DisplayName("Should reject invalid transaction ID")
        void shouldRejectInvalidTransactionId() {
            // Given
            Map<Integer, Integer> items = Map.of(1, 2);
            Map<Integer, Double> probabilities = Map.of(1, 0.8);
            
            // When/Then
            assertThatThrownBy(() -> Transaction.of(-1, items, probabilities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transaction ID must be positive");
        }
        
        @Test
        @DisplayName("Should reject empty items")
        void shouldRejectEmptyItems() {
            // Given
            Map<Integer, Integer> items = new HashMap<>();
            Map<Integer, Double> probabilities = new HashMap<>();
            
            // When/Then
            assertThatThrownBy(() -> Transaction.of(1, items, probabilities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
        }
        
        @ParameterizedTest
        @ValueSource(doubles = {-0.1, 1.1, 2.0})
        @DisplayName("Should reject invalid probabilities")
        void shouldRejectInvalidProbabilities(double probability) {
            // Given
            Map<Integer, Integer> items = Map.of(1, 2);
            Map<Integer, Double> probabilities = Map.of(1, probability);
            
            // When/Then
            assertThatThrownBy(() -> Transaction.of(1, items, probabilities))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid probability");
        }
    }
    
    @Nested
    @DisplayName("Operation Tests")
    class OperationTests {
        
        @Test
        @DisplayName("Should calculate utility correctly")
        void shouldCalculateUtility() {
            // Given
            Map<Integer, Integer> items = Map.of(1, 2, 2, 3, 3, 1);
            Map<Integer, Double> probabilities = Map.of(1, 0.8, 2, 0.9, 3, 0.7);
            Map<Integer, Double> profits = Map.of(1, 5.0, 2, -3.0, 3, 10.0);
            
            Transaction transaction = Transaction.of(1, items, probabilities);
            
            // When
            double utility = transaction.calculateUtility(profits);
            
            // Then
            // Utility = (2*5.0) + (3*-3.0) + (1*10.0) = 10 - 9 + 10 = 11
            assertThat(utility).isEqualTo(11.0);
        }
        
        @Test
        @DisplayName("Should handle item queries correctly")
        void shouldHandleItemQueries() {
            // Given
            Map<Integer, Integer> items = Map.of(1, 2, 2, 3);
            Map<Integer, Double> probabilities = Map.of(1, 0.8, 2, 0.9);
            
            Transaction transaction = Transaction.of(1, items, probabilities);
            
            // When/Then
            assertThat(transaction.containsItem(1)).isTrue();
            assertThat(transaction.containsItem(2)).isTrue();
            assertThat(transaction.containsItem(3)).isFalse(); // No probability
            assertThat(transaction.containsItem(999)).isFalse();
            
            assertThat(transaction.getQuantity(1)).isEqualTo(2);
            assertThat(transaction.getQuantity(999)).isEqualTo(0);
            
            assertThat(transaction.getProbability(1)).isEqualTo(0.8);
            assertThat(transaction.getProbability(999)).isEqualTo(0.0);
        }
    }
}