package br.com.wallet.project.service;

import br.com.wallet.project.domain.TransactionMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WalletTransactionProducerService {

    private final KafkaTemplate<String, TransactionMessage> kafkaTemplate;

    public WalletTransactionProducerService(KafkaTemplate<String, TransactionMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendMessage(TransactionMessage message) {
        kafkaTemplate.executeInTransaction(operations -> {
            operations.send("wallet-transactions", message);
            return true;
        });
    }
}