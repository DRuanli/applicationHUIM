// File: src/test/java/com/mining/parallel/TopKManagerTest.java
package com.mining.parallel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TopKManager with concurrency testing.
 */
@DisplayName("TopKManager Tests")
class TopKManagerTest {
    
    private TopKManager topKManager;
    
    @BeforeEach
    void setUp() {
        topKManager = new TopKManager(3); // k=3
    }
    
    @Test
    @DisplayName("Should maintain top-K items correctly")
    void shouldMaintainTopKItems() {
        // Add items
        assertThat(topKManager.tryAdd(Set.of(1), 10.0, 0.8)).isTrue();
        assertThat(topKManager.tryAdd(Set.of(2), 20.0, 0.9)).isTrue();
        assertThat(topKManager.tryAdd(Set.of(3), 15.0, 0.7)).isTrue();

        // Now the threshold should be 10.0 (minimum of the three)
        assertThat(topKManager.getThreshold()).isEqualTo(10.0);

        // Adding item with utility 5.0 should fail (below threshold)
        assertThat(topKManager.tryAdd(Set.of(4), 5.0, 0.6)).isFalse();

        // Adding item with utility 12.0 should succeed (above threshold)
        assertThat(topKManager.tryAdd(Set.of(5), 12.0, 0.8)).isTrue();

        // Verify top-K
        var topK = topKManager.getTopK();
        assertThat(topK).hasSize(3);
        assertThat(topK.get(0).getExpectedUtility()).isEqualTo(20.0);
        assertThat(topK.get(1).getExpectedUtility()).isEqualTo(15.0);
        assertThat(topK.get(2).getExpectedUtility()).isEqualTo(12.0);

        // Verify new threshold is 12.0
        assertThat(topKManager.getThreshold()).isEqualTo(12.0);
    }

    @Test
    @DisplayName("Should handle concurrent updates correctly")
    void shouldHandleConcurrentUpdates() throws InterruptedException {
        // Given
        int threadCount = 10;
        int itemsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When - Multiple threads adding items concurrently
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < itemsPerThread; i++) {
                        int itemId = threadId * 1000 + i;
                        double utility = Math.random() * 100;
                        topKManager.tryAdd(Set.of(itemId), utility, 0.5);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Then
        var topK = topKManager.getTopK();
        assertThat(topK).hasSize(3); // Should maintain exactly k items

        // Verify items are properly sorted (with tolerance for floating point)
        for (int i = 1; i < topK.size(); i++) {
            double prevUtility = topK.get(i - 1).getExpectedUtility();
            double currUtility = topK.get(i).getExpectedUtility();

            // Use a small epsilon for floating point comparison
            assertThat(prevUtility).isGreaterThanOrEqualTo(currUtility - 1e-10);
        }

        // Verify successful updates occurred
        assertThat(topKManager.getSuccessfulUpdates().get()).isGreaterThan(0);

        // Verify CAS efficiency is reasonable (should be > 30% in most cases)
        // Lower threshold since high concurrency can cause more retries
        double efficiency = topKManager.getCASEfficiency();
        assertThat(efficiency).isGreaterThanOrEqualTo(0.3);
    }

    @Test
    @DisplayName("Should update duplicate items correctly")
    void shouldUpdateDuplicateItems() {
        // Given
        Set<Integer> items = Set.of(1, 2);

        // When - Add same itemset with different utilities
        assertThat(topKManager.tryAdd(items, 10.0, 0.8)).isTrue();  // Initial add
        assertThat(topKManager.tryAdd(items, 15.0, 0.9)).isTrue();  // Should update (higher utility)
        assertThat(topKManager.tryAdd(items, 8.0, 0.7)).isFalse();  // Should NOT update (lower utility)

        // Then
        var topK = topKManager.getTopK();
        assertThat(topK).hasSize(1);
        assertThat(topK.get(0).getExpectedUtility()).isEqualTo(15.0);
        assertThat(topK.get(0).getProbability()).isEqualTo(0.9); // Should have the max probability
    }
}