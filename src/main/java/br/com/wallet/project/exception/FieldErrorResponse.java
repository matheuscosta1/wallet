package br.com.wallet.project.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class FieldErrorResponse implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String field;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String error;
}
