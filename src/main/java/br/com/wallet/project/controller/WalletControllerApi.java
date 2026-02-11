package br.com.wallet.project.controller;

import br.com.wallet.project.controller.request.*;
import br.com.wallet.project.controller.response.TransactionHistoryResponse;
import br.com.wallet.project.controller.response.TransactionResponse;
import br.com.wallet.project.controller.response.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Wallet")
public interface WalletControllerApi {

    @Operation(summary = "Create wallet", description = "Create wallet for user")
    @PostMapping("creation")
    ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody WalletRequest walletRequest);

    @Operation(summary = "Wallet balance funds", description = "Retrieve wallet balance")
    @GetMapping("balance")
    ResponseEntity<WalletResponse> retrieveBalance(@Valid @RequestBody WalletRequest walletRequest);

    @Operation(summary = "Wallet deposit funds", description = "Deposit funds into wallet")
    @PostMapping("deposit")
    ResponseEntity<TransactionResponse> depositFunds(@Valid @RequestBody TransactionOperationRequest transactionOperationRequest);

    @Operation(summary = "Wallet withdraw funds", description = "Withdraw funds from wallet")
    @PostMapping("withdraw")
    ResponseEntity<TransactionResponse> withdrawFunds(@Valid @RequestBody TransactionOperationRequest transactionOperationRequest);

    @Operation(summary = "Wallet transfer funds", description = "Transfer funds from an user to another")
    @PostMapping("transfer")
    ResponseEntity<TransactionResponse> transferFunds(@Valid @RequestBody TransferRequest transactionOperationRequest);

    @Operation(summary = "Wallet any operation funds", description = "Any operation")
    @PostMapping("any-operation")
    ResponseEntity<TransactionResponse> anyOperation(@Valid @RequestBody GenericRequest transactionOperationRequest);

    @Operation(summary = "Wallet transaction history", description = "Transaction history by specified date")
    @GetMapping("history")
    ResponseEntity<List<TransactionHistoryResponse>> history(@Valid @RequestBody HistoryTransactionRequest historyTransactionRequest);
}
