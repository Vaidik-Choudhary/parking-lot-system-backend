package com.parkease.booking.entity;

/**
 * Represents the two booking modes supported by ParkEase:
 *
 * PRE_BOOKING  â€“ User reserves a spot in advance by specifying a future
 *                start/end time window. The booking starts in RESERVED status
 *                and transitions to ACTIVE only after check-in (within the
 *                grace period).
 *
 * DRIVE_IN     â€“ User arrives at the lot and books on the spot. The booking
 *                starts immediately (startTime = now) and is auto-activated
 *                (status set to ACTIVE) upon creation â€” no separate check-in
 *                grace period applies.
 */
public enum BookingType {
    PRE_BOOKING,
    DRIVE_IN
}
