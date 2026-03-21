package br.com.wallet.project.adapter.out.persistence;

import br.com.wallet.project.adapter.out.persistence.jpa.JpaWalletEventRepository;
import br.com.wallet.project.application.port.out.WalletEventRepository;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;
import br.com.wallet.project.shared.mapper.WalletEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Driven adapter: implements {@link WalletEventRepository} using JPA + PostgreSQL.
 *
 * <p>All writes are append-only. Duplicate eventIds are silently dropped
 * (idempotent save) to support at-least-once delivery from Kafka.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class WalletEventPersistenceAdapter implements WalletEventRepository {

    private final JpaWalletEventRepository jpaWalletEventRepository;

    @Override
    public WalletEvent save(WalletEvent event) {
        if (jpaWalletEventRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate event detected and skipped: eventId={} type={}",
                    event.getEventId(), event.getEventType());
            return event;
        }
        var saved = jpaWalletEventRepository.save(WalletEventMapper.toEntity(event));
        log.debug("Event persisted: eventId={} type={} aggregateId={}",
                saved.getEventId(), saved.getEventType(), saved.getAggregateId());
        return WalletEventMapper.toDomain(saved);
    }

    @Override
    public List<WalletEvent> findByAggregateId(String aggregateId) {
        return jpaWalletEventRepository
                .findByAggregateIdOrderByOccurredAtAsc(aggregateId)
                .stream()
                .map(WalletEventMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WalletEvent> findByCorrelationId(UUID correlationId) {
        return jpaWalletEventRepository
                .findByCorrelationIdOrderByOccurredAtAsc(correlationId)
                .stream()
                .map(WalletEventMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<WalletEvent> findByEventId(UUID eventId) {
        return jpaWalletEventRepository
                .findByEventId(eventId)
                .map(WalletEventMapper::toDomain);
    }

    @Override
    public List<WalletEvent> findByAggregateIdAndEventType(String aggregateId,
                                                            WalletEventType eventType) {
        return jpaWalletEventRepository
                .findByAggregateIdAndEventTypeOrderByOccurredAtAsc(aggregateId, eventType)
                .stream()
                .map(WalletEventMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<WalletEvent> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return jpaWalletEventRepository
                .findByDateRange(start, end)
                .stream()
                .map(WalletEventMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByEventId(UUID eventId) {
        return jpaWalletEventRepository.existsByEventId(eventId);
    }
}
