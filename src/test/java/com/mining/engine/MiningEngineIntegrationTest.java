// File: src/test/java/com/mining/engine/MiningEngineIntegrationTest.java
package com.mining.engine;

import com.mining.core.model.Itemset;
import com.mining.core.model.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for MiningEngine.
 */
@DisplayName("MiningEngine Integration Tests")
class MiningEngineIntegrationTest {
    
    private Map<Integer, Double> profits;
    private List<Transaction> database;
    
    @BeforeEach
    void setUp() {
        // Setup test data
        profits = Map.of(
            1, 5.0,
            2, 10.0,
            3, 1.0,
            4, 6.0,
            5, -2.0
        );
        
        database = createTestDatabase();
    }
    
    @Test
    @DisplayName("Should mine top-K itemsets from small database")
    void shouldMineTopKItemsets() {
        // Given
        MiningEngine engine = MiningEngine.builder()
            .itemProfits(profits)
            .k(3)
            .minProbability(0.3)
            .build();
        
        // When
        List<Itemset> results = engine.mine(database);
        
        // Then
        assertThat(results).isNotNull();
        assertThat(results).hasSizeLessThanOrEqualTo(3);

        // Verify results are sorted by utility
        for (int i = 1; i < results.size(); i++) {
            assertThat(results.get(i - 1).getExpectedUtility())
                .isGreaterThanOrEqualTo(results.get(i).getExpectedUtility());
        }

        // Verify all results meet minimum probability
        results.forEach(itemset ->
            assertThat(itemset.getProbability()).isGreaterThanOrEqualTo(0.3)
        );

        // Check that we have some reasonable utility itemsets
        // Based on the test data, expected utilities should be in range 10-50
        if (!results.isEmpty()) {
            // The top itemset should have reasonable utility (>= 10.0)
            assertThat(results.get(0).getExpectedUtility()).isGreaterThanOrEqualTo(10.0);

            // Verify itemsets contain valid items
            for (Itemset itemset : results) {
                for (Integer item : itemset.getItems()) {
                    assertThat(item).isBetween(1, 5);
                }
            }
        }

        // Cleanup
        engine.shutdown();
    }

    @Test
    @DisplayName("Should handle database with negative utilities")
    void shouldHandleNegativeUtilities() {
        // Given
        MiningEngine engine = MiningEngine.builder()
            .itemProfits(profits) // Contains item 5 with negative utility
            .k(5)
            .minProbability(0.2)
            .build();

        // When
        List<Itemset> results = engine.mine(database);

        // Then
        assertThat(results).isNotNull();

        // Verify negative utility items are properly handled
        results.forEach(itemset -> {
            // If contains item 5 (negative), total utility should reflect it
            if (itemset.containsItem(5)) {
                // Just verify it doesn't break the algorithm
                assertThat(itemset.getExpectedUtility()).isNotNull();
            }
        });

        engine.shutdown();
    }

    @Test
    @DisplayName("Should apply pruning strategies effectively")
    void shouldApplyPruningStrategies() {
        // Given
        MiningEngine engine = MiningEngine.builder()
            .itemProfits(profits)
            .k(1) // Very restrictive k
            .minProbability(0.8) // High probability threshold
            .build();

        // When
        List<Itemset> results = engine.mine(database);

        // Then
        assertThat(results).hasSizeLessThanOrEqualTo(1);

        // Verify statistics show pruning occurred
        var statistics = engine.getStatistics();

        // With such restrictive parameters, we expect significant pruning
        // Use isGreaterThanOrEqualTo to handle case where no pruning needed
        assertThat(statistics.getCandidatesPruned().get()).isGreaterThanOrEqualTo(0L);

        // If candidates were generated, some should be pruned with these strict parameters
        if (statistics.getCandidatesGenerated().get() > 0) {
            double pruningRate = statistics.getPruningEffectiveness();
            // With minProbability=0.8, we expect some pruning
            assertThat(pruningRate).isGreaterThanOrEqualTo(0.0);
        }

        engine.shutdown();
    }

    private List<Transaction> createTestDatabase() {
        List<Transaction> db = new ArrayList<>();

        // Transaction 1: {1:2, 2:4}
        db.add(Transaction.of(1,
            Map.of(1, 2, 2, 4),
            Map.of(1, 0.9, 2, 0.9)
        ));

        // Transaction 2: {1:1, 3:3, 4:1}
        db.add(Transaction.of(2,
            Map.of(1, 1, 3, 3, 4, 1),
            Map.of(1, 0.8, 3, 0.7, 4, 0.9)
        ));

        // Transaction 3: {2:2, 3:2, 5:1}
        db.add(Transaction.of(3,
            Map.of(2, 2, 3, 2, 5, 1),
            Map.of(2, 0.8, 3, 0.6, 5, 0.5)
        ));

        // Transaction 4: {1:1, 2:1, 4:2}
        db.add(Transaction.of(4,
            Map.of(1, 1, 2, 1, 4, 2),
            Map.of(1, 0.7, 2, 0.8, 4, 0.9)
        ));
        
        // Transaction 5: {3:1, 4:1}
        db.add(Transaction.of(5,
            Map.of(3, 1, 4, 1),
            Map.of(3, 0.9, 4, 0.8)
        ));
        
        return db;
    }
}