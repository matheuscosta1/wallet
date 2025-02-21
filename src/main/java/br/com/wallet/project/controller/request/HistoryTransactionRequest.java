package br.com.wallet.project.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema
public class HistoryTransactionRequest {

  @Schema(example = "user-account-id", description = "Wallet user id.")
  @NotBlank
  private String userId;

  @Schema(example = "2025-02-21 00:00:00.000", description = "History transaction date.")
  @NotNull
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
  private LocalDateTime date;
}
