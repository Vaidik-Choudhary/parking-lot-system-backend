package com.parkease.auth.controller;

import com.parkease.auth.entity.SupportTicket;
import com.parkease.auth.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketRepository repository;
    private final com.parkease.auth.mapper.SupportTicketMapper mapper;

    @PostMapping("/tickets")
    public ResponseEntity<SupportTicket> createTicket(@RequestBody com.parkease.auth.dto.request.SupportTicketRequest request) {
        SupportTicket ticket = mapper.toEntity(request);
        return ResponseEntity.ok(repository.save(ticket));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<SupportTicket>> getMyTickets(@RequestParam String userId) {
        return ResponseEntity.ok(repository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/admin/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportTicket>> getAllTickets() {
        return ResponseEntity.ok(repository.findAllByOrderByCreatedAtDesc());
    }

    @PatchMapping("/admin/tickets/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SupportTicket> updateStatus(
            @PathVariable Long id,
            @RequestParam SupportTicket.TicketStatus status,
            @RequestParam(required = false) String notes) {
        
        SupportTicket ticket = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus(status);
        if (notes != null) {
            ticket.setAdminNotes(notes);
        }
        ticket.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(repository.save(ticket));
    }
}
