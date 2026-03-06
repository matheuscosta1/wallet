package br.com.wallet.project.adapter.in.web.controller;
import br.com.wallet.project.domain.exception.GatewayException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    private static final String INVALID_REQUEST_MESSAGE = "Revise sua requisição";
    @ExceptionHandler(MethodArgumentNotValidException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        log.error(ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse();
        response.setMessage(INVALID_REQUEST_MESSAGE);
        for (FieldError error : ex.getBindingResult().getFieldErrors()) response.addError(error.getField(), error.getDefaultMessage());
        return response;
    }
    @ExceptionHandler(ConstraintViolationException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleConstraintViolation(ConstraintViolationException ex) {
        log.error(ex.getMessage(), ex);
        ErrorResponse response = ErrorResponse.builder().message(INVALID_REQUEST_MESSAGE).build();
        ex.getConstraintViolations().forEach(v -> response.addError(v.getMessage()));
        return response;
    }
    @ExceptionHandler(InvalidFormatException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidFormat(InvalidFormatException ex) {
        log.error(ex.getMessage(), ex);
        ErrorResponse response = ErrorResponse.builder().message(INVALID_REQUEST_MESSAGE).build();
        response.addError(ex.getMessage());
        return response;
    }
    @ExceptionHandler(HttpMessageNotReadableException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleNotReadable(HttpMessageNotReadableException ex) {
        log.error(ex.getMessage(), ex);
        ErrorResponse response = new ErrorResponse();
        response.setMessage("Revise o corpo da sua requisição");
        return response;
    }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class) @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.error(ex.getMessage(), ex);
        Set<HttpMethod> methods = ex.getSupportedHttpMethods();
        String supported = methods != null ? methods.stream().map(HttpMethod::name).collect(Collectors.joining("; ")) : "";
        ErrorResponse response = new ErrorResponse();
        response.setMessage(String.format("Método %s não suportado. Métodos suportados: %s", ex.getMethod(), supported));
        return response;
    }
    @ExceptionHandler(GatewayException.class) @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleGateway(GatewayException e) {
        return ErrorResponse.builder().message(e.getMessage()).code(e.getCode()).build();
    }
}
