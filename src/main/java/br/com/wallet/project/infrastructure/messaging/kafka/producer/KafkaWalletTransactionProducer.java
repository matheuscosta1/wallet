package br.com.wallet.project.infrastructure.messaging.kafka.producer;

import br.com.wallet.project.domain.TransactionMessage;
import br.com.wallet.project.infrastructure.messaging.WalletTransactionProducer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class KafkaWalletTransactionProducer implements WalletTransactionProducer {
    public static final String TOPIC = "wallet-transactions";
    private final KafkaTemplate<String, TransactionMessage> kafkaTemplate;

    public KafkaWalletTransactionProducer(KafkaTemplate<String, TransactionMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional("kafkaTransactionManager")
    public void sendMessage(TransactionMessage message) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send(TOPIC, message);
            return true;
        });
    }
}