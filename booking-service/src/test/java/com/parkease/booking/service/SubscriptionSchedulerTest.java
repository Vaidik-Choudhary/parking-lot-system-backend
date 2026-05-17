package com.parkease.booking.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionSchedulerTest {

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private SubscriptionScheduler scheduler;

    @Test
    void testActivateSubscriptions() {
        scheduler.activateSubscriptions();
        verify(subscriptionService).processDailyActivations();
    }

    @Test
    void testExpireSubscriptions() {
        scheduler.expireSubscriptions();
        verify(subscriptionService).processDailyExpirations();
    }
}
