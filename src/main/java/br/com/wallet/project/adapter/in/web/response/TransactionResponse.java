package br.com.wallet.project.adapter.in.web.response;
import br.com.wallet.project.domain.model.enums.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder @Schema
public class TransactionResponse {
    @Schema(example = "user-id-123") private String userId;
    @Schema(example = "0000-0000-0000-0000") private UUID transactionId;
    @Schema(example = "DEPOSIT") private TransactionType transactionType;
    @Schema(example = "10.0") private BigDecimal amount;
}
