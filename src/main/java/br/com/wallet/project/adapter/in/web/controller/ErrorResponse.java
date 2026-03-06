package br.com.wallet.project.adapter.in.web.controller;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ErrorResponse {
    private String code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("erros")
    private List<FieldErrorResponse> errors;
    public void addError(String field, String error) {
        if (this.errors == null) this.errors = new ArrayList<>();
        this.errors.add(FieldErrorResponse.builder().field(field).error(error).build());
    }
    public void addError(String error) {
        if (this.errors == null) this.errors = new ArrayList<>();
        this.errors.add(FieldErrorResponse.builder().error(error).build());
    }
}
