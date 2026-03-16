package br.com.wallet.project.adapter.in.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class CdiRequest {

    @Schema(example = "user-account-id", description = "Wallet user identifier.")
    @NotBlank
    private String userId;

    @Schema(
        example = "0.000456",
        description = "Daily CDI rate to apply (e.g. 0.000456 = 0.0456% per day, equivalent to ~14% per year)."
    )
    @NotNull
    @DecimalMin(value = "0.000001", message = "CDI rate must be greater than zero.")
    @DecimalMax(value = "1.0", message = "CDI rate must be a valid daily rate (max 100%).")
    private BigDecimal cdiRate;
}
