package br.com.wallet.project.adapter.in.web.response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder @Schema
public class WalletResponse {
    @Schema(example = "user-id-123") private String userId;
    @Schema(example = "0.00") private BigDecimal balance;
}
