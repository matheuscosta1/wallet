package br.com.wallet.project.application.port.out;

import br.com.wallet.project.domain.model.WalletCdi;

import java.util.List;

/**
 * Driven port: persistence contract for WalletCdi records.
 * Stores and retrieves CDI yield calculation history per wallet.
 */
public interface WalletCdiRepository {

    /**
     * Persists a CDI yield calculation record.
     */
    WalletCdi save(WalletCdi walletCdi);

    /**
     * Returns the full CDI calculation history for the given userId.
     */
    List<WalletCdi> findByUserId(String userId);
}
