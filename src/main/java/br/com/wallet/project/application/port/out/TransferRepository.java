package br.com.wallet.project.application.port.out;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * Driven port: persistence contract for Transfer records.
 */
public interface TransferRepository {
    void save(Wallet fromWallet, Wallet toWallet,
              Transaction debitTransaction, Transaction creditTransaction,
              BigDecimal amount, LocalDateTime timestamp);
}
