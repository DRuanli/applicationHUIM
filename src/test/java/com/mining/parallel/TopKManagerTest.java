// File: src/test/java/com/mining/parallel/TopKManagerTest.java
package com.mining.parallel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

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
        assertThat(topKManager.tryAdd(Set.of(4), 5.0, 0.6)).isFalse(); // Too low

        // Verify top-K
        var topK = topKManager.getTopK();
        assertThat(topK).hasSize(3);
        assertThat(topK.get(0).getExpectedUtility()).isEqualTo(20.0);
        assertThat(topK.get(1).getExpectedUtility()).isEqualTo(15.0);
        assertThat(topK.get(2).getExpectedUtility()).isEqualTo(10.0);

        // Verify threshold
        assertThat(topKManager.getThreshold()).isEqualTo(10.0);
    }

    @Test
    @DisplayName("Should handle concurrent updates correctly")
    void shouldHandleConcurrentUpdates() throws InterruptedException {
        // Given
        int threadCount = 10;
        int itemsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // When - Multiple threads adding items concurrently with deterministic utilities
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    for (int i = 0; i < itemsPerThread; i++) {
                        int itemId = threadId * 1000 + i;
                        // Use deterministic utility based on itemId to ensure top-3 are predictable
                        double utility = (itemId % 100) + random.nextDouble() * 10; // Base + small random
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

        // Verify items are properly sorted
        for (int i = 1; i < topK.size(); i++) {
            assertThat(topK.get(i - 1).getExpectedUtility())
                .isGreaterThanOrEqualTo(topK.get(i).getExpectedUtility());
        }

        // Verify CAS operations occurred (should have some successful updates)
        assertThat(topKManager.getSuccessfulUpdates().get()).isGreaterThan(0);

        // Verify the top items have high utilities (items with id ending in 99 should score high)
        assertThat(topK.get(0).getExpectedUtility()).isGreaterThan(99.0); // 99 + random(0-10)
    }

    @Test
    @DisplayName("Should update duplicate items correctly")
    void shouldUpdateDuplicateItems() {
        // Given
        Set<Integer> items = Set.of(1, 2);

        // When - Add same itemset with different utilities
        assertThat(topKManager.tryAdd(items, 10.0, 0.8)).isTrue();
        assertThat(topKManager.tryAdd(items, 15.0, 0.8)).isTrue(); // Should update
        assertThat(topKManager.tryAdd(items, 8.0, 0.8)).isFalse(); // Should not downgrade

        // Then
        var topK = topKManager.getTopK();
        assertThat(topK).hasSize(1);
        assertThat(topK.get(0).getExpectedUtility()).isEqualTo(15.0);
    }
}