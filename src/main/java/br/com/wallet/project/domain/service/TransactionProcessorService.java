package br.com.wallet.project.domain.service;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.factory.TransactionStrategyFactory;
import br.com.wallet.project.domain.strategy.TransactionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class TransactionProcessorService {
    private final TransactionStrategyFactory transactionStrategyFactory;

    public TransactionProcessorService(@Lazy TransactionStrategyFactory transactionStrategyFactory) {
        this.transactionStrategyFactory = transactionStrategyFactory;
    }

    public Transaction processTransaction(TransactionRequest transactionRequest, TransactionType transactionType) {
        TransactionStrategy strategy = transactionStrategyFactory.getStrategy(transactionType);
        return strategy.execute(transactionRequest);
    }
}

