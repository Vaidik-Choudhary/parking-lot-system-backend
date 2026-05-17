package com.parkease.auth.mapper;

import com.parkease.auth.dto.request.SupportTicketRequest;
import com.parkease.auth.entity.SupportTicket;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketMapper {

    public SupportTicket toEntity(SupportTicketRequest request) {
        if (request == null) return null;
        return SupportTicket.builder()
                .userId(request.getUserId())
                .userName(request.getUserName())
                .userRole(request.getUserRole())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(SupportTicket.TicketStatus.OPEN)
                .priority(SupportTicket.TicketPriority.MEDIUM)
                .build();
    }
}
