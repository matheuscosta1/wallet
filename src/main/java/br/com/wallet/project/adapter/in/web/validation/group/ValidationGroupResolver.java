package br.com.wallet.project.adapter.in.web.validation.group;
import br.com.wallet.project.domain.model.enums.TransactionType;
import java.util.Optional;
public class ValidationGroupResolver {
    private ValidationGroupResolver() {}
    public static Class<?> resolve(TransactionType type) {
        return determineTransfer(type)
            .or(() -> determineDeposit(type))
            .or(() -> determineWithdraw(type))
            .orElse(DefaultValidationGroup.class);
    }
    private static Optional<Class<?>> determineTransfer(TransactionType t) {
        return TransactionType.TRANSFER.equals(t) ? Optional.of(TransferValidationGroup.class) : Optional.empty();
    }
    private static Optional<Class<?>> determineDeposit(TransactionType t) {
        return TransactionType.DEPOSIT.equals(t) ? Optional.of(DepositValidationGroup.class) : Optional.empty();
    }
    private static Optional<Class<?>> determineWithdraw(TransactionType t) {
        return TransactionType.WITHDRAW.equals(t) ? Optional.of(WithdrawValidationGroup.class) : Optional.empty();
    }
}
