package com.parkease.notification.entity;

/**
 * Types of notifications that ParkEase can send.
 *
 * MANAGER_ALERT â€“ Notifications directed at Lot Managers:
 *                 new bookings, check-in/out events, upcoming arrivals.
 */
public enum NotificationType {
    BOOKING_CONFIRMED,
    CHECKIN,
    CHECKOUT,
    PAYMENT,
    EXPIRY_REMINDER,
    CANCELLATION,
    NEW_BOOKING,
    LOT_APPROVED,
    BROADCAST,
    MANAGER_ALERT
}
