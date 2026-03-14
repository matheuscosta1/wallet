package br.com.wallet.project.adapter.out.messaging.kafka.producer;
import br.com.wallet.project.application.port.out.TransactionEventPublisher;
import br.com.wallet.project.domain.model.TransactionMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
/** Driven adapter: implements TransactionEventPublisher port using Apache Kafka. */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {
    private static final String TOPIC = "wallet-transactions";
    private final KafkaTemplate<String, TransactionMessage> kafkaTemplate;
    @Override
    @Transactional("kafkaTransactionManager")
    public void publish(TransactionMessage message) {
        produceWithPartitionKey(message);
//        produceWithoutPartitionKey(message);
    }

    private void produceWithPartitionKey(TransactionMessage message) {
        String partitionKey = message.getUserId() != null
                ? message.getUserId()
                : message.getFromUserId();
        kafkaTemplate.executeInTransaction(ops -> {
            ops.send(TOPIC, partitionKey, message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish message transactionId={} error={} partitionKey={}",
                            message.getTransactionId(), ex.getMessage(), partitionKey);
                } else {
                    log.info("Message published transactionId={} type={} userId={} partition={} offset={}, partitionKey={}",
                            message.getTransactionId(),
                            message.getType(),
                            message.getUserId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset(),
                            partitionKey);
                }
            });
            return true;
        });
    }

    private void produceWithoutPartitionKey(TransactionMessage message) {
        kafkaTemplate.executeInTransaction(ops -> {
            ops.send(TOPIC, message).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish message transactionId={} error={}",
                            message.getTransactionId(), ex.getMessage());
                } else {
                    log.info("Message published transactionId={} type={} userId={} partition={} offset={}",
                            message.getTransactionId(),
                            message.getType(),
                            message.getUserId(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            });
            return true;
        });
    }
}
