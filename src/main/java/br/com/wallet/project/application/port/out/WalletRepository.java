package br.com.wallet.project.application.port.out;
import br.com.wallet.project.domain.model.Wallet;
/**
 * Driven port: persistence contract for Wallet.
 * The application layer depends on this interface; infrastructure implements it.
 */
public interface WalletRepository {
    Wallet findByUserId(String userId);
    void save(Wallet wallet);
}
