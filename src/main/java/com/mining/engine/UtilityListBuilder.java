// File: src/main/java/com/mining/engine/UtilityListBuilder.java
package com.mining.engine;

import com.mining.config.AlgorithmConstants;
import com.mining.core.model.Transaction;
import com.mining.core.model.UtilityList;
import com.mining.core.model.UtilityList.Element;
import com.mining.engine.statistics.MiningStatistics;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Builder for creating utility lists with suffix sum optimization.
 * This class implements the O(T) optimization for utility list construction.
 */
@Slf4j
public class UtilityListBuilder {
    
    private final Map<Integer, Double> itemProfits;
    private final double minProbability;
    private final MiningStatistics statistics;
    
    public UtilityListBuilder(Map<Integer, Double> itemProfits, double minProbability,
                             MiningStatistics statistics) {
        this.itemProfits = itemProfits;
        this.minProbability = minProbability;
        this.statistics = statistics;
    }
    
    /**
     * Calculate RTWU (Remaining Transaction Weighted Utility) for all items.
     * Single-pass optimization: O(|DB| * T_avg) instead of O(2 * |DB| * T_avg)
     * 
     * @param database Transaction database
     * @return Map of item to RTWU value
     */
    public Map<Integer, Double> calculateRTWU(List<Transaction> database) {
        log.debug("Calculating RTWU for {} transactions", database.size());
        
        Map<Integer, Double> itemRTWU = new HashMap<>();
        
        for (Transaction transaction : database) {
            // Calculate RTU (positive utilities only)
            double rtu = calculateTransactionUtility(transaction, true);
            
            // Update RTWU for all items with positive probabilities
            for (Map.Entry<Integer, Integer> entry : transaction.getItems().entrySet()) {
                Integer item = entry.getKey();
                Double probability = transaction.getProbabilities().get(item);
                
                if (probability != null && probability > 0) {
                    itemRTWU.merge(item, rtu, Double::sum);
                }
            }
        }
        
        log.info("Calculated RTWU for {} unique items", itemRTWU.size());
        return itemRTWU;
    }
    
    /**
     * Build utility lists for single items with suffix sum optimization.
     * Core optimization: Reduces complexity from O(T²) to O(T) per transaction
     * 
     * @param database Transaction database
     * @param itemToRank Item ranking map
     * @param itemRTWU Item RTWU values
     * @return Map of item to utility list
     */
    public Map<Integer, UtilityList> buildUtilityLists(List<Transaction> database,
                                                       Map<Integer, Integer> itemToRank,
                                                       Map<Integer, Double> itemRTWU) {
        log.debug("Building utility lists with suffix sum optimization");
        
        // Step 1: Build elements for each item
        Map<Integer, List<Element>> itemElements = new HashMap<>();
        
        for (Transaction transaction : database) {
            processTransactionWithSuffixSum(transaction, itemToRank, itemElements);
        }
        
        // Step 2: Create utility lists with pre-computed values
        Map<Integer, UtilityList> utilityLists = new HashMap<>();
        
        for (Map.Entry<Integer, List<Element>> entry : itemElements.entrySet()) {
            Integer item = entry.getKey();
            List<Element> elements = entry.getValue();
            
            if (!elements.isEmpty()) {
                UtilityList ul = UtilityList.createSingleItem(
                    item, elements, itemRTWU.getOrDefault(item, 0.0)
                );
                
                // Only keep if meets minimum probability threshold
                if (ul.getExistentialProbability() >= minProbability - AlgorithmConstants.EPSILON) {
                    utilityLists.put(item, ul);
                    statistics.incrementUtilityListsCreated();
                }
            }
        }
        
        log.info("Created {} utility lists after filtering", utilityLists.size());
        return utilityLists;
    }
    
    /**
     * Process a transaction with suffix sum optimization.
     * Key optimization: Pre-computes remaining utilities in O(T) time
     */
    private void processTransactionWithSuffixSum(Transaction transaction,
                                                Map<Integer, Integer> itemToRank,
                                                Map<Integer, List<Element>> itemElements) {
        // Extract and sort items by rank
        List<ItemData> sortedItems = extractSortedItems(transaction, itemToRank);
        
        if (sortedItems.isEmpty()) {
            return;
        }
        
        // OPTIMIZATION: Compute suffix sums in O(T) time
        double[] suffixSums = computeSuffixSums(sortedItems);
        
        // Create elements with pre-computed remaining utilities
        for (int i = 0; i < sortedItems.size(); i++) {
            ItemData itemData = sortedItems.get(i);
            
            if (itemData.logProbability > AlgorithmConstants.LOG_EPSILON) {
                // O(1) access to remaining utility!
                double remainingUtility = suffixSums[i];
                
                Element element = new Element(
                    transaction.getTid(),
                    itemData.getUtility(),
                    remainingUtility,
                    itemData.logProbability
                );
                
                itemElements.computeIfAbsent(itemData.item, k -> new ArrayList<>())
                    .add(element);
            }
        }
    }
    
    /**
     * Extract and sort items from transaction by rank.
     */
    private List<ItemData> extractSortedItems(Transaction transaction,
                                             Map<Integer, Integer> itemToRank) {
        List<ItemData> validItems = new ArrayList<>();
        
        for (Map.Entry<Integer, Integer> entry : transaction.getItems().entrySet()) {
            Integer item = entry.getKey();
            Integer quantity = entry.getValue();
            
            // Only process items that exist in ranking
            if (!itemToRank.containsKey(item)) {
                continue;
            }
            
            Double profit = itemProfits.get(item);
            Double probability = transaction.getProbabilities().get(item);
            
            if (profit != null && probability != null && probability > 0) {
                double logProb = Math.log(probability);
                validItems.add(new ItemData(item, quantity, profit, logProb));
            }
        }
        
        // Sort by rank for consistent ordering
        validItems.sort((a, b) -> {
            Integer rankA = itemToRank.get(a.item);
            Integer rankB = itemToRank.get(b.item);
            return rankA.compareTo(rankB);
        });
        
        return validItems;
    }
    
    /**
     * Compute suffix sums for remaining utility calculation.
     * Core optimization: O(T) preprocessing instead of O(T²) nested loops
     * 
     * Algorithm:
     * - suffixSum[i] = sum of positive utilities from position i+1 to end
     * - Each item's remaining utility = suffixSum[i]
     */
    private double[] computeSuffixSums(List<ItemData> sortedItems) {
        int n = sortedItems.size();
        double[] suffixSums = new double[n];
        
        // Last item has no remaining utility
        suffixSums[n - 1] = 0.0;
        
        // Fill suffix sums from right to left
        for (int i = n - 2; i >= 0; i--) {
            ItemData nextItem = sortedItems.get(i + 1);
            
            // Only include positive utilities in remaining
            double nextUtility = nextItem.profit > 0 ? nextItem.getUtility() : 0.0;
            suffixSums[i] = suffixSums[i + 1] + nextUtility;
        }
        
        return suffixSums;
    }
    
    /**
     * Calculate transaction utility.
     * 
     * @param transaction The transaction
     * @param positiveOnly Whether to include only positive utilities
     * @return Transaction utility
     */
    private double calculateTransactionUtility(Transaction transaction, boolean positiveOnly) {
        double utility = 0.0;
        
        for (Map.Entry<Integer, Integer> entry : transaction.getItems().entrySet()) {
            Integer item = entry.getKey();
            Integer quantity = entry.getValue();
            Double profit = itemProfits.get(item);
            
            if (profit != null) {
                if (!positiveOnly || profit > 0) {
                    utility += profit * quantity;
                }
            }
        }
        
        return utility;
    }
    
    /**
     * Helper class for item data during processing.
     */
    @Data
    @AllArgsConstructor
    private static class ItemData {
        private final int item;
        private final int quantity;
        private final double profit;
        private final double logProbability;
        
        public double getUtility() {
            return profit * quantity;
        }
    }
}