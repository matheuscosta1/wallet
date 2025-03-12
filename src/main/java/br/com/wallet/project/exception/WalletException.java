package br.com.wallet.project.exception;


public class WalletException extends GatewayException {

  public WalletException(String message, String code, int group) {
    super(message, code, group);
  }

  public WalletException(String message, String code, int group, Throwable cause) {
    super(message, code, group, cause);
  }
}
