package br.com.wallet.project.service;

import br.com.wallet.project.controller.request.HistoryTransactionRequest;
import br.com.wallet.project.controller.request.WalletRequest;
import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.controller.response.WalletResponse;
import br.com.wallet.project.domain.request.TransactionRequest;
import br.com.wallet.project.repositoy.TransactionRepository;
import br.com.wallet.project.repositoy.WalletRepository;
import br.com.wallet.project.repositoy.model.Transaction;
import br.com.wallet.project.repositoy.model.Wallet;
import br.com.wallet.project.service.enums.WalletErrors;
import br.com.wallet.project.service.exception.WalletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class WalletService extends AbstractWalletService {
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionProcessorService transactionProcessorService;

    @Autowired
    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository, TransactionProcessorService transactionProcessorService) {
        super(walletRepository);
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.transactionProcessorService = transactionProcessorService;
    }

    @Transactional("transactionManager")
    public WalletResponse createWallet(WalletRequest walletRequest) {
        Wallet wallet = walletRepository.findByUserId(walletRequest.getUserId());
        if(wallet != null) {
            throw new WalletException(
                    MessageFormat.format(
                            WalletErrors.W0001.message(), walletRequest.getUserId()),
                    WalletErrors.W0001.name(),
                    WalletErrors.W0001.group());
        }
        Wallet newWallet = buildWalletEntity(walletRequest);
        walletRepository.save(newWallet);
        return WalletResponse.builder().balance(newWallet.getBalance().setScale(2, RoundingMode.HALF_DOWN)).userId(walletRequest.getUserId()).build();
    }

    @Transactional("transactionManager")
    public WalletResponse retrieveBalance(WalletRequest walletRequest) {
        Wallet wallet = validateWallet(walletRequest.getUserId());
        return WalletResponse
                .builder()
                .balance(wallet.getBalance().setScale(2, RoundingMode.HALF_DOWN))
                .userId(walletRequest.getUserId())
                .build();
    }

    @Transactional("transactionManager")
    public List<TransactionHistoryResponse> balanceHistory(HistoryTransactionRequest walletRequest) {
        validateWallet(walletRequest.getUserId());
        LocalDateTime startOfDay = LocalDateTime.of(walletRequest.getDate().toLocalDate(), LocalTime.MIDNIGHT);
        LocalDateTime endOfDay = LocalDateTime.of(walletRequest.getDate().toLocalDate(), LocalTime.MAX);

        List<Transaction> transactions = transactionRepository.findTransactionsByDateAndUserId(startOfDay, endOfDay, walletRequest.getUserId());

        return transactions.stream()
                .map(this::mapToTransactionHistoryResponse)
                .collect(Collectors.toList());
    }

    private TransactionHistoryResponse mapToTransactionHistoryResponse(Transaction transaction) {
        return TransactionHistoryResponse.builder()
                .userId(transaction.getWallet().getUserId())
                .transactionId(transaction.getTransactionTrackId())
                .transactionType(transaction.getType())
                .amount(transaction.getAmount().setScale(2,  RoundingMode.DOWN))
                .date(transaction.getTimestamp())
                .balanceBeforeTransaction(transaction.getBalanceBeforeTransaction().setScale(2,  RoundingMode.DOWN))
                .balanceAfterTransaction(transaction.getBalanceAfterTransaction().setScale(2,  RoundingMode.DOWN))
                .build();
    }

    @Transactional("transactionManager")
    public void transactionOperation(TransactionRequest transactionRequest) {
        log.info("Transaction operation service for user id: {} and transaction id {} started to process",
                transactionRequest.getUserId(), transactionRequest.getTransactionId());
        transactionProcessorService.processTransaction(transactionRequest, transactionRequest.getTransactionType());
        log.info("Transaction processed successfully for user id: {} and transaction id {}", transactionRequest.getUserId(), transactionRequest.getTransactionId());
    }

    private Wallet buildWalletEntity(WalletRequest walletRequest) {
        return Wallet.builder().balance(BigDecimal.ZERO).userId(walletRequest.getUserId()).build();
    }
}