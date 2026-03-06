package br.com.wallet.project.adapter.in.web.controller;
import br.com.wallet.project.adapter.in.web.request.*;
import br.com.wallet.project.adapter.in.web.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@Tag(name = "Wallet")
public interface WalletControllerApi {
    @Operation(summary = "Create wallet") @PostMapping("creation")
    ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody WalletRequest request);
    @Operation(summary = "Retrieve balance") @GetMapping("balance")
    ResponseEntity<WalletResponse> retrieveBalance(@Valid @RequestBody WalletRequest request);
    @Operation(summary = "Deposit funds") @PostMapping("deposit")
    ResponseEntity<TransactionResponse> deposit(@Valid @RequestBody TransactionOperationRequest request);
    @Operation(summary = "Withdraw funds") @PostMapping("withdraw")
    ResponseEntity<TransactionResponse> withdraw(@Valid @RequestBody TransactionOperationRequest request);
    @Operation(summary = "Transfer funds") @PostMapping("transfer")
    ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request);
    @Operation(summary = "Any operation") @PostMapping("any-operation")
    ResponseEntity<TransactionResponse> anyOperation(@Valid @RequestBody GenericTransactionRequest request);
    @Operation(summary = "Transaction history") @GetMapping("history")
    ResponseEntity<List<TransactionHistoryResponse>> history(@Valid @RequestBody HistoryTransactionRequest request);
}
