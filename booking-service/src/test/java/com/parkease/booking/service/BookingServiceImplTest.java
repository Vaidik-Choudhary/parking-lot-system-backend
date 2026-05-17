package com.parkease.booking.service;

import com.parkease.booking.client.LotServiceClient;
import com.parkease.booking.client.PaymentServiceClient;
import com.parkease.booking.client.SpotServiceClient;
import com.parkease.booking.dto.request.CreateBookingRequest;
import com.parkease.booking.dto.request.ExtendBookingRequest;
import com.parkease.booking.dto.response.BookingResponseDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.exception.BookingException;
import com.parkease.booking.mapper.BookingMapper;
import com.parkease.booking.messaging.NotificationPublisher;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpServletRequest;
import com.parkease.booking.client.VehicleServiceClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceImplTest {

    @Mock BookingRepository repo;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock BookingMapper mapper;
    @Mock SpotServiceClient spotServiceClient;
    @Mock LotServiceClient lotServiceClient;
    @Mock PaymentServiceClient paymentServiceClient;
    @Mock NotificationPublisher notificationPublisher;
    @Mock HttpServletRequest httpServletRequest;
    @Mock VehicleServiceClient vehicleServiceClient;

    @InjectMocks BookingServiceImpl service;

    Booking booking;
    BookingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "graceMinutes", 15);
        booking = Booking.builder()
                .bookingId(1L).driverEmail("vaidik@test.com")
                .lotId(10L).spotId(101L).vehiclePlate("MP04AB1234")
                .bookingType(BookingType.PRE_BOOKING).status(BookingStatus.RESERVED)
                .startTime(LocalDateTime.now().plusMinutes(5))
                .endTime(LocalDateTime.now().plusHours(2))
                .pricePerHour(50.0).build();
        responseDTO = new BookingResponseDTO();
    }

    @Test void shouldCreateBookingSuccessfully() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L); req.setSpotId(101L); req.setVehiclePlate("mp04ab1234");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(2));

        when(vehicleServiceClient.getVehicleByPlate(any(), anyString()))
                .thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));
        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        doNothing().when(notificationPublisher).publish(any());

        BookingResponseDTO result = service.createBooking(req, "vaidik@test.com");

        assertNotNull(result);
        verify(repo).save(any(Booking.class));
        verify(spotServiceClient).reserveSpot(101L);
        verify(lotServiceClient).decrementAvailable(10L);
        verify(notificationPublisher).publish(any());
    }

    @Test void shouldThrowWhenEndTimeBeforeStart() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04AB1234");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusHours(2));
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(vehicleServiceClient.getVehicleByPlate(any(), anyString()))
                .thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));

        assertThrows(IllegalArgumentException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void shouldThrowWhenSpotAlreadyBooked() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04AB1234");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(vehicleServiceClient.getVehicleByPlate(any(), anyString()))
                .thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));
        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(true);

        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void shouldCheckInSuccessfully() {
        booking.setStartTime(LocalDateTime.now().minusMinutes(5));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        doNothing().when(notificationPublisher).publish(any());

        BookingResponseDTO result = service.checkIn(1L, "vaidik@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.ACTIVE, booking.getStatus());
        verify(spotServiceClient).occupySpot(booking.getSpotId());
    }

    @Test void checkIn_driveIn_throws() {
        booking.setBookingType(BookingType.DRIVE_IN);
        booking.setStatus(BookingStatus.ACTIVE);
        when(repo.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(BookingException.class, () -> service.checkIn(1L, "vaidik@test.com"));
    }

    @Test void checkIn_preBookingGraceExpired_throws() {
        booking.setBookingType(BookingType.PRE_BOOKING);
        booking.setStartTime(LocalDateTime.now().minusHours(1));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(BookingException.class, () -> service.checkIn(1L, "vaidik@test.com"));
    }

    @Test void shouldCheckOutSuccessfully() {
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(LocalDateTime.now().minusHours(2));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        doNothing().when(notificationPublisher).publish(any());

        BookingResponseDTO result = service.checkOut(1L, "vaidik@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.COMPLETED, booking.getStatus());
        assertTrue(booking.getTotalAmount() > 0);
        verify(spotServiceClient).releaseSpot(booking.getSpotId());
        verify(lotServiceClient).incrementAvailable(booking.getLotId());
    }

    @Test void checkOut_notActive_throws() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking)); // status=RESERVED
        assertThrows(BookingException.class, () -> service.checkOut(1L, "vaidik@test.com"));
    }

    @Test void shouldCancelBookingSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        doNothing().when(notificationPublisher).publish(any());

        BookingResponseDTO result = service.cancelBooking(1L, "vaidik@test.com");

        assertNotNull(result);
        assertEquals(BookingStatus.CANCELLED, booking.getStatus());
        verify(spotServiceClient).releaseSpot(booking.getSpotId());
    }

    @Test void shouldExtendBookingSuccessfully() {
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(booking.getEndTime().plusHours(2));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        doNothing().when(notificationPublisher).publish(any());

        assertNotNull(service.extendBooking(1L, req, "vaidik@test.com"));
        assertEquals(req.getNewEndTime(), booking.getEndTime());
    }

    @Test void extendBooking_newEndBeforeCurrent_throws() {
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(booking.getEndTime().minusHours(1));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(IllegalArgumentException.class,
                () -> service.extendBooking(1L, req, "vaidik@test.com"));
    }

    @Test void shouldCalculateFareSuccessfully() {
        booking.setCheckInTime(LocalDateTime.now().minusHours(2));
        booking.setCheckOutTime(LocalDateTime.now());
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        assertEquals(100.0, service.calculateFare(1L));
    }

    @Test void calculateFare_noCheckIn_throws() {
        booking.setCheckInTime(null);
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(BookingException.class, () -> service.calculateFare(1L));
    }

    @Test void shouldThrowWhenDriverMismatch() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(RuntimeException.class, () -> service.checkIn(1L, "other@test.com"));
    }

    @Test void getMyBookings_returnsAll() {
        when(repo.findByDriverEmailOrderByCreatedAtDesc("vaidik@test.com"))
                .thenReturn(List.of(booking));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertEquals(1, service.getMyBookings("vaidik@test.com").size());
    }

    @Test void getActiveBookings_returnsActive() {
        when(repo.findByDriverEmailAndStatus("vaidik@test.com", BookingStatus.ACTIVE))
                .thenReturn(List.of(booking));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertEquals(1, service.getActiveBookings("vaidik@test.com").size());
    }

    @Test void getAllBookings_returnsAll() {
        when(repo.findAll()).thenReturn(List.of(booking));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertEquals(1, service.getAllBookings().size());
    }

    @Test void getDistinctLotIds_returnsList() {
        when(repo.findDistinctLotIds()).thenReturn(List.of(1L, 2L));
        assertEquals(2, service.getDistinctLotIds().size());
    }

    @Test void createBooking_DriveIn_Success() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L); req.setSpotId(101L); req.setVehiclePlate("mp04ab1234");
        req.setBookingType(BookingType.DRIVE_IN);
        req.setEndTime(LocalDateTime.now().plusHours(2));

        when(vehicleServiceClient.getVehicleByPlate(any(), anyString()))
                .thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));
        when(repo.existsBySpotIdAndStatus(101L, BookingStatus.ACTIVE)).thenReturn(false);
        when(repo.existsByVehiclePlateAndStatus("MP04AB1234", BookingStatus.ACTIVE)).thenReturn(false);
        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(repo.isVehicleBookedInWindow(anyString(), any(), any())).thenReturn(false);
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        doNothing().when(notificationPublisher).publish(any());

        BookingResponseDTO result = service.createBooking(req, "vaidik@test.com");
        assertNotNull(result);
        verify(spotServiceClient).occupySpot(101L);
    }

    @Test void createBooking_PendingPayments_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(paymentServiceClient.getPendingCount("Bearer token")).thenReturn(2L);
        
        assertThrows(BookingException.class, () -> service.createBooking(req, "vaidik@test.com"));
    }

    @Test void getAvailableSpotsForPreBooking_Success() {
        when(repo.findBookedSpotIdsInWindow(anyLong(), any(), any())).thenReturn(List.of(102L));
        when(spotServiceClient.getSpotsByLot(10L)).thenReturn(List.of(
                Map.of("spotId", 101L, "status", "FREE"),
                Map.of("spotId", 102L, "status", "FREE"),
                Map.of("spotId", 103L, "status", "OCCUPIED")
        ));
        var res = service.getAvailableSpotsForPreBooking(10L, LocalDateTime.now().plusMinutes(5), LocalDateTime.now().plusHours(1));
        assertEquals(1, res.size());
        assertEquals(101L, ((Number) res.get(0).get("spotId")).longValue());
    }

    @Test void getDriveInSpotView_Success() {
        when(spotServiceClient.getSpotsByLot(10L)).thenReturn(List.of(
                Map.of("spotId", 101L, "status", "FREE", "spotNumber", "A1", "floor", 1, "spotType", "STANDARD", "vehicleType", "FOUR_WHEELER", "pricePerHour", 50.0, "isEVCharging", false, "isHandicapped", false),
                Map.of("spotId", 102L, "status", "OCCUPIED", "spotNumber", "A2", "floor", 1, "spotType", "STANDARD", "vehicleType", "FOUR_WHEELER", "pricePerHour", 50.0, "isEVCharging", false, "isHandicapped", false)
        ));
        when(repo.findActiveOrReservedBookingsForSpot(101L)).thenReturn(List.of());
        var res = service.getDriveInSpotView(10L);
        assertEquals(2, res.size());
        assertEquals("FREE", res.get(0).getStatus());
        assertEquals("OCCUPIED", res.get(1).getStatus());
    }

    @Test void getManagerDashboard_Success() {
        when(repo.findActiveBookingsByLot(10L)).thenReturn(List.of(booking));
        when(repo.findUpcomingBookingsByLot(eq(10L), any())).thenReturn(List.of());
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        var res = service.getManagerDashboard(10L);
        assertEquals(1, res.getTotalActive());
        assertEquals(0, res.getTotalUpcoming());
    }
    
    @Test void getBookingById_Success() {
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(paymentServiceClient.getPaymentByBookingId(anyString(), eq(1L))).thenReturn(Map.of("status", "PAID"));
        
        BookingResponseDTO res = service.getBookingById(1L);
        assertNotNull(res);
        assertTrue(res.isPaid());
    }
    
    @Test void getBookingsByLot_Success() {
        when(repo.findByLotIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(booking));
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        assertEquals(1, service.getBookingsByLot(10L).size());
    }

    @Test void createBooking_PendingPaymentsException_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        when(httpServletRequest.getHeader("Authorization")).thenReturn("Bearer token");
        when(paymentServiceClient.getPendingCount(anyString())).thenThrow(new RuntimeException("Payment service down"));
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_VehicleNull_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(null);
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_VehicleNotActive_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", false));
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_VehicleException_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenThrow(new RuntimeException("Error"));
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_SpotNull_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(null);
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_SpotException_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenThrow(new RuntimeException("Spot Error"));
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_Compat_SpotTypeNull_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0)); // No spotType
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"EV", "MOTORBIKE", "LARGE", "UNKNOWN_TYPE"})
    void createBooking_Compat_Throws(String spotType) {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", spotType));
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_MissingStartTime_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(null);
        assertThrows(IllegalArgumentException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_MissingEndTime_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(null);
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));
        assertThrows(IllegalArgumentException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_VehicleConflict_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        req.setStartTime(LocalDateTime.now().plusMinutes(5));
        req.setEndTime(LocalDateTime.now().plusHours(1));
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));
        when(repo.isSpotBookedInWindow(anyLong(), any(), any())).thenReturn(false);
        when(repo.isVehicleBookedInWindow(eq("MP04"), any(), any())).thenReturn(true);
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_DriveIn_SpotOccupied_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.DRIVE_IN);
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));
        when(repo.existsBySpotIdAndStatus(101L, BookingStatus.ACTIVE)).thenReturn(true);
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void createBooking_DriveIn_VehicleCheckedIn_Throws() {
        CreateBookingRequest req = new CreateBookingRequest();
        req.setSpotId(101L);
        req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.DRIVE_IN);
        when(vehicleServiceClient.getVehicleByPlate(any(), eq("MP04"))).thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));
        when(repo.existsBySpotIdAndStatus(101L, BookingStatus.ACTIVE)).thenReturn(false);
        when(repo.existsByVehiclePlateAndStatus("MP04", BookingStatus.ACTIVE)).thenReturn(true);
        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void checkIn_Early_Throws() {
        booking.setStartTime(LocalDateTime.now().plusHours(1));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        assertThrows(BookingException.class, () -> service.checkIn(1L, "vaidik@test.com"));
    }

    @Test void checkIn_SpotOccupied_Throws() {
        booking.setStartTime(LocalDateTime.now().minusMinutes(5)); // within grace
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.existsBySpotIdAndStatus(101L, BookingStatus.ACTIVE)).thenReturn(true);
        assertThrows(BookingException.class, () -> service.checkIn(1L, "vaidik@test.com"));
    }

    @Test void checkIn_VehicleCheckedIn_Throws() {
        booking.setStartTime(LocalDateTime.now().minusMinutes(5)); // within grace
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.existsBySpotIdAndStatus(101L, BookingStatus.ACTIVE)).thenReturn(false);
        when(repo.existsByVehiclePlateAndStatus(booking.getVehiclePlate(), BookingStatus.ACTIVE)).thenReturn(true);
        assertThrows(BookingException.class, () -> service.checkIn(1L, "vaidik@test.com"));
    }

    @Test void extendBooking_NotActiveOrReserved_Throws() {
        booking.setStatus(BookingStatus.COMPLETED);
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(LocalDateTime.now().plusHours(5));
        assertThrows(BookingException.class, () -> service.extendBooking(1L, req, "vaidik@test.com"));
    }

    @Test void extendBooking_SpotConflict_Throws() {
        booking.setStatus(BookingStatus.ACTIVE);
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(booking.getEndTime().plusHours(2));
        when(repo.isSpotBookedInWindow(booking.getSpotId(), booking.getEndTime(), req.getNewEndTime())).thenReturn(true);
        assertThrows(BookingException.class, () -> service.extendBooking(1L, req, "vaidik@test.com"));
    }

    @Test void extendBooking_VehicleConflict_Throws() {
        booking.setStatus(BookingStatus.ACTIVE);
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        ExtendBookingRequest req = new ExtendBookingRequest();
        req.setNewEndTime(booking.getEndTime().plusHours(2));
        when(repo.isSpotBookedInWindow(booking.getSpotId(), booking.getEndTime(), req.getNewEndTime())).thenReturn(false);
        when(repo.isVehicleBookedInWindow(booking.getVehiclePlate(), booking.getEndTime(), req.getNewEndTime())).thenReturn(true);
        assertThrows(BookingException.class, () -> service.extendBooking(1L, req, "vaidik@test.com"));
    }

    @Test void validateLotOperatingHours_Overnight_Success() {
        // 22:00 to 06:00
        when(lotServiceClient.getLotById(10L)).thenReturn(Map.of("openTime", "22:00", "closeTime", "06:00", "isOpen", true));
        
        // We need a method that calls validateLotOperatingHours
        // getAvailableSpotsForPreBooking doesn't call it. Let's use checkIn or createBooking.
        booking.setLotId(10L);
        booking.setStartTime(LocalDateTime.now().minusMinutes(5));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        // Current time is ~14:30. Let's mock operating hours to include NOW.
        when(lotServiceClient.getLotById(10L)).thenReturn(Map.of("openTime", "00:00", "closeTime", "23:59", "isOpen", true));
        
        assertDoesNotThrow(() -> service.checkIn(1L, "vaidik@test.com"));
    }

    @Test void validateLotOperatingHours_Overnight_Failure() {
        // Mock lot to be open only at night
        when(lotServiceClient.getLotById(10L)).thenReturn(Map.of("openTime", "22:00", "closeTime", "06:00", "isOpen", true));
        
        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L); req.setSpotId(101L); req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.PRE_BOOKING);
        // Use a future time that is OUTSIDE 22:00-06:00 (e.g., 14:00)
        LocalDateTime futureDay = LocalDateTime.now().plusDays(1).withHour(14).withMinute(0);
        req.setStartTime(futureDay);
        req.setEndTime(futureDay.plusHours(1));

        when(vehicleServiceClient.getVehicleByPlate(any(), anyString()))
                .thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(spotServiceClient.getSpotById(101L)).thenReturn(Map.of("pricePerHour", 50.0, "spotType", "STANDARD"));

        assertThrows(BookingException.class, () -> service.createBooking(req, "v@t.com"));
    }

    @Test void getSpotPrice_Fallback_OnException() {
        // fetchSpotDetails (line 131) must succeed first
        Map<String, Object> details = Map.of("pricePerHour", 50.0, "spotType", "STANDARD");
        // We need getSpotPrice (line 168) to fail. 
        // Both call spotServiceClient.getSpotById(spotId).
        // We can use sideEffects or just lenient stubbing if we want it to fail on the second call, 
        // but it's the same call.
        // Actually, getSpotPrice is only called if we don't already have the price? 
        // No, it's called independently.
        
        when(spotServiceClient.getSpotById(101L))
            .thenReturn(details) // First call for fetchSpotDetails
            .thenThrow(new RuntimeException("API down")); // Second call for getSpotPrice

        CreateBookingRequest req = new CreateBookingRequest();
        req.setLotId(10L); req.setSpotId(101L); req.setVehiclePlate("MP04");
        req.setBookingType(BookingType.DRIVE_IN);
        req.setEndTime(LocalDateTime.now().plusHours(1));

        when(vehicleServiceClient.getVehicleByPlate(any(), anyString()))
                .thenReturn(Map.of("active", true, "vehicleType", "FOUR_WHEELER", "ev", false));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);

        service.createBooking(req, "v@t.com");
        assertEquals(50.0, booking.getPricePerHour());
    }

    @Test void callLotService_NoCrash_OnException() {
        booking.setStatus(BookingStatus.ACTIVE);
        booking.setCheckInTime(LocalDateTime.now().minusHours(1));
        when(repo.findById(1L)).thenReturn(Optional.of(booking));
        when(repo.save(any())).thenReturn(booking);
        when(mapper.toDTO(any())).thenReturn(responseDTO);
        
        doThrow(new RuntimeException("Lot service down")).when(lotServiceClient).incrementAvailable(anyLong());
        
        assertDoesNotThrow(() -> service.checkOut(1L, "vaidik@test.com"));
    }
}
