package br.com.wallet.project.application.port.out;
import br.com.wallet.project.domain.model.TransactionMessage;
/**
 * Driven port: messaging contract for publishing transaction domain events.
 * Decouples application layer from Kafka or any other broker.
 */
public interface TransactionEventPublisher {
    void publish(TransactionMessage message);
}
