package br.com.wallet.project.application.service;

import br.com.wallet.project.application.port.in.WalletEventUseCase;
import br.com.wallet.project.application.port.out.WalletEventRepository;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.event.WalletEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletEventService implements WalletEventUseCase {

    private final WalletEventRepository walletEventRepository;

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<WalletEvent> getAggregateHistory(String aggregateId) {
        return walletEventRepository.findByAggregateId(aggregateId);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<WalletEvent> getEventsByCorrelationId(UUID correlationId) {
        return walletEventRepository.findByCorrelationId(correlationId);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public Optional<WalletEvent> getEventById(UUID eventId) {
        return walletEventRepository.findByEventId(eventId);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<WalletEvent> getEventsByAggregateIdAndType(String aggregateId,
                                                            WalletEventType eventType) {
        return walletEventRepository.findByAggregateIdAndEventType(aggregateId, eventType);
    }

    @Override
    @Transactional(value = "transactionManager", readOnly = true)
    public List<WalletEvent> getEventsByDateRange(LocalDateTime start, LocalDateTime end) {
        return walletEventRepository.findByDateRange(start, end);
    }
}
