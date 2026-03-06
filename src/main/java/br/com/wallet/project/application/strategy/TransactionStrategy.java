package br.com.wallet.project.application.strategy;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.domain.model.Transaction;
/** Strategy contract for executing a specific transaction type. */
public interface TransactionStrategy {
    Transaction execute(TransactionCommand command);
}
