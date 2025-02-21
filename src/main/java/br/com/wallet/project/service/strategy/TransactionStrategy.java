package br.com.wallet.project.service.strategy;

import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.model.Transaction;

public interface TransactionStrategy {
    Transaction execute(TransactionRequest transactionRequest);
}