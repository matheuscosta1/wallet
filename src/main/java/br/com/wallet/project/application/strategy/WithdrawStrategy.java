package br.com.wallet.project.application.strategy;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.application.port.in.WalletUseCase;
import br.com.wallet.project.application.port.out.TransactionRepository;
import br.com.wallet.project.application.port.out.WalletRepository;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.model.enums.TransactionType;
import br.com.wallet.project.shared.mapper.TransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
@Component
@Slf4j
@RequiredArgsConstructor
public class WithdrawStrategy implements TransactionStrategy {
    private final WalletUseCase walletUseCase;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    @Override
    public Transaction execute(TransactionCommand command) {
        log.info("Withdraw for user {} transaction {}", command.getUserId(), command.getTransactionId());
        Wallet wallet = walletUseCase.validateWallet(command.getUserId(), command.getTransactionId());
        BigDecimal balanceBefore = wallet.getBalance();
        wallet.withdraw(command.getAmount(), command.getTransactionId());
        walletRepository.save(wallet);
        Transaction tx = transactionRepository.save(
            TransactionMapper.fromCommand(command, wallet, TransactionType.WITHDRAW, balanceBefore, wallet.getBalance()));
        log.info("Withdraw completed for user {} transaction {}", command.getUserId(), command.getTransactionId());
        return tx;
    }
}
