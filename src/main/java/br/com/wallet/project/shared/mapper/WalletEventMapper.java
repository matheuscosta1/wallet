package br.com.wallet.project.shared.mapper;

import br.com.wallet.project.adapter.out.persistence.entity.WalletEventEntity;
import br.com.wallet.project.domain.event.WalletEvent;

public class WalletEventMapper {

    private WalletEventMapper() {}

    public static WalletEvent toDomain(WalletEventEntity entity) {
        if (entity == null) return null;
        return WalletEvent.builder()
                .eventId(entity.getEventId())
                .aggregateId(entity.getAggregateId())
                .aggregateType(entity.getAggregateType())
                .eventType(entity.getEventType())
                .eventVersion(entity.getEventVersion())
                .payload(entity.getPayload())
                .metadata(entity.getMetadata())
                .correlationId(entity.getCorrelationId())
                .causationId(entity.getCausationId())
                .occurredAt(entity.getOccurredAt())
                .build();
    }

    public static WalletEventEntity toEntity(WalletEvent event) {
        return WalletEventEntity.builder()
                .eventId(event.getEventId())
                .aggregateId(event.getAggregateId())
                .aggregateType(event.getAggregateType())
                .eventType(event.getEventType())
                .eventVersion(event.getEventVersion())
                .payload(event.getPayload())
                .metadata(event.getMetadata())
                .correlationId(event.getCorrelationId())
                .causationId(event.getCausationId())
                .occurredAt(event.getOccurredAt())
                .build();
    }
}
