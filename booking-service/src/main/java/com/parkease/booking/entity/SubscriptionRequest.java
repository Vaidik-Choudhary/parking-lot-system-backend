package com.parkease.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "subscription_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SubscriptionRequest extends BaseEntity {

    @Column(nullable = false)
    private String driverEmail;

    @Column(nullable = false)
    private Long lotId;

    @Column(nullable = false)
    private Long spotId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionRequestStatus status;

    private String managerComment;

    @Override
    public void prePersist() {
        super.prePersist();
        if (status == null) status = SubscriptionRequestStatus.PENDING;
    }
}
