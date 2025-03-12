package br.com.wallet.project.infrastructure.messaging;

import br.com.wallet.project.domain.TransactionMessage;

public interface WalletTransactionProducer {
    void sendMessage(TransactionMessage message);
}