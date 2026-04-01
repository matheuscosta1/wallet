Feature: End-to-End Scenarios

  Background:
    Given the system is available

  Scenario: E-01 - Full lifecycle create deposit withdraw transfer and check history
    Given a wallet exists for user "user-e01-alice"
    And a wallet exists for user "user-e01-bob"
    When user "user-e01-alice" deposits "500.00"
    Then the balance of "user-e01-alice" should be "500.00"
    When user "user-e01-alice" withdraws "100.00"
    Then there should be 1 transaction of type "WITHDRAW" for user "user-e01-alice" within 15 seconds
    And the balance of "user-e01-alice" should be "400.00"
    When I transfer "150.00" from "user-e01-alice" to "user-e01-bob"
    Then there should be 1 transfer record in the database within 15 seconds
    And the balance of "user-e01-alice" should be "250.00"
    And the balance of "user-e01-bob" should be "150.00"
    When I retrieve the history for user "user-e01-alice" for today
    Then the response should contain a list with 3 transactions

  Scenario: E-02 - Merchant payment user pays 30.00 and merchant receives exactly 30.00
    Given a wallet exists for user "user-e02-customer"
    And a wallet exists for user "user-e02-merchant"
    And user "user-e02-customer" has a balance of "100.00"
    When I transfer "30.00" from "user-e02-customer" to "user-e02-merchant"
    Then there should be 1 transfer record in the database within 15 seconds
    And the balance of "user-e02-customer" should be "70.00"
    And the balance of "user-e02-merchant" should be "30.00"

  Scenario: E-03 - Employer deposits salary to 3 employees
    Given a wallet exists for user "employer-e03"
    And a wallet exists for user "employee-e03-1"
    And a wallet exists for user "employee-e03-2"
    And a wallet exists for user "employee-e03-3"
    And user "employer-e03" has a balance of "10000.00"
    When I transfer "3000.00" from "employer-e03" to "employee-e03-1"
    And I transfer "3500.00" from "employer-e03" to "employee-e03-2"
    And I transfer "2000.00" from "employer-e03" to "employee-e03-3"
    Then there should be 3 transfer records in the database within 15 seconds
    And the balance of "employer-e03" should be "1500.00"
    And the balance of "employee-e03-1" should be "3000.00"
    And the balance of "employee-e03-2" should be "3500.00"
    And the balance of "employee-e03-3" should be "2000.00"
    And the total sum of all balances should be "10000.00"

  Scenario: E-04 - Refund scenario user pays and receives full refund ending at original balance
    Given a wallet exists for user "user-e04-buyer"
    And a wallet exists for user "user-e04-seller"
    And user "user-e04-buyer" has a balance of "200.00"
    When I transfer "50.00" from "user-e04-buyer" to "user-e04-seller"
    Then there should be 1 transfer record in the database within 15 seconds
    And the balance of "user-e04-buyer" should be "150.00"
    When I transfer "50.00" from "user-e04-seller" to "user-e04-buyer"
    Then there should be 2 transfer records in the database within 15 seconds
    And the balance of "user-e04-buyer" should be "200.00"
    And the balance of "user-e04-seller" should be "0.00"

  Scenario: E-05 - Overdraft attempt chain balance never goes negative
    Given a wallet exists for user "user-e05"
    And user "user-e05" has a balance of "100.00"
    When user "user-e05" withdraws "40.00"
    Then there should be 1 transaction of type "WITHDRAW" for user "user-e05" within 15 seconds
    And the balance of "user-e05" should be "60.00"
    When I withdraw "80.00" for user "user-e05"
    Then after waiting 3 seconds the balance of "user-e05" should remain "60.00"
    And the balance of "user-e05" should be greater than or equal to "0.00"
    And there should be exactly 1 transaction of type "WITHDRAW" for user "user-e05"