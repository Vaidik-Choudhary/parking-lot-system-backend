package com.parkease.booking.service;

import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.exception.BookingException;
import com.parkease.booking.mapper.BookingMapper;
import com.parkease.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock
    private BookingRepository repo;

    @Mock
    private BookingMapper mapper;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BookingServiceImpl service;

    private Booking booking;
    private BookingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "spotServiceUrl", "http://parkingspot-service");
        ReflectionTestUtils.setField(service, "lotServiceUrl", "http://parkinglot-service");
        ReflectionTestUtils.setField(service, "graceMinutes", 15);

        booking = Booking.builder()
                .bookingId(1L)
                .driverEmail("vaidik@test.com")
                .lotId(10L)
                .spotId(101L)
                .vehiclePlate("MP04AB1234")
                .bookingType(BookingType.PRE_BOOKING)
                .status(BookingStatus.RESERVED)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusHours(2))
                .pricePerHour(50.0)
                .build();

        responseDTO = new BookingResponseDTO();
    }

    @Test
    void shouldCreateBookingSuccessfully() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L);
        req.setSpotId(101L);
        req.setVehiclePlate("mp04ab1234");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(2));

        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenReturn(Map.of("pricePerHour", 50.0));
        when(repo.save(any(Booking.class))).thenReturn(booking);
        when(mapper.toDTO(any(Booking.class))).thenReturn(responseDTO);

        BookingResponseDTO result = service.createBooking(req, "vaidik@test.com");

        assertNotNull(result);
        verify(repo).save(any(Booking.class));
        verify(restTemplate).put(contains("/reserve"), isNull());
        verify(restTemplate).put(contains("/decrement"), isNull());
    }

    @Test
    void shouldThrowWhenSpotAlreadyBooked() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setStartTime(LocalDateTime.now());
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(true);

        assertThrows(BookingException.class,
                () -> service.createBooking(req, "vaidik@test.com"));
    }

    @Test
    void shouldCheckInSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.checkIn(1L, "vaidik@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        verify(restTemplate).put(contains("/occupy"), isNull());
    }

    @Test
    void shouldCheckOutSuccessfully() {
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(LocalDateTime.now().minusHours(2));

        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.checkOut(1L, "vaidik@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertTrue(booking.getTotalAmount() > 0);
        verify(restTemplate).put(contains("/release"), isNull());
        verify(restTemplate).put(contains("/increment"), isNull());
    }

    @Test
    void shouldCancelBookingSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.cancelBooking(1L, "vaidik@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(restTemplate).put(contains("/release"), isNull());
        verify(restTemplate).put(contains("/increment"), isNull());
    }

    @Test
    void shouldExtendBookingSuccessfully() {
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(booking.getEndTime().plusHours(2));

        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        BookingResponseDTO result = service.extendBooking(1L, req, "vaidik@test.com");

        assertNotNull(result);
        assertEquals(req.getNewEndTime(), booking.getEndTime());
    }

    @Test
    void shouldCalculateFareSuccessfully() {
        booking.setCheckInTime(LocalDateTime.now().minusHours(2));
        booking.setCheckOutTime(LocalDateTime.now());

        when(repo.findById(1L)).thenReturn(Optional.of(booking));

        double fare = service.calculateFare(1L);

        assertEquals(100.0, fare);
    }

    @Test
    void shouldThrowWhenDriverMismatch() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(RuntimeException.class,
                () -> service.checkIn(1L, "other@test.com"));
    }
}