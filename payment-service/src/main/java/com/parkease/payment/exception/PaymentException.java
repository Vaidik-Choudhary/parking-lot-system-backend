package com.parkease.payment.exception;

public class PaymentException extends RuntimeException {
    public PaymentException(String msg) { super(msg); }
    public PaymentException(String msg, Throwable cause) { super(msg, cause); }
}
