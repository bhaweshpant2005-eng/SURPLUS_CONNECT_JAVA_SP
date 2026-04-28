package com.example.surplusconnect.service;

import com.example.surplusconnect.model.Allocation;
import com.example.surplusconnect.model.Item;
import com.example.surplusconnect.model.Notification;
import com.example.surplusconnect.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Service
public class NotificationService implements NotificationObserver {

    private final NotificationRepository notificationRepository;
    
    // Feature 8: Event-based notifications using Queue
    private final Queue<Notification> notificationQueue = new LinkedList<>();

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void onMatchFound(Allocation allocation, Item item) {
        String message = "Match found for item: " + item.getItemType() + " (Allocated: " + allocation.getAllocatedQuantity() + ")";

        // Notify NGO
        Notification ngoNotif = new Notification(allocation.getNgoId(), "Match found! " + message, "MATCH_FOUND");
        queueNotification(ngoNotif);

        // Notify Donor (use donorId from item, not item.getId())
        if (item.getDonorId() != null) {
            Notification donorNotif = new Notification(item.getDonorId(), "Your donation was matched! " + message, "MATCH_FOUND");
            queueNotification(donorNotif);
        }
    }

    @Override
    public void onDeliveryStatusUpdate(Allocation allocation, String status) {
        String message = "Delivery status updated to: " + status;
        Notification ngoNotif = new Notification(allocation.getNgoId(), message, "DELIVERY_UPDATE");
        queueNotification(ngoNotif);
    }

    public void queueNotification(Notification notification) {
        notificationQueue.offer(notification);
        processQueue(); // can be asynchronous or scheduled in a real system
    }

    private synchronized void processQueue() {
        while (!notificationQueue.isEmpty()) {
            Notification notif = notificationQueue.poll();
            notificationRepository.save(notif);
        }
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByTimestampDesc(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notif -> {
            notif.setRead(true);
            notificationRepository.save(notif);
        });
    }
}
