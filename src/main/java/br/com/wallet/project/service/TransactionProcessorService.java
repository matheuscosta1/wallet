package br.com.wallet.project.service;

import br.com.wallet.project.domain.TransactionType;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.model.Transaction;
import br.com.wallet.project.service.factory.TransactionStrategyFactory;
import br.com.wallet.project.service.strategy.TransactionStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class TransactionProcessorService {
    private final TransactionStrategyFactory transactionStrategyFactory;

    @Autowired
    public TransactionProcessorService(@Lazy TransactionStrategyFactory transactionStrategyFactory) {
        this.transactionStrategyFactory = transactionStrategyFactory;
    }

    public Transaction processTransaction(TransactionRequest transactionRequest, TransactionType transactionType) {
        TransactionStrategy strategy = transactionStrategyFactory.getStrategy(transactionType);
        return strategy.execute(transactionRequest);
    }
}

