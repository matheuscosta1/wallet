package br.com.wallet.project.service;

import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.domain.request.TransactionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WalletConsumerService {

    @Autowired
    WalletService walletService;

    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "${wallet.kafka.topics.transactions}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(TransactionMessage message, Acknowledgment acknowledgment) {
        log.info("Processing message with transaction id {}", message.getTransactionId());
        try {
            walletService.transactionOperation(
                    TransactionRequest
                            .builder()
                            .amount(message.getAmount())
                            .userId(message.getUserId())
                            .fromUserWalletId(message.getFromUserId())
                            .toUserWalletId(message.getToUserId())
                            .transactionId(message.getTransactionId())
                            .transactionType(message.getType())
                            .build()
            );
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
}