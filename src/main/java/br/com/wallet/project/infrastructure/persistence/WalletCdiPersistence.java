package br.com.wallet.project.infrastructure.persistence;

import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.WalletCdi;

public interface WalletCdiPersistence {
        void save(WalletCdi wallet);
}