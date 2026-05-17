package com.parkease.payment.dto.response;

public class OrderResponseDTO {

    private Long paymentId;        
    private String razorpayOrderId; 
    private double amount;         
    private String currency;   
    private String status;          
    private String razorpayKeyId;

    public OrderResponseDTO() {
        // Empty constructor required by Jackson for JSON deserialization
    }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRazorpayKeyId() { return razorpayKeyId; }
    public void setRazorpayKeyId(String razorpayKeyId) { this.razorpayKeyId = razorpayKeyId; }

    public static OrderResponseDTOBuilder builder() {
        return new OrderResponseDTOBuilder();
    }

    public static class OrderResponseDTOBuilder {
        private final OrderResponseDTO instance = new OrderResponseDTO();
        public OrderResponseDTOBuilder paymentId(Long id) { instance.setPaymentId(id); return this; }
        public OrderResponseDTOBuilder razorpayOrderId(String id) { instance.setRazorpayOrderId(id); return this; }
        public OrderResponseDTOBuilder amount(double a) { instance.setAmount(a); return this; }
        public OrderResponseDTOBuilder currency(String c) { instance.setCurrency(c); return this; }
        public OrderResponseDTOBuilder status(String s) { instance.setStatus(s); return this; }
        public OrderResponseDTOBuilder razorpayKeyId(String k) { instance.setRazorpayKeyId(k); return this; }
        public OrderResponseDTO build() { return instance; }
    }
}
