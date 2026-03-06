package br.com.wallet.project.infrastructure.persistence;

import br.com.wallet.project.domain.dto.WalletDTO;

public interface WalletRepository {
    WalletDTO findByUserId(String userId);
    void save(WalletDTO walletDTO);
}