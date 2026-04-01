#language: pt
Feature: Operações de Transferência
  Como usuário do sistema de carteira
  Quero transferir fundos entre carteiras
  Para que os saldos sejam atualizados atomicamente

  Background:
    Given que o sistema está disponível

  Scenario: T-01 - Transferência bem-sucedida atualiza ambas as carteiras atomicamente
    Given que existe uma carteira para o usuário "user-t01-a"
    And que existe uma carteira para o usuário "user-t01-b"
    And o usuário "user-t01-a" tem saldo de "100.00"
    When eu transfiro "50.00" de "user-t01-a" para "user-t01-b"
    Then deve existir 2 transações vinculadas à transferência em até 10 segundos
    And o saldo da carteira de "user-t01-a" deve ser "50.00"
    And o saldo da carteira de "user-t01-b" deve ser "50.00"

  Scenario: T-02 - Transferência com saldo insuficiente não altera nenhuma carteira
    Given que existe uma carteira para o usuário "user-t02-a"
    And que existe uma carteira para o usuário "user-t02-b"
    And o usuário "user-t02-a" tem saldo de "100.00"
    When eu transfiro "200.00" de "user-t02-a" para "user-t02-b"
    Then após aguardar 3 segundos não deve existir nenhuma transferência no banco
    And o saldo da carteira de "user-t02-a" deve ser "100.00"
    And o saldo da carteira de "user-t02-b" deve ser "0.00"

  Scenario: T-03 - Transferência de carteira de origem inexistente não persiste nada
    Given que existe uma carteira para o usuário "user-t03-b"
    When eu transfiro "50.00" de "ghost-user-t03" para "user-t03-b"
    Then após aguardar 3 segundos não deve existir nenhuma transferência no banco
    And o saldo da carteira de "user-t03-b" deve ser "0.00"

  Scenario: T-04 - Transferência para carteira de destino inexistente não altera a origem
    Given que existe uma carteira para o usuário "user-t04-a"
    And o usuário "user-t04-a" tem saldo de "100.00"
    When eu transfiro "50.00" de "user-t04-a" para "ghost-destination-t04"
    Then após aguardar 3 segundos não deve existir nenhuma transferência no banco
    And o saldo da carteira de "user-t04-a" deve ser "100.00"

  Scenario: T-05 - Transferência para si mesmo resulta em zero transações vinculadas
    Given que existe uma carteira para o usuário "user-t05"
    And o usuário "user-t05" tem saldo de "100.00"
    When eu transfiro "50.00" de "user-t05" para "user-t05"
    Then não deve existir nenhuma transação vinculada à transferência

  Scenario: T-06 - Transferência com valor zero é rejeitada
    Given que existe uma carteira para o usuário "user-t06-a"
    And que existe uma carteira para o usuário "user-t06-b"
    When eu transfiro "0.00" de "user-t06-a" para "user-t06-b"
    Then a resposta deve retornar status 400

  Scenario: T-07 - Registro de transferência vincula corretamente as transações de débito e crédito
    Given que existe uma carteira para o usuário "user-t07-a"
    And que existe uma carteira para o usuário "user-t07-b"
    And o usuário "user-t07-a" tem saldo de "100.00"
    When eu transfiro "30.00" de "user-t07-a" para "user-t07-b"
    Then deve existir 1 registro de transferência no banco em até 10 segundos
    And o registro de transferência deve ter debit_transaction_id e credit_transaction_id distintos

  Scenario: T-08 - Transferência do saldo exato zera a origem e credita o destino corretamente
    Given que existe uma carteira para o usuário "user-t08-a"
    And que existe uma carteira para o usuário "user-t08-b"
    And o usuário "user-t08-a" tem saldo de "60.00"
    And o usuário "user-t08-b" tem saldo de "20.00"
    When eu transfiro "60.00" de "user-t08-a" para "user-t08-b"
    Then deve existir 1 registro de transferência no banco em até 10 segundos
    And o saldo da carteira de "user-t08-a" deve ser "0.00"
    And o saldo da carteira de "user-t08-b" deve ser "80.00"
