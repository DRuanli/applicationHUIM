// File: src/test/java/com/mining/core/model/ItemsetTest.java
package com.mining.core.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Itemset class.
 */
@DisplayName("Itemset Tests")
class ItemsetTest {
    
    @Test
    @DisplayName("Should create and compare itemsets correctly")
    void shouldCreateAndCompareItemsets() {
        // Given
        Itemset itemset1 = new Itemset(Set.of(1, 2, 3), 100.0, 0.8, 5);
        Itemset itemset2 = new Itemset(Set.of(2, 3, 4), 90.0, 0.9, 3);
        Itemset itemset3 = new Itemset(Set.of(1, 2, 3), 100.0, 0.8, 5);
        
        // When/Then - Equality based on items only
        assertThat(itemset1).isEqualTo(itemset3);
        assertThat(itemset1).isNotEqualTo(itemset2);
        
        // Comparison based on utility first
        assertThat(itemset1.compareTo(itemset2)).isLessThan(0); // 100 > 90, but reversed
    }
    
    @Test
    @DisplayName("Should perform set operations correctly")
    void shouldPerformSetOperations() {
        // Given
        Itemset itemset1 = new Itemset(Set.of(1, 2, 3), 100.0, 0.8);
        Itemset itemset2 = new Itemset(Set.of(2, 3), 50.0, 0.9);
        Itemset itemset3 = new Itemset(Set.of(4, 5), 60.0, 0.7);
        
        // When/Then
        assertThat(itemset1.contains(itemset2)).isTrue();
        assertThat(itemset2.contains(itemset1)).isFalse();
        assertThat(itemset1.contains(itemset3)).isFalse();
        
        assertThat(itemset1.containsItem(2)).isTrue();
        assertThat(itemset1.containsItem(5)).isFalse();
    }
    
    @Test
    @DisplayName("Should join itemsets correctly")
    void shouldJoinItemsets() {
        // Given
        Itemset itemset1 = new Itemset(Set.of(1, 2), 50.0, 0.8);
        Itemset itemset2 = new Itemset(Set.of(3, 4), 40.0, 0.7);
        
        // When
        Itemset joined = itemset1.join(itemset2, 85.0, 0.6);
        
        // Then
        assertThat(joined.getItems()).containsExactlyInAnyOrder(1, 2, 3, 4);
        assertThat(joined.getExpectedUtility()).isEqualTo(85.0);
        assertThat(joined.getProbability()).isEqualTo(0.6);
    }
}