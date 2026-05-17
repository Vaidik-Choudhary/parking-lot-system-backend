package com.parkease.payment.config;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RazorpayConfig {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(RazorpayConfig.class);

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Bean
    public RazorpayClient razorpayClient() throws RazorpayException {
        if (log.isInfoEnabled()) {
            log.info("Initializing Razorpay client with key: {}...", keyId.substring(0, 8));
        }
        return new RazorpayClient(keyId, keySecret);
    }
}
