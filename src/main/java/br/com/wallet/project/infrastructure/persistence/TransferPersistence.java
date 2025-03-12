package br.com.wallet.project.infrastructure.persistence;

import br.com.wallet.project.domain.model.Transfer;

public interface TransferPersistence {
    void save(Transfer transfer);
}