package com.example.surplusconnect.model;

/**
 * Multi-Level Queue categorization (Feature 10).
 * 
 * Used by the Dynamic Re-Prioritization Engine (Feature 3)
 * to segregate requests into distinct processing queues.
 * 
 * CRITICAL is activated during Emergency Mode (Feature 18).
 */
public enum UrgencyLevel {
    LOW(1),
    NORMAL(2),
    HIGH(3),
    CRITICAL(4);  // Emergency Mode override

    private final int weight;

    UrgencyLevel(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
