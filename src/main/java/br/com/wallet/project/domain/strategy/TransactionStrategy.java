package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.domain.model.Transaction;

public interface TransactionStrategy {
    Transaction execute(TransactionRequest transactionRequest);
}