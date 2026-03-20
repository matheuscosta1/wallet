package br.com.wallet.project.unit;

import br.com.wallet.project.adapter.out.persistence.WalletEventPersistenceAdapter;
import br.com.wallet.project.adapter.out.persistence.entity.WalletEventEntity;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaWalletEventRepository;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletEventPersistenceAdapterTest {

    @Mock
    private JpaWalletEventRepository jpaWalletEventRepository;

    @InjectMocks
    private WalletEventPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private WalletEvent buildEvent(WalletEventType type, String aggregateId) {
        return WalletEvent.builder()
                .eventId(UUID.randomUUID())
                .aggregateId(aggregateId)
                .aggregateType("WALLET")
                .eventType(type)
                .eventVersion(1)
                .payload(Map.of("userId", aggregateId, "amount", "100.00"))
                .occurredAt(LocalDateTime.now())
                .build();
    }

    private WalletEventEntity buildEntity(WalletEvent event) {
        return WalletEventEntity.builder()
                .id(1L)
                .eventId(event.getEventId())
                .aggregateId(event.getAggregateId())
                .aggregateType(event.getAggregateType())
                .eventType(event.getEventType())
                .eventVersion(event.getEventVersion())
                .payload(event.getPayload())
                .occurredAt(event.getOccurredAt())
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── save ──────────────────────────────────────────────────────────────────

    @Test
    void save_shouldPersistAndReturnDomainEvent() {
        WalletEvent event = buildEvent(WalletEventType.DEPOSIT_COMPLETED, "alice");
        when(jpaWalletEventRepository.existsByEventId(event.getEventId())).thenReturn(false);
        when(jpaWalletEventRepository.save(any())).thenReturn(buildEntity(event));

        WalletEvent result = adapter.save(event);

        assertNotNull(result);
        assertEquals(event.getEventId(), result.getEventId());
        assertEquals(WalletEventType.DEPOSIT_COMPLETED, result.getEventType());
        verify(jpaWalletEventRepository).save(any());
    }

    @Test
    void save_shouldSkipDuplicateEventId() {
        WalletEvent event = buildEvent(WalletEventType.DEPOSIT_COMPLETED, "alice");
        when(jpaWalletEventRepository.existsByEventId(event.getEventId())).thenReturn(true);

        WalletEvent result = adapter.save(event);

        // Returns the original event unchanged, but does NOT call save
        assertEquals(event.getEventId(), result.getEventId());
        verify(jpaWalletEventRepository, never()).save(any());
    }

    // ── findByAggregateId ─────────────────────────────────────────────────────

    @Test
    void findByAggregateId_shouldReturnMappedEvents() {
        WalletEvent event = buildEvent(WalletEventType.WALLET_CREATED, "alice");
        when(jpaWalletEventRepository.findByAggregateIdOrderByOccurredAtAsc("alice"))
                .thenReturn(List.of(buildEntity(event)));

        List<WalletEvent> results = adapter.findByAggregateId("alice");

        assertEquals(1, results.size());
        assertEquals("alice", results.get(0).getAggregateId());
    }

    @Test
    void findByAggregateId_shouldReturnEmptyListWhenNoEvents() {
        when(jpaWalletEventRepository.findByAggregateIdOrderByOccurredAtAsc("ghost"))
                .thenReturn(List.of());

        List<WalletEvent> results = adapter.findByAggregateId("ghost");

        assertTrue(results.isEmpty());
    }

    // ── findByEventId ─────────────────────────────────────────────────────────

    @Test
    void findByEventId_shouldReturnPresentOptional() {
        WalletEvent event = buildEvent(WalletEventType.DEPOSIT_REQUESTED, "alice");
        when(jpaWalletEventRepository.findByEventId(event.getEventId()))
                .thenReturn(Optional.of(buildEntity(event)));

        Optional<WalletEvent> result = adapter.findByEventId(event.getEventId());

        assertTrue(result.isPresent());
        assertEquals(event.getEventId(), result.get().getEventId());
    }

    @Test
    void findByEventId_shouldReturnEmptyOptionalWhenNotFound() {
        UUID unknownId = UUID.randomUUID();
        when(jpaWalletEventRepository.findByEventId(unknownId)).thenReturn(Optional.empty());

        Optional<WalletEvent> result = adapter.findByEventId(unknownId);

        assertTrue(result.isEmpty());
    }

    // ── findByCorrelationId ───────────────────────────────────────────────────

    @Test
    void findByCorrelationId_shouldReturnChainEvents() {
        UUID correlationId = UUID.randomUUID();
        WalletEvent requested  = buildEvent(WalletEventType.DEPOSIT_REQUESTED, "alice");
        WalletEvent completed  = buildEvent(WalletEventType.DEPOSIT_COMPLETED, "alice");
        when(jpaWalletEventRepository.findByCorrelationIdOrderByOccurredAtAsc(correlationId))
                .thenReturn(List.of(buildEntity(requested), buildEntity(completed)));

        List<WalletEvent> results = adapter.findByCorrelationId(correlationId);

        assertEquals(2, results.size());
    }

    // ── existsByEventId ───────────────────────────────────────────────────────

    @Test
    void existsByEventId_shouldDelegateToJpa() {
        UUID id = UUID.randomUUID();
        when(jpaWalletEventRepository.existsByEventId(id)).thenReturn(true);

        assertTrue(adapter.existsByEventId(id));
        verify(jpaWalletEventRepository).existsByEventId(id);
    }

    // ── findByAggregateIdAndEventType ─────────────────────────────────────────

    @Test
    void findByAggregateIdAndEventType_shouldFilterCorrectly() {
        WalletEvent event = buildEvent(WalletEventType.DEPOSIT_COMPLETED, "alice");
        when(jpaWalletEventRepository.findByAggregateIdAndEventTypeOrderByOccurredAtAsc(
                "alice", WalletEventType.DEPOSIT_COMPLETED))
                .thenReturn(List.of(buildEntity(event)));

        List<WalletEvent> results = adapter.findByAggregateIdAndEventType(
                "alice", WalletEventType.DEPOSIT_COMPLETED);

        assertEquals(1, results.size());
        assertEquals(WalletEventType.DEPOSIT_COMPLETED, results.get(0).getEventType());
    }
}
