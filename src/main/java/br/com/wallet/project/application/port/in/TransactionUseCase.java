package br.com.wallet.project.application.port.in;
import br.com.wallet.project.adapter.in.web.response.TransactionResponse;
import br.com.wallet.project.application.command.TransactionCommand;
import java.util.UUID;
/** Driving port: transaction processing use cases. */
public interface TransactionUseCase {
    TransactionResponse publishTransaction(TransactionCommand command, UUID idempotencyId);
    void processTransaction(TransactionCommand command);
}
