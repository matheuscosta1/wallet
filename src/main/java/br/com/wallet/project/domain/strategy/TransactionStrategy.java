package br.com.wallet.project.domain.strategy;

import br.com.wallet.project.domain.dto.TransactionDTO;
import br.com.wallet.project.domain.request.TransactionRequest;

public interface TransactionStrategy {
    TransactionDTO execute(TransactionRequest transactionRequest);
}