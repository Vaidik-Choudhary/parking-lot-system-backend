package com.parkease.booking.service;

import com.parkease.booking.client.SpotServiceClient;
import com.parkease.booking.client.PaymentServiceClient;
import com.parkease.booking.dto.request.CreateSubscriptionRequest;
import com.parkease.booking.dto.response.SubscriptionRequestResponseDTO;
import com.parkease.booking.dto.response.SubscriptionResponseDTO;
import com.parkease.booking.entity.*;
import com.parkease.booking.exception.BookingException;
import com.parkease.booking.mapper.SubscriptionMapper;
import com.parkease.booking.messaging.NotificationPublisher;
import com.parkease.booking.repository.BookingRepository;
import com.parkease.booking.repository.SubscriptionRequestRepository;
import com.parkease.booking.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock
    private SubscriptionRequestRepository requestRepo;

    @Mock
    private SubscriptionRepository subscriptionRepo;

    @Mock
    private BookingRepository bookingRepo;

    @Mock
    private SubscriptionMapper mapper;

    @Mock
    private SpotServiceClient spotServiceClient;

    @Mock
    private PaymentServiceClient paymentServiceClient;

    @Mock
    private NotificationPublisher notificationPublisher;

    @InjectMocks
    private SubscriptionServiceImpl service;

    private CreateSubscriptionRequest createReq;
    private SubscriptionRequest subReq;
    private SubscriptionRequestResponseDTO reqDto;

    @BeforeEach
    void setUp() {
        createReq = new CreateSubscriptionRequest();
        createReq.setLotId(1L);
        createReq.setSpotId(10L);

        subReq = SubscriptionRequest.builder()
                .id(100L)
                .lotId(1L)
                .spotId(10L)
                .driverEmail("driver@test.com")
                .status(SubscriptionRequestStatus.PENDING)
                .build();

        reqDto = new SubscriptionRequestResponseDTO();
    }

    @Test
    void createRequest_Success() {
        when(spotServiceClient.getSpotById(10L)).thenReturn(Map.of("monthlySubscriptionEnabled", true));
        when(requestRepo.save(any())).thenReturn(subReq);
        when(mapper.toRequestDTO(any())).thenReturn(reqDto);

        assertNotNull(service.createRequest(createReq, "driver@test.com"));
        verify(requestRepo).save(any());
    }

    @Test
    void createRequest_NotEnabled() {
        when(spotServiceClient.getSpotById(10L)).thenReturn(Map.of("monthlySubscriptionEnabled", false));
        assertThrows(BookingException.class, () -> service.createRequest(createReq, "d@t.com"));
    }

    @Test
    void getPendingRequestsByLot_Success() {
        when(requestRepo.findByLotIdAndStatus(1L, SubscriptionRequestStatus.PENDING)).thenReturn(List.of(subReq));
        when(mapper.toRequestDTO(any())).thenReturn(reqDto);
        assertEquals(1, service.getPendingRequestsByLot(1L).size());
    }

    @Test
    void getDriverRequests_Success() {
        when(requestRepo.findByDriverEmailOrderByCreatedAtDesc("d@t.com")).thenReturn(List.of(subReq));
        when(mapper.toRequestDTO(any())).thenReturn(reqDto);
        assertEquals(1, service.getDriverRequests("d@t.com").size());
    }

    @Test
    void approveRequest_Success() {
        when(requestRepo.findById(100L)).thenReturn(Optional.of(subReq));
        when(spotServiceClient.getSpotById(10L)).thenReturn(Map.of("monthlyRate", 5000.0));
        when(bookingRepo.findActiveOrReservedBookingsForSpot(10L)).thenReturn(List.of());
        
        Subscription sub = Subscription.builder().id(50L).build();
        when(subscriptionRepo.save(any())).thenReturn(sub);
        when(mapper.toDTO(any())).thenReturn(new SubscriptionResponseDTO());

        assertNotNull(service.approveRequest(100L, "Looks good"));
        assertEquals(SubscriptionRequestStatus.APPROVED, subReq.getStatus());
        verify(notificationPublisher, times(1)).publish(any()); // Request approved
    }

    @Test
    void approveRequest_AlreadyProcessed() {
        subReq.setStatus(SubscriptionRequestStatus.REJECTED);
        when(requestRepo.findById(100L)).thenReturn(Optional.of(subReq));
        assertThrows(BookingException.class, () -> service.approveRequest(100L, "Ok"));
    }

    @Test
    void rejectRequest_Success() {
        when(requestRepo.findById(100L)).thenReturn(Optional.of(subReq));
        service.rejectRequest(100L, "Not enough history");
        assertEquals(SubscriptionRequestStatus.REJECTED, subReq.getStatus());
        verify(notificationPublisher).publish(any());
    }

    @Test
    void getActiveSubscriptionsByLot_Success() {
        when(subscriptionRepo.findByLotIdAndStatus(1L, SubscriptionStatus.ACTIVE)).thenReturn(List.of(new Subscription()));
        when(mapper.toDTO(any())).thenReturn(new SubscriptionResponseDTO());
        assertEquals(1, service.getActiveSubscriptionsByLot(1L).size());
    }

    @Test
    void getMyActiveSubscriptions_Success() {
        Subscription sub = Subscription.builder().driverEmail("d@t.com").status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepo.findAll()).thenReturn(List.of(sub));
        when(mapper.toDTO(any())).thenReturn(new SubscriptionResponseDTO());
        assertEquals(1, service.getMyActiveSubscriptions("d@t.com").size());
    }

    @Test
    void getSubscribedSpotIds_Success() {
        Subscription sub = Subscription.builder().lotId(1L).spotId(10L).status(SubscriptionStatus.ACTIVE).build();
        when(subscriptionRepo.findAll()).thenReturn(List.of(sub));
        when(requestRepo.findByLotIdAndStatus(1L, SubscriptionRequestStatus.PENDING)).thenReturn(List.of(subReq));
        
        List<Long> result = service.getSubscribedSpotIds(1L);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0));
    }

    @Test
    void cancelSubscription_Scheduled() {
        Subscription sub = Subscription.builder().id(50L).driverEmail("d@t.com").status(SubscriptionStatus.SCHEDULED).build();
        when(subscriptionRepo.findById(50L)).thenReturn(Optional.of(sub));
        
        service.cancelSubscription(50L, "d@t.com");
        
        assertEquals(SubscriptionStatus.CANCELLED, sub.getStatus());
        verify(subscriptionRepo).save(sub);
        verify(notificationPublisher).publish(any());
    }

    @Test
    void cancelSubscription_Active() {
        Subscription sub = Subscription.builder()
            .id(50L)
            .driverEmail("d@t.com")
            .status(SubscriptionStatus.ACTIVE)
            .spotId(10L)
            .monthlyRate(3000.0)
            .startDate(LocalDateTime.now().minusDays(10))
            .endDate(LocalDateTime.now().plusDays(20))
            .build();
        when(subscriptionRepo.findById(50L)).thenReturn(Optional.of(sub));
        
        service.cancelSubscription(50L, "d@t.com");
        
        assertEquals(SubscriptionStatus.CANCELLED, sub.getStatus());
        verify(subscriptionRepo).save(sub);
        verify(spotServiceClient).releaseSpot(10L);
        verify(paymentServiceClient).initializeSubscriptionPayment(any());
        verify(notificationPublisher).publish(any());
    }

    @Test
    void cancelSubscription_WrongEmail() {
        Subscription sub = Subscription.builder().id(50L).driverEmail("other@t.com").build();
        when(subscriptionRepo.findById(50L)).thenReturn(Optional.of(sub));
        assertThrows(BookingException.class, () -> service.cancelSubscription(50L, "d@t.com"));
    }

    @Test
    void cancelSubscription_NotActiveOrScheduled() {
        Subscription sub = Subscription.builder().id(50L).driverEmail("d@t.com").status(SubscriptionStatus.CANCELLED).build();
        when(subscriptionRepo.findById(50L)).thenReturn(Optional.of(sub));
        assertThrows(BookingException.class, () -> service.cancelSubscription(50L, "d@t.com"));
    }
    @Test
    void processDailyActivations_Success() {
        Subscription sub = Subscription.builder().id(50L).status(SubscriptionStatus.SCHEDULED).build();
        when(subscriptionRepo.findByStatusAndStartDateLessThanEqual(eq(SubscriptionStatus.SCHEDULED), any())).thenReturn(List.of(sub));
        
        service.processDailyActivations();
        
        assertEquals(SubscriptionStatus.ACTIVE, sub.getStatus());
        verify(subscriptionRepo).save(sub);
    }

    @Test
    void processDailyExpirations_Success() {
        Subscription sub = Subscription.builder()
                .id(50L)
                .driverEmail("d@t.com")
                .status(SubscriptionStatus.ACTIVE)
                .spotId(10L)
                .monthlyRate(3000.0)
                .build();
        when(subscriptionRepo.findByStatusAndEndDateLessThanEqual(eq(SubscriptionStatus.ACTIVE), any())).thenReturn(List.of(sub));
        
        service.processDailyExpirations();
        
        assertEquals(SubscriptionStatus.EXPIRED, sub.getStatus());
        verify(subscriptionRepo).save(sub);
        verify(spotServiceClient).releaseSpot(10L);
        verify(paymentServiceClient).initializeSubscriptionPayment(any());
    }
}
