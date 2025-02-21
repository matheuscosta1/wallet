package br.com.wallet.project.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ExceptionHandlingController {

  private static final String INVALID_REQUEST_MESSAGE = "Revise sua requisição";

  private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandlingController.class);

  @ExceptionHandler({MethodArgumentNotValidException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleCustomFieldsValidationException(
          final MethodArgumentNotValidException exception) {
    LOGGER.error(exception.getMessage(), exception);
    final ErrorResponse response = new ErrorResponse();
    response.setMessage(INVALID_REQUEST_MESSAGE);
    for (FieldError error : exception.getBindingResult().getFieldErrors()) {
      response.addError(error.getField(), error.getDefaultMessage());
    }
    return response;
  }

  @ExceptionHandler({InvalidFormatException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleParseException(final InvalidFormatException exception) {
    LOGGER.error(exception.getMessage(), exception);
    final ErrorResponse errorResponse =
            ErrorResponse.builder().message(INVALID_REQUEST_MESSAGE).build();

    errorResponse.addError(exception.getMessage());

    return errorResponse;
  }

  @ExceptionHandler({IllegalArgumentException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleIllegalArgumentException(final IllegalArgumentException exception) {
    LOGGER.error(exception.getMessage(), exception);
    final ErrorResponse errorResponse =
            ErrorResponse.builder().message(INVALID_REQUEST_MESSAGE).build();

    errorResponse.addError(exception.getMessage());

    return errorResponse;
  }

  @ExceptionHandler({MissingServletRequestParameterException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleMissingServletRequestParameterException(
          final MissingServletRequestParameterException exception) {
    LOGGER.error(exception.getMessage(), exception);

    final ErrorResponse errorResponse =
            ErrorResponse.builder().message(INVALID_REQUEST_MESSAGE).build();

    final String errorMessagePretty =
            String.format("O campo %s deve ser informado.", exception.getParameterName());

    errorResponse.addError(exception.getParameterName(), errorMessagePretty);

    return errorResponse;
  }

  @ExceptionHandler({HttpMessageNotReadableException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleHttpMessageNotReadableException(
          final HttpMessageNotReadableException exception) {
    LOGGER.error(exception.getMessage(), exception);
    final ErrorResponse response = new ErrorResponse();
    response.setMessage("Revise o corpo da sua requisição");
    return response;
  }

  @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleHttpRequestMethodNotSupportedException(
          final HttpRequestMethodNotSupportedException exception) {
    LOGGER.error(exception.getMessage(), exception);
    final ErrorResponse response = new ErrorResponse();
    Set<HttpMethod> supportedHttpMethods = exception.getSupportedHttpMethods();
    response.setMessage(
            String.format(
                    "Método %s não suportado. Métodos suportados: %s",
                    exception.getMethod(),
                    supportedHttpMethods != null
                            ? supportedHttpMethods.stream()
                            .map(HttpMethod::name)
                            .collect(Collectors.joining("; "))
                            : ""));
    return response;
  }

  @ExceptionHandler({ConstraintViolationException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleConstraintViolationException(ConstraintViolationException exception) {
    LOGGER.error(exception.getMessage(), exception);
    final ErrorResponse errorResponse =
            ErrorResponse.builder().message(INVALID_REQUEST_MESSAGE).build();

    exception
            .getConstraintViolations()
            .forEach(error -> errorResponse.addError(error.getMessage()));

    return errorResponse;
  }

  @ExceptionHandler({GatewayException.class})
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ErrorResponse gatewayException(final GatewayException e) {
    return ErrorResponse.builder().message(e.getMessage()).code(e.getCode()).build();
  }
}
