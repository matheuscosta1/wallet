package br.com.wallet.project.domain.exception;
public class WalletDomainException extends GatewayException {
    public WalletDomainException(String message, String code, int group) { super(message, code, group); }
    public WalletDomainException(String message, String code, int group, Throwable cause) { super(message, code, group, cause); }
}
