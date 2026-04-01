#language: pt
Feature: Operações de Depósito
  Como usuário do sistema de carteira
  Quero realizar depósitos na minha carteira
  Para que meu saldo seja atualizado corretamente de forma assíncrona

  Background:
    Given que o sistema está disponível

  Scenario: D-01 - Depósito bem-sucedido atualiza saldo assincronamente
    Given que existe uma carteira para o usuário "user-d01"
    When eu realizo um depósito de "100.00" para o usuário "user-d01"
    Then a resposta deve conter um "transactionId"
    And o saldo da carteira de "user-d01" deve ser "100.00" em até 10 segundos
    And deve existir 1 transação do tipo "DEPOSIT" para o usuário "user-d01"

  Scenario: D-02 - Três depósitos sequenciais acumulam corretamente
    Given que existe uma carteira para o usuário "user-d02"
    When eu realizo 3 depósitos de "50.00" para o usuário "user-d02"
    Then deve existir 3 transações do tipo "DEPOSIT" para o usuário "user-d02"
    And o saldo da carteira de "user-d02" deve ser "150.00" em até 10 segundos

  Scenario: D-03 - Depósito com valor zero é rejeitado
    Given que existe uma carteira para o usuário "user-d03"
    When eu realizo um depósito de "0.00" para o usuário "user-d03"
    Then a resposta deve retornar status 400

  Scenario: D-04 - Depósito com valor negativo é rejeitado
    Given que existe uma carteira para o usuário "user-d04"
    When eu realizo um depósito de "-50.00" para o usuário "user-d04"
    Then a resposta deve retornar status 400

  Scenario: D-05 - Depósito em carteira inexistente vai para DLQ sem alterar estado
    When eu realizo um depósito de "100.00" para o usuário "ghost-user-d05"
    Then a resposta deve retornar status 200
    And após aguardar 3 segundos não deve existir nenhuma transação no banco

  Scenario: D-06 - Registro de transação salva saldo antes e depois corretamente
    Given que existe uma carteira para o usuário "user-d06"
    When eu realizo um depósito de "200.00" para o usuário "user-d06"
    Then deve existir 1 transação para o usuário "user-d06" em até 10 segundos
    And o saldo antes da transação de "user-d06" deve ser "0.00"
    And o saldo depois da transação de "user-d06" deve ser "200.00"

  Scenario: D-07 - Endpoint de depósito responde imediatamente sem aguardar Kafka
    Given que existe uma carteira para o usuário "user-d07"
    When eu realizo um depósito de "100.00" para o usuário "user-d07"
    Then o tempo de resposta deve ser inferior a 2000 milissegundos
