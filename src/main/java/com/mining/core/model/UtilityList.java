// File: src/main/java/com/mining/core/model/UtilityList.java
package com.mining.core.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mining.config.AlgorithmConstants;
import lombok.Getter;
import lombok.ToString;

import java.util.*;

/**
 * Enhanced utility list data structure for efficient itemset mining.
 * Includes pre-computed values for O(1) access to key metrics.
 */
@Getter
@ToString(exclude = "elements")
public class UtilityList {
    
    /**
     * The itemset represented by this utility list.
     */
    private final Set<Integer> itemset;
    
    /**
     * List of utility elements for each transaction.
     */
    private final List<Element> elements;
    
    /**
     * Remaining Transaction Weighted Utility (RTWU).
     */
    private final double rtwu;
    
    /**
     * Pre-computed sum of expected utilities (O(1) access).
     */
    private final double sumEU;
    
    /**
     * Pre-computed sum of remaining utilities (O(1) access).
     */
    private final double sumRemaining;
    
    /**
     * Pre-computed existential probability (O(1) access).
     */
    private final double existentialProbability;
    
    /**
     * Utility list element representing transaction information.
     */
    @Getter
    @ToString
    public static class Element {
        private final int tid;
        private final double utility;
        private final double remaining;
        private final double logProbability;
        
        public Element(int tid, double utility, double remaining, double logProbability) {
            this.tid = tid;
            this.utility = utility;
            this.remaining = remaining;
            this.logProbability = logProbability;
        }
        
        /**
         * Gets the actual probability (not log).
         */
        public double getProbability() {
            return Math.exp(logProbability);
        }
    }
    
    /**
     * Creates a new utility list with pre-computed values.
     */
    public UtilityList(Set<Integer> itemset, List<Element> elements, double rtwu) {
        Preconditions.checkNotNull(itemset, "Itemset cannot be null");
        Preconditions.checkNotNull(elements, "Elements cannot be null");
        Preconditions.checkArgument(rtwu >= 0, "RTWU must be non-negative");
        
        this.itemset = Collections.unmodifiableSet(new HashSet<>(itemset));
        this.elements = ImmutableList.copyOf(elements);
        this.rtwu = rtwu;
        
        // Pre-compute values for O(1) access
        double tempSumEU = 0.0;
        double tempSumRemaining = 0.0;
        double tempLogProbSum = 0.0;
        
        for (Element element : elements) {
            tempSumEU += element.utility * Math.exp(element.logProbability);
            tempSumRemaining += element.remaining;
            tempLogProbSum += element.logProbability;
        }
        
        this.sumEU = tempSumEU;
        this.sumRemaining = tempSumRemaining;
        this.existentialProbability = 1.0 - Math.exp(tempLogProbSum);
    }
    
    /**
     * Creates a single-item utility list.
     */
    public static UtilityList createSingleItem(int item, List<Element> elements, double rtwu) {
        return new UtilityList(Collections.singleton(item), elements, rtwu);
    }
    
    /**
     * Gets the expected utility (pre-computed, O(1)).
     */
    public double getSumEU() {
        return sumEU;
    }
    
    /**
     * Gets the sum of remaining utilities (pre-computed, O(1)).
     */
    public double getSumRemaining() {
        return sumRemaining;
    }
    
    /**
     * Gets the existential probability (pre-computed, O(1)).
     */
    public double getExistentialProbability() {
        return existentialProbability;
    }
    
    /**
     * Gets the upper bound utility (EU + remaining).
     */
    public double getUpperBound() {
        return sumEU + sumRemaining;
    }
    
    /**
     * Checks if this utility list is promising based on threshold.
     */
    public boolean isPromising(double threshold) {
        return getUpperBound() >= threshold - AlgorithmConstants.EPSILON;
    }
    
    /**
     * Gets the number of elements (transactions).
     */
    public int size() {
        return elements.size();
    }
    
    /**
     * Checks if the utility list is empty.
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }
    
    /**
     * Gets element at specific index.
     */
    public Element getElement(int index) {
        return elements.get(index);
    }
    
    /**
     * Creates an itemset from this utility list.
     */
    public Itemset toItemset() {
        return new Itemset(itemset, sumEU, existentialProbability, elements.size());
    }
}