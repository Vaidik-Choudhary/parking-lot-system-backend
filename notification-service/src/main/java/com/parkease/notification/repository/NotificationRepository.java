package com.parkease.notification.repository;

import com.parkease.notification.entity.Notification;
import com.parkease.notification.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientEmailOrderBySentAtDesc(String recipientEmail);

    List<Notification> findByRecipientEmailAndIsReadFalseOrderBySentAtDesc(String email);

    int countByRecipientEmailAndIsReadFalse(String email);

    List<Notification> findByRecipientEmailAndType(String email, NotificationType type);

    List<Notification> findByRelatedIdAndRelatedType(Long relatedId, String relatedType);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipientEmail = :email AND n.isRead = false")
    int markAllAsRead(@Param("email") String email);

    void deleteAllByRecipientEmail(String email);
}
