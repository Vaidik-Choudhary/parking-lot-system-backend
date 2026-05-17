package com.parkease.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubscriptionScheduler {

    private final SubscriptionService subscriptionService;

    /**
     * Runs every day at midnight (00:00).
     * Activates subscriptions that are scheduled to start today.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void activateSubscriptions() {
        log.info("SubscriptionScheduler: Activating scheduled subscriptions...");
        subscriptionService.processDailyActivations();
    }

    /**
     * Runs every day at 11:59 PM.
     * Handles expirations and triggers billing.
     */
    @Scheduled(cron = "0 59 23 * * *")
    public void expireSubscriptions() {
        log.info("SubscriptionScheduler: Expiring active subscriptions and triggering billing...");
        subscriptionService.processDailyExpirations();
    }
}
