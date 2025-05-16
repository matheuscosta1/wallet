package br.com.wallet.project.controller.validation.strategy;

import br.com.wallet.project.domain.TransactionType;

public interface ValidationGroupStrategy {
    Class<?> determineValidationGroup(TransactionType transactionType);
}
