package br.com.wallet.project.adapter.in.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class CdiResponse {

    @Schema(example = "user-id-123")
    private String userId;

    @Schema(example = "1000.00", description = "Wallet balance before CDI yield.")
    private BigDecimal balanceBeforeCdi;

    @Schema(example = "1000.46", description = "Wallet balance after CDI yield.")
    private BigDecimal balanceAfterCdi;

    @Schema(example = "0.000456", description = "Daily CDI rate applied.")
    private BigDecimal cdiRate;

    @Schema(example = "0.46", description = "Yield amount credited to the wallet.")
    private BigDecimal yieldAmount;

    @Schema(example = "2025-01-01T00:00:00", description = "Timestamp of the CDI calculation.")
    private LocalDateTime calculatedAt;
}
