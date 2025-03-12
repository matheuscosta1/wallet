package br.com.wallet.project.domain.service;

import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.domain.request.TransactionRequest;

import java.util.UUID;

public interface WalletTransaction {
    TransactionResponse processDeposit(TransactionRequest transactionOperationRequest, UUID transactionId);
    TransactionResponse processWithdraw(TransactionRequest transactionOperationRequest, UUID transactionId);
    TransactionResponse processTransfer(TransactionRequest transactionOperationRequest, UUID transactionId);
}
