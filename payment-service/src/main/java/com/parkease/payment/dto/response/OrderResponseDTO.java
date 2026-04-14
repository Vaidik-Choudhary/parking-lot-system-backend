package com.parkease.payment.dto.response;

import lombok.*;


@Data 
@Builder 
@NoArgsConstructor 
@AllArgsConstructor
public class OrderResponseDTO {

    private Long paymentId;        
    private String razorpayOrderId; 
    private double amount;         
    private String currency;   
    private String status;          
    private String razorpayKeyId;
}
