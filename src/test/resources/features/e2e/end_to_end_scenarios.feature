#language: pt
Feature: Cenários End-to-End
  Como usuário do sistema de carteira
  Quero executar jornadas completas de uso
  Para garantir que o sistema funcione de ponta a ponta em cenários reais

  Background:
    Given que o sistema está disponível

  Scenario: E-01 - Ciclo de vida completo: criar, depositar, sacar, transferir e consultar histórico
    Given que existe uma carteira para o usuário "user-e01-alice"
    And que existe uma carteira para o usuário "user-e01-bob"
    When o usuário "user-e01-alice" deposita "500.00"
    Then o saldo da carteira de "user-e01-alice" deve ser "500.00"
    When o usuário "user-e01-alice" saca "100.00"
    Then deve existir 1 transação do tipo "WITHDRAW" para o usuário "user-e01-alice" em até 15 segundos
    And o saldo da carteira de "user-e01-alice" deve ser "400.00"
    When eu transfiro "150.00" de "user-e01-alice" para "user-e01-bob"
    Then deve existir 1 registro de transferência no banco em até 15 segundos
    And o saldo da carteira de "user-e01-alice" deve ser "250.00"
    And o saldo da carteira de "user-e01-bob" deve ser "150.00"
    When eu consulto o histórico do usuário "user-e01-alice" para a data de hoje
    Then a resposta deve conter uma lista com 3 transações

  Scenario: E-02 - Pagamento a lojista: usuário paga 30.00 e lojista recebe exatamente 30.00
    Given que existe uma carteira para o usuário "user-e02-customer"
    And que existe uma carteira para o usuário "user-e02-merchant"
    And o usuário "user-e02-customer" tem saldo de "100.00"
    When eu transfiro "30.00" de "user-e02-customer" para "user-e02-merchant"
    Then deve existir 1 registro de transferência no banco em até 15 segundos
    And o saldo da carteira de "user-e02-customer" deve ser "70.00"
    And o saldo da carteira de "user-e02-merchant" deve ser "30.00"

  Scenario: E-03 - Pagamento de salário: empregador deposita para 3 funcionários simultaneamente
    Given que existe uma carteira para o usuário "employer-e03"
    And que existe uma carteira para o usuário "employee-e03-1"
    And que existe uma carteira para o usuário "employee-e03-2"
    And que existe uma carteira para o usuário "employee-e03-3"
    And o usuário "employer-e03" tem saldo de "10000.00"
    When eu transfiro "3000.00" de "employer-e03" para "employee-e03-1"
    And eu transfiro "3500.00" de "employer-e03" para "employee-e03-2"
    And eu transfiro "2000.00" de "employer-e03" para "employee-e03-3"
    Then deve existir 3 registros de transferência no banco em até 15 segundos
    And o saldo da carteira de "employer-e03" deve ser "1500.00"
    And o saldo da carteira de "employee-e03-1" deve ser "3000.00"
    And o saldo da carteira de "employee-e03-2" deve ser "3500.00"
    And o saldo da carteira de "employee-e03-3" deve ser "2000.00"
    And a soma total dos saldos de todos os participantes deve ser "10000.00"

  Scenario: E-04 - Estorno: usuário paga e recebe reembolso completo voltando ao saldo original
    Given que existe uma carteira para o usuário "user-e04-buyer"
    And que existe uma carteira para o usuário "user-e04-seller"
    And o usuário "user-e04-buyer" tem saldo de "200.00"
    When eu transfiro "50.00" de "user-e04-buyer" para "user-e04-seller"
    Then deve existir 1 registro de transferência no banco em até 15 segundos
    And o saldo da carteira de "user-e04-buyer" deve ser "150.00"
    When eu transfiro "50.00" de "user-e04-seller" para "user-e04-buyer"
    Then deve existir 2 registros de transferência no banco em até 15 segundos
    And o saldo da carteira de "user-e04-buyer" deve ser "200.00"
    And o saldo da carteira de "user-e04-seller" deve ser "0.00"

  Scenario: E-05 - Tentativa de overdraft em cadeia: saldo nunca fica negativo
    Given que existe uma carteira para o usuário "user-e05"
    And o usuário "user-e05" tem saldo de "100.00"
    When o usuário "user-e05" saca "40.00"
    Then deve existir 1 transação do tipo "WITHDRAW" para o usuário "user-e05" em até 15 segundos
    And o saldo da carteira de "user-e05" deve ser "60.00"
    When eu realizo um saque de "80.00" para o usuário "user-e05"
    Then após aguardar 3 segundos o saldo da carteira de "user-e05" deve permanecer "60.00"
    And o saldo da carteira de "user-e05" deve ser maior ou igual a "0.00"
    And deve existir exatamente 1 transação do tipo "WITHDRAW" para o usuário "user-e05"
