package br.com.wallet.project.application.service;
import br.com.wallet.project.adapter.in.web.response.TransactionResponse;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.application.port.in.TransactionUseCase;
import br.com.wallet.project.application.port.out.IdempotencyRepository;
import br.com.wallet.project.application.port.out.TransactionEventPublisher;
import br.com.wallet.project.application.strategy.TransactionStrategyFactory;
import br.com.wallet.project.domain.exception.WalletDomainException;
import br.com.wallet.project.domain.exception.WalletErrors;
import br.com.wallet.project.domain.model.TransactionMessage;
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
    @Override
    @Transactional("transactionManager")
    public TransactionResponse publishTransaction(TransactionCommand command, UUID idempotencyId) {
        log.info("{} funds for idempotency id {}", command.getTransactionType(), idempotencyId);
        TransactionMessage message = TransactionMessageMapper.toMessage(command, idempotencyId);
        eventPublisher.publish(message);
        log.info("{} message published for idempotency id {}", command.getTransactionType(), idempotencyId);
        return TransactionResponseMapper.toResponse(message, command.getTransactionType());
    }
    @Override
    @Transactional("transactionManager")
    public void processTransaction(TransactionCommand command) {
        log.info("Processing {} transaction id {} for user {}",
            command.getTransactionType(), command.getTransactionId(), command.getUserId());
        boolean registered = idempotencyRepository.registerIfAbsent(command.getIdempotencyId().toString());
        if (!registered) {
            log.warn("Duplicate transaction detected for idempotency id: {}", command.getIdempotencyId());
            throw new WalletDomainException(
                MessageFormat.format(WalletErrors.W0008.message(), command.getTransactionId()),
                WalletErrors.W0008.name(), WalletErrors.W0008.group());
        }
        try {
            strategyFactory.getStrategy(command.getTransactionType()).execute(command);
            idempotencyRepository.markAsCompleted(command.getIdempotencyId().toString());
        } catch (WalletDomainException e) {
            idempotencyRepository.release(command.getIdempotencyId().toString());
            throw e;
        }
        log.info("Transaction processed successfully for id {}", command.getTransactionId());
    }
}
