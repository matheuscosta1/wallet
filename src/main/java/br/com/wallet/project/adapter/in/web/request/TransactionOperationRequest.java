package br.com.wallet.project.adapter.in.web.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder @Schema
public class TransactionOperationRequest {
    @Schema(example = "user-account-id", description = "Wallet user identifier.")
    @NotBlank
    private String userId;
    @Schema(example = "10.0", description = "Transaction amount.")
    @NotNull
    private BigDecimal amount;
    @Schema(example = "550e8400-e29b-41d4-a716-446655440000", description = "Unique idempotency key.")
    @NotNull
    private UUID idempotencyId;
}
