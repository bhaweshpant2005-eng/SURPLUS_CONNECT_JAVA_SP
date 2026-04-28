package com.example.surplusconnect.model;

/**
 * Finite State Machine (FSM) for Resource Lifecycle Management (Feature 16).
 * 
 * Valid transitions:
 *   REGISTERED -> MATCHED -> IN_TRANSIT -> DELIVERED -> CONSUMED
 *   Any state   -> EXPIRED  (time-based transition)
 *   MATCHED     -> REGISTERED (rollback on allocation failure - Feature 13)
 */
public enum LifecycleState {
    REGISTERED,  // legacy alias for PENDING (kept for DB compatibility)
    PENDING,
    MATCHED,
    CONFIRMED,
    DISPATCHED,
    DELIVERED,
    REJECTED,
    EXPIRED
}
