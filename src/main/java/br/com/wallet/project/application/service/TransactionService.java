package br.com.wallet.project.application.service;

import br.com.wallet.project.adapter.in.web.response.TransactionResponse;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.application.port.in.TransactionUseCase;
import br.com.wallet.project.application.port.out.IdempotencyRepository;
import br.com.wallet.project.application.port.out.TransactionEventPublisher;
import br.com.wallet.project.application.port.out.WalletEventRepository;
import br.com.wallet.project.application.strategy.TransactionStrategyFactory;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.exception.WalletDomainException;
import br.com.wallet.project.domain.exception.WalletErrors;
import br.com.wallet.project.domain.model.TransactionMessage;
import br.com.wallet.project.shared.factory.WalletEventFactory;
import br.com.wallet.project.shared.mapper.TransactionMessageMapper;
import br.com.wallet.project.shared.mapper.TransactionResponseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionService implements TransactionUseCase {

    private final TransactionEventPublisher eventPublisher;
    private final TransactionStrategyFactory strategyFactory;
    private final IdempotencyRepository idempotencyRepository;
    private final WalletEventRepository walletEventRepository;

    /**
     * Sync path: publish Kafka message and emit a *_REQUESTED event.
     * Returns immediately — processing is async.
     */
    @Override
    @Transactional("transactionManager")
    public TransactionResponse publishTransaction(TransactionCommand command,
                                                   UUID idempotencyId) {
        log.info("{} funds for idempotency id {}", command.getTransactionType(), idempotencyId);

        TransactionMessage message = TransactionMessageMapper.toMessage(command, idempotencyId);
        command.setTransactionId(message.getTransactionId());
        eventPublisher.publish(message);

        WalletEvent requestedEvent = switch (command.getTransactionType()) {
            case DEPOSIT  -> WalletEventFactory.depositRequested(command);
            case WITHDRAW -> WalletEventFactory.withdrawRequested(command);
            case TRANSFER -> WalletEventFactory.transferRequested(command);
        };
        walletEventRepository.save(requestedEvent);

        log.info("{} message published for idempotency id {}",
                command.getTransactionType(), idempotencyId);

        return TransactionResponseMapper.toResponse(message, command.getTransactionType());
    }

    /**
     * Async path (Kafka consumer): performs the actual processing.
     *
     * <p><strong>Error event strategy:</strong>
     * <ul>
     *   <li>On duplicate idempotency: emits {@code IDEMPOTENCY_DUPLICATE_DETECTED} and
     *       throws. Kafka will retry — each retry also emits this event. The definitive
     *       "gave up" record ({@code SENT_TO_DLQ}) is written by {@code @DltHandler}
     *       in {@code KafkaTransactionConsumer}.</li>
     *   <li>On domain error (insufficient funds, wallet not found): just re-throws.
     *       NO *_FAILED event is written here — that would fire once per retry attempt.
     *       The single {@code SENT_TO_DLQ} event written by {@code @DltHandler} is
     *       the authoritative failure record.</li>
     * </ul>
     */
    @Override
    @Transactional("transactionManager")
    public void processTransaction(TransactionCommand command) {
        log.info("Processing {} transaction id {} for user {}",
                command.getTransactionType(), command.getTransactionId(), command.getUserId());

        boolean registered =
                idempotencyRepository.registerIfAbsent(command.getIdempotencyId().toString());

        if (!registered) {
            log.warn("Duplicate transaction detected for idempotency id: {}",
                    command.getIdempotencyId());

            walletEventRepository.save(
                    WalletEventFactory.idempotencyDuplicateDetected(command));

            throw new WalletDomainException(
                    MessageFormat.format(WalletErrors.W0008.message(),
                            command.getTransactionId()),
                    WalletErrors.W0008.name(), WalletErrors.W0008.group());
        }

        try {
            strategyFactory.getStrategy(command.getTransactionType()).execute(command);
            idempotencyRepository.markAsCompleted(command.getIdempotencyId().toString());

        } catch (WalletDomainException e) {
            idempotencyRepository.release(command.getIdempotencyId().toString());
            log.warn("Domain error during transaction processing. "
                    + "transactionId={} type={} reason={} — Kafka will retry.",
                    command.getTransactionId(), command.getTransactionType(),
                    e.getMessage());

            throw e;
        }

        log.info("Transaction processed successfully for id {}", command.getTransactionId());
    }
}
