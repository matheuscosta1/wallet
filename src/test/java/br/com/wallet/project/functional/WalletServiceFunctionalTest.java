package br.com.wallet.project.functional;

import br.com.wallet.project.adapter.in.web.request.WalletRequest;
import br.com.wallet.project.adapter.out.persistence.entity.WalletEntity;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaTransactionRepository;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaWalletRepository;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.application.port.in.TransactionUseCase;
import br.com.wallet.project.application.port.in.WalletUseCase;
import br.com.wallet.project.domain.model.enums.TransactionType;
import br.com.wallet.project.shared.util.MoneyUtil;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class WalletServiceFunctionalTest implements TestContainerSetup {

    private final WalletUseCase walletUseCase;
    private final TransactionUseCase transactionUseCase;
    private final JpaWalletRepository jpaWalletRepository;
    private final JpaTransactionRepository jpaTransactionRepository;

    @Test
    @Transactional("transactionManager")
    void testCreateWallet() {
        String userId = UUID.randomUUID().toString();
        walletUseCase.createWallet(new WalletRequest(userId));
        assertNotNull(jpaWalletRepository.findByUserId(userId));
    }

    @Test
    @Transactional("transactionManager")
    void testDepositFundsForUser() throws InterruptedException {
        String userId = UUID.randomUUID().toString();
        UUID transactionId = UUID.randomUUID();
        UUID idempotencyId = UUID.randomUUID();

        walletUseCase.createWallet(new WalletRequest(userId));
        TransactionCommand command = buildCommand(userId, transactionId, BigDecimal.valueOf(100.0), TransactionType.DEPOSIT, null, null, idempotencyId);
        transactionUseCase.processTransaction(command);

        Thread.sleep(2000);
        WalletEntity wallet = jpaWalletRepository.findByUserId(userId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(100.0)), wallet.getBalance());
        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionId));
    }

    private static TransactionCommand buildCommand(String userId, UUID transactionId, BigDecimal amount,
                                                    TransactionType type, String fromUserId, String toUserId, UUID idempotencyId) {
        return TransactionCommand.builder()
                .userId(userId).transactionId(transactionId).amount(amount)
                .transactionType(type).fromUserWalletId(fromUserId).toUserWalletId(toUserId)
                .idempotencyId(idempotencyId).build();
    }
}
