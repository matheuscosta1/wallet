package br.com.wallet.project.adapter.in.web.controller;

import br.com.wallet.project.application.port.in.WalletEventUseCase;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Driving adapter: exposes the event store over HTTP.
 *
 * <p>All endpoints are read-only — the event log is append-only
 * and must never be modified via API.
 */
@RestController
@RequestMapping("events")
@RequiredArgsConstructor
@Tag(name = "Wallet Events")
public class WalletEventController {

    private final WalletEventUseCase walletEventUseCase;

    @Operation(summary = "Full event history for a wallet aggregate")
    @GetMapping("/aggregate/{aggregateId}")
    public ResponseEntity<List<WalletEvent>> getAggregateHistory(
            @PathVariable String aggregateId) {
        return ResponseEntity.ok(walletEventUseCase.getAggregateHistory(aggregateId));
    }

    @Operation(summary = "All events linked by the same correlation id (full request trace)")
    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<WalletEvent>> getByCorrelationId(
            @PathVariable UUID correlationId) {
        return ResponseEntity.ok(
                walletEventUseCase.getEventsByCorrelationId(correlationId));
    }

    @Operation(summary = "Find a single event by its unique event id")
    @GetMapping("/{eventId}")
    public ResponseEntity<WalletEvent> getById(@PathVariable UUID eventId) {
        return walletEventUseCase.getEventById(eventId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Events for an aggregate filtered by type")
    @GetMapping("/aggregate/{aggregateId}/type/{eventType}")
    public ResponseEntity<List<WalletEvent>> getByAggregateAndType(
            @PathVariable String aggregateId,
            @PathVariable WalletEventType eventType) {
        return ResponseEntity.ok(
                walletEventUseCase.getEventsByAggregateIdAndType(aggregateId, eventType));
    }

    @Operation(summary = "Events within a date range (ISO format: yyyy-MM-dd'T'HH:mm:ss)")
    @GetMapping("/range")
    public ResponseEntity<List<WalletEvent>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(walletEventUseCase.getEventsByDateRange(start, end));
    }
}
