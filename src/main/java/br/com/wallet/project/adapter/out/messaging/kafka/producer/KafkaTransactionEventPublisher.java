package br.com.wallet.project.adapter.out.messaging.kafka.producer;
import br.com.wallet.project.application.port.out.TransactionEventPublisher;
import br.com.wallet.project.domain.model.TransactionMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
/** Driven adapter: implements TransactionEventPublisher port using Apache Kafka. */
@Component
@RequiredArgsConstructor
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {
    private static final String TOPIC = "wallet-transactions";
    private final KafkaTemplate<String, TransactionMessage> kafkaTemplate;
    @Override
    @Transactional("kafkaTransactionManager")
    public void publish(TransactionMessage message) {
        kafkaTemplate.executeInTransaction(ops -> { ops.send(TOPIC, message); return true; });
    }
}
