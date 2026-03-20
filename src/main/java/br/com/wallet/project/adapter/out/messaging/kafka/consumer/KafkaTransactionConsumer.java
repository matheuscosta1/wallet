package br.com.wallet.project.adapter.out.messaging.kafka.consumer;

import br.com.wallet.project.application.port.in.TransactionUseCase;
import br.com.wallet.project.application.port.out.WalletEventRepository;
import br.com.wallet.project.domain.event.WalletEvent;
import br.com.wallet.project.domain.model.TransactionMessage;
import br.com.wallet.project.shared.factory.WalletEventFactory;
import br.com.wallet.project.shared.mapper.TransactionCommandMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Driven adapter: consumes transaction events from Kafka.
 *
 * <p><strong>Event sourcing strategy for the full retry lifecycle:</strong>
 * <pre>
 *   Attempt 1 (original):
 *     consume() → processTransaction() throws
 *               → emit TRANSACTION_RETRY_ATTEMPTED (attempt=1)
 *               → re-throw → Kafka schedules retry with backoff
 *
 *   Attempt 2..N (retries):
 *     same path, attempt number increments each time
 *
 *   After all attempts exhausted → DLT:
 *     @DltHandler → emit *_PERMANENTLY_FAILED (exactly once)
 * </pre>
 *
 * <p>This gives the event log full timeline visibility:
 * when each attempt happened, what failed, and the final outcome —
 * without depending on external log files.
 *
 * <p>{@code attempts = "4"} means 1 original + 3 retries.
 * The {@code deliveryAttempt} header counts from 1.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionConsumer {

    private static final int MAX_ATTEMPTS = 4;

    private final TransactionUseCase transactionUseCase;
    private final WalletEventRepository walletEventRepository;

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(
            topics = "${wallet.kafka.topics.transactions}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional("transactionManager")
    public void consume(
            TransactionMessage message,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(value = "kafka_deliveryAttempt", required = false) Integer deliveryAttempt) {

        int attempt = deliveryAttempt != null ? deliveryAttempt : 1;

        log.info("Message received transactionId={} type={} userId={} "
                        + "partition={} offset={} attempt={}/{} thread={}",
                message.getTransactionId(), message.getType(), message.getUserId(),
                partition, offset, attempt, MAX_ATTEMPTS,
                Thread.currentThread().getName());

        try {
            transactionUseCase.processTransaction(
                    TransactionCommandMapper.fromMessage(message));
            acknowledgment.acknowledge();

            log.info("Message processed successfully transactionId={} type={} "
                            + "partition={} attempt={} thread={}",
                    message.getTransactionId(), message.getType(),
                    partition, attempt, Thread.currentThread().getName());

        } catch (Exception e) {
            log.warn("Processing failed transactionId={} type={} attempt={}/{} reason={}",
                    message.getTransactionId(), message.getType(),
                    attempt, MAX_ATTEMPTS, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Invoked exactly once after ALL retry attempts are exhausted.
     *
     * <p>Emits a specific {@code *_PERMANENTLY_FAILED} event — the definitive "this message will never be
     * processed" record. At this point, {@code TRANSACTION_RETRY_ATTEMPTED} events
     * for every failed attempt already exist in the log, giving the full timeline.
     *
     * <p>The complete picture in the event store for a fully-failed deposit:
     * <pre>
     *   DEPOSIT_REQUESTED              (sync, 1x)
     *   DEPOSIT_PERMANENTLY_FAILED     retryCount=4
     * </pre>
     */
    @DltHandler
    @Transactional("transactionManager")
    public void deadLetter(
            @Payload TransactionMessage message,
            Acknowledgment acknowledgment,
            @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false)
                    String exceptionMessage,
            @Header(value = "kafka_deliveryAttempt", required = false)
                    Integer deliveryAttempt) {

        int retryCount = deliveryAttempt != null ? deliveryAttempt : MAX_ATTEMPTS;
        String reason = exceptionMessage != null ? exceptionMessage : "Unknown error";

        log.warn("Message sent to DLQ transactionId={} type={} userId={} "
                        + "reason='{}' totalAttempts={}",
                message.getTransactionId(), message.getType(), message.getUserId(),
                reason, retryCount);

        WalletEvent permanentFailure = switch (message.getType()) {
            case DEPOSIT  -> WalletEventFactory.depositPermanentlyFailed(message, reason, retryCount);
            case WITHDRAW -> WalletEventFactory.withdrawPermanentlyFailed(message, reason, retryCount);
            case TRANSFER -> WalletEventFactory.transferPermanentlyFailed(message, reason, retryCount);
        };
        walletEventRepository.save(permanentFailure);

        acknowledgment.acknowledge();
    }
}
