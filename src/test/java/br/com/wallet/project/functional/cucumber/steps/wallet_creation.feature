Feature: Carteira
  Scenario: Criar carteira de usuário
    Dado que não exista carteira para o usuário "user-001"
    Quando eu criar a carteira para o usuário "user-001"
    Então a carteira deve existir com o usuário "user-001" e saldo "0.00"
