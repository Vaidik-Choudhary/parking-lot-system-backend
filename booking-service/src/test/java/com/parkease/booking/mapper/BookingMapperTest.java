package com.parkease.booking.mapper;

import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class BookingMapperTest {
    private BookingMapper mapper;
    @BeforeEach void setUp() { mapper = new BookingMapper(); }

    @Test void toDTO_mapsAllFields() {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end   = start.plusHours(2);
        Booking b = Booking.builder().bookingId(1L).driverEmail("d@t.com")
                .lotId(10L).spotId(101L).vehiclePlate("MP04AB1234")
                .bookingType(BookingType.PRE_BOOKING).status(BookingStatus.RESERVED)
                .startTime(start).endTime(end).pricePerHour(50.0).totalAmount(0.0).build();
        BookingResponseDTO dto = mapper.toDTO(b);
        assertEquals(1L, dto.getBookingId());
        assertEquals("MP04AB1234", dto.getVehiclePlate());
        assertEquals(100.0, dto.getEstimatedAmount());
    }

    @Test void toDTO_nullTimes_estimatedIsZero() {
        Booking b = Booking.builder().bookingId(2L).driverEmail("d@t.com")
                .lotId(1L).spotId(1L).vehiclePlate("AA00BB0001")
                .bookingType(BookingType.DRIVE_IN).status(BookingStatus.ACTIVE)
                .startTime(null).endTime(null).pricePerHour(60.0).totalAmount(0.0).build();
        assertEquals(0.0, mapper.toDTO(b).getEstimatedAmount());
    }

    @Test void toDTO_lessThanOneHour_chargesMinimumOneHour() {
        LocalDateTime start = LocalDateTime.now();
        Booking b = Booking.builder().bookingId(3L).driverEmail("d@t.com")
                .lotId(1L).spotId(1L).vehiclePlate("AA00BB0002")
                .bookingType(BookingType.PRE_BOOKING).status(BookingStatus.RESERVED)
                .startTime(start).endTime(start.plusMinutes(30)).pricePerHour(60.0).totalAmount(0.0).build();
        assertEquals(60.0, mapper.toDTO(b).getEstimatedAmount());
    }
}
