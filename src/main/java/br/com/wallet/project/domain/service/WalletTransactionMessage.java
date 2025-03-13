package br.com.wallet.project.domain.service;

import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.domain.request.TransactionRequest;

import java.util.UUID;

public interface WalletTransactionMessage {
    TransactionResponse processTransactionMessage(TransactionRequest transactionOperationRequest, UUID transactionId);
}
