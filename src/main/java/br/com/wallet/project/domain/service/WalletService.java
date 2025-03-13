package br.com.wallet.project.domain.service;

import br.com.wallet.project.controller.request.HistoryTransactionRequest;
import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.controller.response.WalletResponse;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.mapper.TransactionHistoryMapper;
import br.com.wallet.project.mapper.WalletMapper;
import br.com.wallet.project.infrastructure.persistence.TransactionPersistence;
import br.com.wallet.project.infrastructure.persistence.WalletPersistence;
import br.com.wallet.project.domain.model.Transaction;
import br.com.wallet.project.domain.model.Wallet;
import br.com.wallet.project.domain.enums.WalletErrors;
import br.com.wallet.project.exception.WalletException;
import br.com.wallet.project.util.MoneyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WalletService extends WalletValidationService {
    private final WalletPersistence walletPersistence;
    private final TransactionPersistence transactionPersistence;
    private final TransactionProcessorService transactionProcessorService;

    public WalletService(WalletPersistence walletPersistence, TransactionPersistence transactionPersistence, TransactionProcessorService transactionProcessorService) {
        super(walletPersistence);
        this.walletPersistence = walletPersistence;
        this.transactionPersistence = transactionPersistence;
        this.transactionProcessorService = transactionProcessorService;
    }

    @Transactional("transactionManager")
    public WalletResponse createWallet(WalletRequest walletRequest) {
        Wallet wallet = walletPersistence.findByUserId(walletRequest.getUserId());
        if(wallet != null) {
            throw new WalletException(
                    MessageFormat.format(
                            WalletErrors.W0001.message(), walletRequest.getUserId()),
                    WalletErrors.W0001.name(),
                    WalletErrors.W0001.group());
        }
        Wallet newWallet = WalletMapper.mapWalletRequestIntoWalletEntity(walletRequest);
        walletPersistence.save(newWallet);
        return WalletResponse
                .builder()
                .balance(MoneyUtil.format(newWallet.getBalance()))
                .userId(walletRequest.getUserId())
                .build();
    }

    @Transactional("transactionManager")
    public WalletResponse retrieveBalance(WalletRequest walletRequest) {
        Wallet wallet = validateWallet(walletRequest.getUserId());
        return WalletResponse
                .builder()
                .balance(MoneyUtil.format(wallet.getBalance()))
                .userId(walletRequest.getUserId())
                .build();
    }

    @Transactional("transactionManager")
    public List<TransactionHistoryResponse> retrieveBalanceHistory(HistoryTransactionRequest walletRequest) {
        validateWallet(walletRequest.getUserId());
        LocalDateTime startOfDay = LocalDateTime.of(walletRequest.getDate().toLocalDate(), LocalTime.MIDNIGHT);
        LocalDateTime endOfDay = LocalDateTime.of(walletRequest.getDate().toLocalDate(), LocalTime.MAX);

        List<Transaction> transactions = transactionPersistence.findTransactionsByDateAndUserId(startOfDay, endOfDay, walletRequest.getUserId());

        return transactions.stream()
                .map(TransactionHistoryMapper::mapToTransactionHistoryResponse)
                .collect(Collectors.toList());
    }

    @Transactional("transactionManager")
    public void transactionProcessor(TransactionRequest transactionRequest) {
        log.info("Transaction operation service for user id: {} and transaction id {} started to process",
                transactionRequest.getUserId(), transactionRequest.getTransactionId());
        transactionProcessorService.processTransaction(transactionRequest, transactionRequest.getTransactionType());
        log.info("Transaction processed successfully for user id: {} and transaction id {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
    }
}