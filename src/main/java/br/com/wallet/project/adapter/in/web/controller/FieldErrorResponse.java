package br.com.wallet.project.adapter.in.web.controller;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.io.Serializable;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class FieldErrorResponse implements Serializable {
    private static final long serialVersionUID = 1L;
    @JsonInclude(JsonInclude.Include.NON_NULL) private String field;
    @JsonInclude(JsonInclude.Include.NON_NULL) private String error;
}
