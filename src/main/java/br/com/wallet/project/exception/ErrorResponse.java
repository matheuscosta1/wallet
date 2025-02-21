package br.com.wallet.project.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
  private String code;
  private String message;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty("erros")
  @SuppressWarnings("squid:S1948")
  private List<FieldErrorResponse> errors;

  public void addError(final String field, final String error) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(FieldErrorResponse.builder().field(field).error(error).build());
  }

  public void addError(final String error) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(FieldErrorResponse.builder().error(error).build());
  }
}
