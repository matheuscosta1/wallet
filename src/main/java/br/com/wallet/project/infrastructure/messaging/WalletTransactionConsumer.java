package br.com.wallet.project.infrastructure.messaging;

import br.com.wallet.project.domain.TransactionMessage;

public interface WalletTransactionConsumer {
    void listen(TransactionMessage message);
}