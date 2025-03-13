package br.com.wallet.project.infrastructure.persistence;

import br.com.wallet.project.domain.model.Wallet;

public interface WalletPersistence {
    Wallet findByUserId(String userId);
    void save(Wallet wallet);
}