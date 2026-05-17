package com.parkease.payment.dto.request;

import jakarta.validation.constraints.NotNull;

public class RefundRequest {

    @NotNull(message = "Booking ID is required")
    private Long bookingId;

    private String reason;

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
