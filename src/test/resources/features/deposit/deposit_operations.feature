Feature: Deposit Operations

  Background:
    Given the system is available

  Scenario: D-01 - Successful deposit updates balance asynchronously
    Given a wallet exists for user "user-d01"
    When I deposit "100.00" for user "user-d01"
    Then the response should contain a "transactionId"
    And the balance of "user-d01" should be "100.00" within 10 seconds
    And there should be 1 transaction of type "DEPOSIT" for user "user-d01"

  Scenario: D-02 - Three sequential deposits accumulate correctly
    Given a wallet exists for user "user-d02"
    When I make 3 deposits of "50.00" for user "user-d02"
    Then there should be 3 transactions of type "DEPOSIT" for user "user-d02"
    And the balance of "user-d02" should be "150.00" within 10 seconds

  Scenario: D-03 - Deposit with zero amount is rejected
    Given a wallet exists for user "user-d03"
    When I deposit "0.00" for user "user-d03"
    Then the response should return status 400

  Scenario: D-04 - Deposit with negative amount is rejected
    Given a wallet exists for user "user-d04"
    When I deposit "-50.00" for user "user-d04"
    Then the response should return status 400

  Scenario: D-05 - Deposit to non-existent wallet goes to DLQ without changing state
    When I deposit "100.00" for user "ghost-user-d05"
    Then the response should return status 200
    And after waiting 3 seconds there should be no transactions in the database

  Scenario: D-06 - Transaction record saves correct before and after balance
    Given a wallet exists for user "user-d06"
    When I deposit "200.00" for user "user-d06"
    Then there should be 1 transaction for user "user-d06" within 10 seconds
    And the balance before the transaction for "user-d06" should be "0.00"
    And the balance after the transaction for "user-d06" should be "200.00"

  Scenario: D-07 - Deposit endpoint responds immediately without waiting for Kafka
    Given a wallet exists for user "user-d07"
    When I deposit "100.00" for user "user-d07"
    Then the response time should be less than 2000 milliseconds