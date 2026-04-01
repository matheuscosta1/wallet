Feature: Transaction History and Generic Operation

  Background:
    Given the system is available

  Scenario: H-01 - History after deposit withdraw and transfer returns 3 transactions
    Given a wallet exists for user "user-h01-a"
    And a wallet exists for user "user-h01-b"
    And user "user-h01-a" has a balance of "200.00"
    And user "user-h01-a" made a withdraw of "50.00"
    And user "user-h01-a" made a transfer of "30.00" to "user-h01-b"
    When I retrieve the history for user "user-h01-a" for today
    Then the response should contain a list with 3 transactions

  Scenario: H-02 - History filtered by today returns only todays transactions
    Given a wallet exists for user "user-h02"
    And user "user-h02" has a balance of "100.00"
    When I retrieve the history for user "user-h02" for today
    Then the response should contain a list with 1 transaction

  Scenario: H-03 - History for non-existent user returns 4xx
    When I retrieve the history for user "ghost-user-h03" for today
    Then the response should return status 4xx

  Scenario: H-04 - History for user with no transactions returns empty list
    Given a wallet exists for user "user-h04"
    When I retrieve the history for user "user-h04" for today
    Then the response should contain an empty list

  Scenario: H-05 - History with future date returns empty list
    Given a wallet exists for user "user-h05"
    And user "user-h05" has a balance of "100.00"
    When I retrieve the history for user "user-h05" for date "2099-12-31 00:00:00.000"
    Then the response should contain an empty list

  Scenario: H-06 - Every transaction in history contains balance before and after
    Given a wallet exists for user "user-h06"
    And user "user-h06" has a balance of "100.00"
    When I retrieve the history for user "user-h06" for today
    Then the first transaction should have balanceBeforeTransaction equal to "0.0"
    And the first transaction should have balanceAfterTransaction equal to "100.0"

  Scenario: G-01 - Generic endpoint with type DEPOSIT processes correctly
    Given a wallet exists for user "user-g01"
    When I perform a generic operation of type "DEPOSIT" with amount "80.00" for user "user-g01"
    Then there should be 1 transaction of type "DEPOSIT" for user "user-g01" within 10 seconds
    And the balance of "user-g01" should be "80.00"

  Scenario: G-02 - Generic endpoint with type WITHDRAW processes correctly
    Given a wallet exists for user "user-g02"
    And user "user-g02" has a balance of "100.00"
    When I perform a generic operation of type "WITHDRAW" with amount "25.00" for user "user-g02"
    Then there should be 1 transaction of type "WITHDRAW" for user "user-g02" within 10 seconds
    And the balance of "user-g02" should be "75.00"

  Scenario: G-03 - Generic endpoint with type TRANSFER processes correctly
    Given a wallet exists for user "user-g03-a"
    And a wallet exists for user "user-g03-b"
    And user "user-g03-a" has a balance of "100.00"
    When I perform a generic transfer of "35.00" from "user-g03-a" to "user-g03-b"
    Then there should be 1 transfer record in the database within 10 seconds
    And the balance of "user-g03-a" should be "65.00"

  Scenario: G-04 - Generic endpoint with unknown type returns 400
    Given a wallet exists for user "user-g04"
    When I perform a generic operation of type "INVALID_TYPE" with amount "50.00" for user "user-g04"
    Then the response should return status 400