package br.com.wallet.project.service.functional;

import br.com.wallet.project.adapter.in.web.request.WalletRequest;
import br.com.wallet.project.adapter.out.persistence.entity.WalletEntity;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaTransactionRepository;
import br.com.wallet.project.adapter.out.persistence.jpa.JpaWalletRepository;
import br.com.wallet.project.application.command.TransactionCommand;
import br.com.wallet.project.application.service.TransactionService;
import br.com.wallet.project.application.service.WalletService;
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
class WalletEntityServiceTest implements TestContainerSetup {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final JpaWalletRepository jpaWalletRepository;
    private final JpaTransactionRepository jpaTransactionRepository;

    @Test
    @Transactional("transactionManager")
    void testCreateWallet() {
        String userId = UUID.randomUUID().toString();
        walletService.createWallet(buildWalletRequest(userId));
        assertNotNull(jpaWalletRepository.findByUserId(userId));
    }

    @Test
    @Transactional("transactionManager")
    void testDepositFundsForUser() throws InterruptedException {
        String userId = UUID.randomUUID().toString();
        UUID transactionId = UUID.randomUUID();
        UUID idempotencyId = UUID.randomUUID();
        BigDecimal funds = BigDecimal.valueOf(100.0);
        walletService.createWallet(buildWalletRequest(userId));
        TransactionCommand transactionRequest =
                buildTransactionCommand(
                        userId,
                        transactionId,
                        funds,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        idempotencyId);
        transactionService.processTransaction(transactionRequest);
        Thread.sleep(2000);
        WalletEntity walletEntityAfterOperation = jpaWalletRepository.findByUserId(userId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(100.0)), walletEntityAfterOperation.getBalance());
        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionId));
    }

    @Test
    @Transactional("transactionManager")
    void shouldWithdrawFundsAfterDeposit() throws InterruptedException {
        String userId = UUID.randomUUID().toString();
        UUID transactionDepositId = UUID.randomUUID();
        UUID transactionWithdrawId = UUID.randomUUID();
        UUID transactionDepositRequestIdempotencyId = UUID.randomUUID();
        UUID transactionWithdrawRequestIdempotencyId = UUID.randomUUID();
        BigDecimal funds = BigDecimal.valueOf(100.0);

        walletService.createWallet(buildWalletRequest(userId));

        TransactionCommand transactionDepositRequest =
                buildTransactionCommand(
                        userId,
                        transactionDepositId,
                        funds,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        transactionDepositRequestIdempotencyId);
        transactionService.processTransaction(transactionDepositRequest);

        TransactionCommand transactionWithdrawRequest =
                buildTransactionCommand(
                        userId,
                        transactionWithdrawId,
                        BigDecimal.TEN,
                        TransactionType.WITHDRAW,
                        null,
                        null,
                        transactionWithdrawRequestIdempotencyId);
        transactionService.processTransaction(transactionWithdrawRequest);

        Thread.sleep(2000);
        WalletEntity walletEntityAfterOperation = jpaWalletRepository.findByUserId(userId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(90.0)), walletEntityAfterOperation.getBalance());
        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionDepositId));
        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionWithdrawId));
    }

    @Test
    @Transactional("transactionManager")
    void shouldTransferFromWalletToAnother() throws InterruptedException {
        String fromUserId = UUID.randomUUID().toString();
        String toUserId = UUID.randomUUID().toString();
        UUID transactionDepositId = UUID.randomUUID();
        UUID transferTransactionId = UUID.randomUUID();
        UUID transactionDepositRequestIdempotencyId = UUID.randomUUID();
        UUID transferRequestIdempotencyId = UUID.randomUUID();
        BigDecimal funds = BigDecimal.valueOf(100.0);

        walletService.createWallet(buildWalletRequest(fromUserId));
        walletService.createWallet(buildWalletRequest(toUserId));

        TransactionCommand transactionDepositRequest =
                buildTransactionCommand(
                        fromUserId,
                        transactionDepositId,
                        funds,
                        TransactionType.DEPOSIT,
                        null,
                        null,
                        transactionDepositRequestIdempotencyId);
        transactionService.processTransaction(transactionDepositRequest);

        TransactionCommand transferRequest =
                buildTransactionCommand(
                        null,
                        transferTransactionId,
                        BigDecimal.valueOf(10),
                        TransactionType.TRANSFER,
                        fromUserId,
                        toUserId,
                        transferRequestIdempotencyId);
        transactionService.processTransaction(transferRequest);

        Thread.sleep(2000);

        WalletEntity walletEntityAfterOperationFromUserId = jpaWalletRepository.findByUserId(fromUserId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(90.00)), walletEntityAfterOperationFromUserId.getBalance());

        WalletEntity walletEntityAfterOperationToUserId = jpaWalletRepository.findByUserId(toUserId);
        assertEquals(MoneyUtil.format(BigDecimal.valueOf(10.00)), walletEntityAfterOperationToUserId.getBalance());

        assertNotNull(jpaTransactionRepository.findByTransactionTrackId(transactionDepositId));
        assertEquals(2, jpaTransactionRepository.findByTransactionTrackId(transferTransactionId).size());
    }

    private static WalletRequest buildWalletRequest(String userId) {
        return new WalletRequest(userId);
    }

    private static TransactionCommand buildTransactionCommand(
            String userId,
            UUID transactionId,
            BigDecimal amount,
            TransactionType transactionType,
            String fromUserWalletId,
            String toUserWalletId,
            UUID idempotencyId
    ) {
        return TransactionCommand
                .builder()
                .userId(userId)
                .transactionId(transactionId)
                .amount(amount)
                .transactionType(transactionType)
                .fromUserWalletId(fromUserWalletId)
                .toUserWalletId(toUserWalletId)
                .idempotencyId(idempotencyId)
                .build();
    }

}

