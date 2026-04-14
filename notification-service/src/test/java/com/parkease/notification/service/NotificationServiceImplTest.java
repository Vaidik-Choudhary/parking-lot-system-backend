package com.parkease.notification.service;

import com.parkease.notification.dto.request.BroadcastRequest;
import com.parkease.notification.dto.request.SendNotificationRequest;
import com.parkease.notification.dto.response.NotificationResponseDTO;
import com.parkease.notification.entity.Notification;
import com.parkease.notification.entity.NotificationChannel;
import com.parkease.notification.entity.NotificationType;
import com.parkease.notification.exception.ResourceNotFoundException;
import com.parkease.notification.mapper.NotificationMapper;
import com.parkease.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repo;

    @Mock
    private NotificationMapper mapper;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificationServiceImpl service;

    private Notification notification;
    private NotificationResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .notificationId(1L)
                .recipientEmail("vaidik@test.com")
                .type(NotificationType.BOOKING_CONFIRMED)
                .channel(NotificationChannel.IN_APP)
                .title("Booking Confirmed")
                .message("Your booking is confirmed")
                .isRead(false)
                .build();

        responseDTO = new NotificationResponseDTO();
    }

    @Test
    void shouldSendInAppNotificationSuccessfully() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientEmail("vaidik@test.com");
        request.setType(NotificationType.BOOKING_CONFIRMED);
        request.setChannel(NotificationChannel.IN_APP);
        request.setTitle("Booking Confirmed");
        request.setMessage("Your booking is confirmed");

        when(repo.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toDTO(notification)).thenReturn(responseDTO);

        NotificationResponseDTO result = service.send(request);

        assertNotNull(result);
        verify(repo).save(any(Notification.class));
        verify(emailService, never()).sendEmail(any(), any(), any(), any());
    }

    @Test
    void shouldSendEmailOnlyNotificationSuccessfully() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientEmail("vaidik@test.com");
        request.setType(NotificationType.BOOKING_CONFIRMED);
        request.setChannel(NotificationChannel.EMAIL);
        request.setTitle("Booking Confirmed");
        request.setMessage("Your booking is confirmed");

        NotificationResponseDTO result = service.send(request);

        assertNotNull(result);
        verify(repo, never()).save(any());
        verify(emailService).sendEmail(
                eq("vaidik@test.com"),
                eq("Booking Confirmed"),
                eq("Booking Confirmed"),
                eq("Your booking is confirmed")
        );
    }

    @Test
    void shouldSendBothNotificationSuccessfully() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setRecipientEmail("vaidik@test.com");
        request.setType(NotificationType.BOOKING_CONFIRMED);
        request.setChannel(NotificationChannel.BOTH);
        request.setTitle("Booking Confirmed");
        request.setMessage("Your booking is confirmed");

        when(repo.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toDTO(notification)).thenReturn(responseDTO);

        NotificationResponseDTO result = service.send(request);

        assertNotNull(result);
        verify(repo).save(any(Notification.class));
        verify(emailService).sendEmail(any(), any(), any(), any());
    }

    @Test
    void shouldBroadcastSuccessfully() {
        BroadcastRequest request = new BroadcastRequest();
        request.setTitle("System Alert");
        request.setMessage("Maintenance tonight");

        int result = service.broadcast(
                request,
                List.of("a@test.com", "b@test.com")
        );

        assertEquals(2, result);
        verify(repo).saveAll(anyList());
    }

    @Test
    void shouldReturnZeroWhenBroadcastRecipientsEmpty() {
        BroadcastRequest request = new BroadcastRequest();

        int result = service.broadcast(request, List.of());

        assertEquals(0, result);
        verify(repo, never()).saveAll(anyList());
    }

    @Test
    void shouldGetMyNotificationsSuccessfully() {
        when(repo.findByRecipientEmailOrderBySentAtDesc("vaidik@test.com"))
                .thenReturn(List.of(notification));
        when(mapper.toDTO(notification)).thenReturn(responseDTO);

        List<NotificationResponseDTO> result =
                service.getMyNotifications("vaidik@test.com");

        assertEquals(1, result.size());
    }

    @Test
    void shouldGetUnreadCountSuccessfully() {
        when(repo.countByRecipientEmailAndIsReadFalse("vaidik@test.com"))
                .thenReturn(5);

        int result = service.getUnreadCount("vaidik@test.com");

        assertEquals(5, result);
    }

    @Test
    void shouldMarkAsReadSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(notification));
        when(repo.save(notification)).thenReturn(notification);
        when(mapper.toDTO(notification)).thenReturn(responseDTO);

        NotificationResponseDTO result =
                service.markAsRead(1L, "vaidik@test.com");

        assertNotNull(result);
        assertTrue(notification.isRead());
    }

    @Test
    void shouldThrowWhenMarkingOthersNotification() {
        when(repo.findById(1L)).thenReturn(Optional.of(notification));

        assertThrows(ResourceNotFoundException.class,
                () -> service.markAsRead(1L, "other@test.com"));
    }

    @Test
    void shouldMarkAllAsReadSuccessfully() {
        when(repo.markAllAsRead("vaidik@test.com")).thenReturn(4);

        int result = service.markAllAsRead("vaidik@test.com");

        assertEquals(4, result);
    }

    @Test
    void shouldDeleteNotificationSuccessfully() {
        when(repo.findById(1L)).thenReturn(Optional.of(notification));

        service.deleteNotification(1L, "vaidik@test.com");

        verify(repo).delete(notification);
    }
}