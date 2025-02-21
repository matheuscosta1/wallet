package br.com.wallet.project.exception;

public class GatewayException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String code;
  private final int group;

  public GatewayException(String message, String code, int group) {
    super(message);
    this.code = code;
    this.group = group;
  }

  public GatewayException(String message, String code, int group, Throwable cause) {
    this(message, code, group);
    initCause(cause);
  }

  public String getCode() {
    return code;
  }

  public int getGroup() {
    return group;
  }
}
