package com.parkease.booking.dto.response;

import lombok.*;
import java.util.List;

/**
 * Aggregated dashboard payload returned to a Lot Manager.
 *
 * activeBookings   â€“ Bookings currently in progress (status = ACTIVE,
 *                    i.e. driver has checked in and is still parked).
 * upcomingBookings â€“ Future reservations (status = RESERVED, startTime > now)
 *                    that have not yet started.
 *
 * This mirrors the BookMyShow-style time-aware seat state view â€” the manager
 * can see who is currently parked and who is arriving next.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManagerDashboardDTO {

    /** Lot this dashboard belongs to */
    private Long lotId;

    /** Count of currently active (checked-in) bookings */
    private int totalActive;

    /** Count of upcoming (reserved but not yet started) bookings */
    private int totalUpcoming;

    /** Full list of active booking details */
    private List<BookingResponseDTO> activeBookings;

    /** Full list of upcoming booking details, ordered by startTime ASC */
    private List<BookingResponseDTO> upcomingBookings;
}
