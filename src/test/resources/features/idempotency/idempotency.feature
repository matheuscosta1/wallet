#language: pt
Feature: Idempotência - Deduplicação via Redis
  Como sistema de carteira
  Quero garantir que operações com o mesmo idempotencyId sejam processadas apenas uma vez
  Para evitar duplicações de saldo em reenvios ou falhas de rede

  Background:
    Given que o sistema está disponível

  Scenario: I-01 - Mesmo idempotencyId enviado duas vezes sequencialmente processa apenas uma vez
    Given que existe uma carteira para o usuário "user-i01"
    When eu realizo 2 depósitos de "100.00" para o usuário "user-i01" com o mesmo idempotencyId
    Then deve existir 1 transação para o usuário "user-i01" em até 10 segundos
    And o saldo da carteira de "user-i01" deve ser "100.00"

  Scenario: I-02 - Mesmo idempotencyId enviado 5 vezes processa exatamente 1 vez
    Given que existe uma carteira para o usuário "user-i02"
    When eu realizo 5 depósitos de "50.00" para o usuário "user-i02" com o mesmo idempotencyId
    Then deve existir 1 transação para o usuário "user-i02" em até 10 segundos
    And o saldo da carteira de "user-i02" deve ser "50.00"

  Scenario: I-03 - Dois depósitos com idempotencyIds diferentes são processados independentemente
    Given que existe uma carteira para o usuário "user-i03"
    When eu realizo 2 depósitos de "100.00" para o usuário "user-i03" com idempotencyIds diferentes
    Then deve existir 2 transações para o usuário "user-i03" em até 10 segundos
    And o saldo da carteira de "user-i03" deve ser "200.00"

  Scenario: I-04 - Mesmo idempotencyId disparado por 10 threads concorrentes processa apenas 1 vez
    Given que existe uma carteira para o usuário "user-i04"
    When 10 threads enviam simultaneamente o mesmo depósito de "100.00" para "user-i04"
    Then deve existir 1 transação para o usuário "user-i04" em até 10 segundos
    And o saldo da carteira de "user-i04" deve ser "100.00"

  Scenario: I-05 - Mesmo idempotencyId para transferência enviado duas vezes processa apenas uma vez
    Given que existe uma carteira para o usuário "user-i05-a"
    And que existe uma carteira para o usuário "user-i05-b"
    And o usuário "user-i05-a" tem saldo de "100.00"
    When eu realizo 2 transferências de "40.00" de "user-i05-a" para "user-i05-b" com o mesmo idempotencyId
    Then deve existir 1 registro de transferência no banco em até 10 segundos
    And o saldo da carteira de "user-i05-a" deve ser "60.00"
    And o saldo da carteira de "user-i05-b" deve ser "40.00"

  Scenario: I-06 - Mesmo idempotencyId para saque enviado duas vezes processa apenas uma vez
    Given que existe uma carteira para o usuário "user-i06"
    And o usuário "user-i06" tem saldo de "100.00"
    When eu realizo 2 saques de "30.00" para o usuário "user-i06" com o mesmo idempotencyId
    Then deve existir 1 transação do tipo "WITHDRAW" para o usuário "user-i06" em até 10 segundos
    And o saldo da carteira de "user-i06" deve ser "70.00"

  Scenario: I-07 - Após expirar a chave Redis o mesmo idempotencyId é tratado como novo
    Given que existe uma carteira para o usuário "user-i07"
    When eu realizo um depósito de "50.00" para o usuário "user-i07" com idempotencyId fixo "6a2ff239-6936-41e8-9f1a-9caecd1fd0ca"
    Then deve existir 1 transação para o usuário "user-i07" em até 10 segundos
    When todas as chaves do Redis são apagadas
    And eu realizo um depósito de "50.00" para o usuário "user-i07" com idempotencyId fixo "6a2ff239-6936-41e8-9f1a-9caecd1fd0ca"
    Then deve existir 2 transações para o usuário "user-i07" em até 10 segundos
    And o saldo da carteira de "user-i07" deve ser "100.00"

  Scenario: I-08 - idempotencyId nulo no depósito retorna 400
    Given que existe uma carteira para o usuário "user-i08"
    When eu realizo um depósito de "100.00" para o usuário "user-i08" com idempotencyId nulo
    Then a resposta deve retornar status 400

  Scenario: I-09 - idempotencyId em branco no depósito retorna 400
    Given que existe uma carteira para o usuário "user-i09"
    When eu realizo um depósito de "100.00" para o usuário "user-i09" com idempotencyId "   "
    Then a resposta deve retornar status 400
