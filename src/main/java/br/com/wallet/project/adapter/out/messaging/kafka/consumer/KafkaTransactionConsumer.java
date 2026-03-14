package br.com.wallet.project.adapter.out.messaging.kafka.consumer;
import br.com.wallet.project.application.port.in.TransactionUseCase;
import br.com.wallet.project.domain.model.TransactionMessage;
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
/** Driven adapter: consumes transaction events from Kafka. */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionConsumer {
    private final TransactionUseCase transactionUseCase;
    @RetryableTopic(backoff = @Backoff(delay = 1000, multiplier = 2.0), topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE)
    @KafkaListener(topics = "${wallet.kafka.topics.transactions}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(TransactionMessage message, Acknowledgment acknowledgment,
                        @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                        @Header(KafkaHeaders.OFFSET) long offset) {
        log.info("Processing message with transaction id {}", message.getTransactionId());
        log.info("Message received transactionId={} type={} userId={} partition={} offset={} thread={}",
                message.getTransactionId(),
                message.getType(),
                message.getUserId(),
                partition,
                offset,
                Thread.currentThread().getName());
        try {
            transactionUseCase.processTransaction(TransactionCommandMapper.fromMessage(message));
            acknowledgment.acknowledge();
            log.info("Message processed successfully transactionId={} type={} partition={} thread={}",
                    message.getTransactionId(),
                    message.getType(),
                    partition,
                    Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("Error processing message transactionId={} type={} partition={} error={}",
                    message.getTransactionId(),
                    message.getType(),
                    partition,
                    e.getMessage());
            throw new RuntimeException(e);
        }
    }
    @DltHandler
    public void deadLetter(@Payload TransactionMessage message, Acknowledgment acknowledgment) {
        log.warn("Message {} sent to DLQ", message.getTransactionId());
        acknowledgment.acknowledge();
    }
}
