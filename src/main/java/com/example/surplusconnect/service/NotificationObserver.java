package com.example.surplusconnect.service;

import com.example.surplusconnect.model.Allocation;
import com.example.surplusconnect.model.Item;

/**
 * Observer interface for the Notification System.
 */
public interface NotificationObserver {
    void onMatchFound(Allocation allocation, Item item);
    void onDeliveryStatusUpdate(Allocation allocation, String status);
}
