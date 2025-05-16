package br.com.wallet.project.controller.validation.strategy;

import br.com.wallet.project.controller.validation.DefaultValidationGroup;
import br.com.wallet.project.controller.validation.DepositValidationGroup;
import br.com.wallet.project.controller.validation.TransferValidationGroup;
import br.com.wallet.project.controller.validation.WithdrawValidationGroup;
import br.com.wallet.project.domain.TransactionType;

import java.util.Optional;

public class ValidationGroupResolver implements ValidationGroupStrategy{
    @Override
    public Class<?> determineValidationGroup(TransactionType transactionType) {
        return determineTransferGroup(transactionType)
                .or(() -> determineDepositGroup(transactionType))
                .or(() -> determineWithdrawGroup(transactionType))
                .orElse(DefaultValidationGroup.class);
    }

    private Optional<Class<?>> determineTransferGroup(TransactionType transactionType) {
        if(TransactionType.TRANSFER.equals(transactionType)) {
            return Optional.of(TransferValidationGroup.class);
        }
        return Optional.empty();
    }

    private Optional<Class<?>> determineDepositGroup(TransactionType transactionType) {
        if(TransactionType.DEPOSIT.equals(transactionType)) {
            return Optional.of(DepositValidationGroup.class);
        }
        return Optional.empty();
    }

    private Optional<Class<?>> determineWithdrawGroup(TransactionType transactionType) {
        if(TransactionType.WITHDRAW.equals(transactionType)) {
            return Optional.of(WithdrawValidationGroup.class);
        }
        return Optional.empty();
    }
}
