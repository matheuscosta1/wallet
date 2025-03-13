package br.com.wallet.project.infrastructure.messaging.kafka.consumer;

import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.service.WalletService;
import br.com.wallet.project.infrastructure.messaging.WalletTransactionConsumer;
import br.com.wallet.project.mapper.TransactionRequestMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaWalletConsumer implements WalletTransactionConsumer {
    private final WalletService walletService;

    public KafkaWalletConsumer(WalletService walletService) {
        this.walletService = walletService;
    }

    @RetryableTopic(
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "${wallet.kafka.topics.transactions}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenWithAcknowledgment(TransactionMessage message, Acknowledgment acknowledgment) {
        log.info("Processing message with transaction id {}", message.getTransactionId());
        try {
            listen(message);
            acknowledgment.acknowledge();
            log.info("Message with transaction id {} was processed successfully", message.getTransactionId());
        } catch (Exception e) {
            log.error("Error processing message with transaction id {}", message.getTransactionId(), e);
            throw new RuntimeException();
        }
    }

    @DltHandler
    public void dlt(@Payload TransactionMessage payload, Acknowledgment acknowledgment) {
        log.info("Message was published into dlq for transaction id {}", payload.getTransactionId());
        acknowledgment.acknowledge();
    }

    @Override
    public void listen(TransactionMessage message) {
        walletService.transactionProcessor(TransactionRequestMapper.mapTransactionMessageToTransactionRequest(message));
    }
}