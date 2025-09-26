// File: src/main/java/com/mining/core/model/Itemset.java
package com.mining.core.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.Set;

/**
 * Represents an itemset with its utility and probability metrics.
 * Immutable class for thread-safety in parallel processing.
 */
@Getter
@EqualsAndHashCode(of = "items")
@ToString
public class Itemset implements Comparable<Itemset> {
    
    /**
     * Set of item IDs in this itemset.
     */
    private final Set<Integer> items;
    
    /**
     * Expected utility of the itemset.
     */
    private final double expectedUtility;
    
    /**
     * Existential probability of the itemset.
     */
    private final double probability;
    
    /**
     * Timestamp when this itemset was created (for ordering).
     */
    private final long timestamp;
    
    /**
     * Support count (number of transactions containing this itemset).
     */
    private final int support;
    
    /**
     * Creates a new itemset.
     */
    public Itemset(Set<Integer> items, double expectedUtility, double probability) {
        this(items, expectedUtility, probability, 0);
    }
    
    /**
     * Creates a new itemset with support information.
     */
    public Itemset(Set<Integer> items, double expectedUtility, double probability, int support) {
        Preconditions.checkNotNull(items, "Items cannot be null");
        Preconditions.checkArgument(!items.isEmpty(), "Itemset cannot be empty");
        Preconditions.checkArgument(expectedUtility >= 0, 
            "Expected utility must be non-negative");
        Preconditions.checkArgument(probability >= 0 && probability <= 1,
            "Probability must be between 0 and 1");
        Preconditions.checkArgument(support >= 0, "Support must be non-negative");
        
        this.items = ImmutableSet.copyOf(items);
        this.expectedUtility = expectedUtility;
        this.probability = probability;
        this.support = support;
        this.timestamp = System.nanoTime();
    }
    
    /**
     * Creates an itemset from a collection of items.
     */
    public static Itemset of(Collection<Integer> items, double utility, double probability) {
        return new Itemset(ImmutableSet.copyOf(items), utility, probability);
    }
    
    /**
     * Gets the size of the itemset.
     */
    public int size() {
        return items.size();
    }
    
    /**
     * Checks if this itemset contains another itemset.
     */
    public boolean contains(Itemset other) {
        return items.containsAll(other.items);
    }
    
    /**
     * Checks if this itemset contains a specific item.
     */
    public boolean containsItem(int item) {
        return items.contains(item);
    }
    
    /**
     * Creates a new itemset by joining with another itemset.
     */
    public Itemset join(Itemset other, double newUtility, double newProbability) {
        ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
        builder.addAll(this.items);
        builder.addAll(other.items);
        return new Itemset(builder.build(), newUtility, newProbability);
    }
    
    @Override
    public int compareTo(Itemset other) {
        // Primary: Compare by utility (descending)
        int utilityComp = Double.compare(other.expectedUtility, this.expectedUtility);
        if (utilityComp != 0) return utilityComp;
        
        // Secondary: Compare by probability (descending)
        int probComp = Double.compare(other.probability, this.probability);
        if (probComp != 0) return probComp;
        
        // Tertiary: Compare by size (ascending - prefer smaller itemsets)
        int sizeComp = Integer.compare(this.size(), other.size());
        if (sizeComp != 0) return sizeComp;
        
        // Finally: Compare by timestamp (earlier first)
        return Long.compare(this.timestamp, other.timestamp);
    }
    
    /**
     * Returns a formatted string representation.
     */
    public String toFormattedString() {
        return String.format("{%s} EU=%.4f P=%.4f S=%d",
            items, expectedUtility, probability, support);
    }
}