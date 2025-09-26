// File: src/main/java/com/mining/core/model/Transaction.java
package com.mining.core.model;

import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a transaction in the database.
 * Each transaction contains items with their quantities and probabilities.
 */
@Data
@Builder
@EqualsAndHashCode(of = "tid")
public class Transaction {

    /**
     * Unique transaction identifier.
     */
    private final int tid;

    /**
     * Map of item IDs to their quantities in this transaction.
     */
    private final Map<Integer, Integer> items;

    /**
     * Map of item IDs to their existence probabilities.
     */
    private final Map<Integer, Double> probabilities;

    /**
     * Creates a new transaction with validation.
     */
    public static Transaction of(int tid, Map<Integer, Integer> items, Map<Integer, Double> probabilities) {
        Preconditions.checkArgument(tid > 0, "Transaction ID must be positive");
        Preconditions.checkNotNull(items, "Items map cannot be null");
        Preconditions.checkNotNull(probabilities, "Probabilities map cannot be null");
        Preconditions.checkArgument(!items.isEmpty(), "Transaction must contain at least one item");

        // Validate quantities
        items.forEach((item, quantity) -> {
            Preconditions.checkArgument(quantity > 0,
                "Item %s has invalid quantity %s", item, quantity);
        });

        // Validate probabilities
        probabilities.forEach((item, probability) -> {
            Preconditions.checkArgument(probability >= 0.0 && probability <= 1.0,
                "Item %s has invalid probability %s", item, probability);
        });

        return Transaction.builder()
            .tid(tid)
            .items(Collections.unmodifiableMap(new HashMap<>(items)))
            .probabilities(Collections.unmodifiableMap(new HashMap<>(probabilities)))
            .build();
    }

    /**
     * Gets the quantity of a specific item.
     */
    public int getQuantity(int item) {
        return items.getOrDefault(item, 0);
    }

    /**
     * Gets the probability of a specific item.
     */
    public double getProbability(int item) {
        return probabilities.getOrDefault(item, 0.0);
    }

    /**
     * Checks if the transaction contains an item.
     */
    public boolean containsItem(int item) {
        return items.containsKey(item) && probabilities.containsKey(item);
    }

    /**
     * Gets the set of all item IDs in this transaction.
     */
    public Set<Integer> getItemIds() {
        return items.keySet();
    }

    /**
     * Calculates the transaction utility given profit values.
     */
    public double calculateUtility(Map<Integer, Double> profits) {
        double utility = 0.0;
        for (Map.Entry<Integer, Integer> entry : items.entrySet()) {
            Double profit = profits.get(entry.getKey());
            if (profit != null) {
                utility += profit * entry.getValue();
            }
        }
        return utility;
    }

    /**
     * Gets the size of the transaction (number of distinct items).
     */
    public int size() {
        return items.size();
    }
}