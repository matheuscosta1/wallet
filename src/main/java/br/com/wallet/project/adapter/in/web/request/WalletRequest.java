package br.com.wallet.project.adapter.in.web.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
@Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder @Schema
public class WalletRequest {
    @Schema(example = "user-account-id", description = "Wallet user identifier.")
    @NotBlank
    private String userId;
}
