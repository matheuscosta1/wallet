Feature: Withdraw Operations

  Background:
    Given the system is available

  Scenario: WD-01 - Withdraw within balance updates correctly
    Given a wallet exists for user "user-wd01"
    And user "user-wd01" has a balance of "100.00"
    When I withdraw "40.00" for user "user-wd01"
    Then there should be 1 transaction of type "WITHDRAW" for user "user-wd01" within 10 seconds
    And the balance of "user-wd01" should be "60.00"

  Scenario: WD-02 - Withdraw exact balance leaves wallet at zero
    Given a wallet exists for user "user-wd02"
    And user "user-wd02" has a balance of "75.00"
    When I withdraw "75.00" for user "user-wd02"
    Then there should be 1 transaction of type "WITHDRAW" for user "user-wd02" within 10 seconds
    And the balance of "user-wd02" should be "0.00"

  Scenario: WD-03 - Withdraw more than balance is rejected and balance unchanged
    Given a wallet exists for user "user-wd03"
    And user "user-wd03" has a balance of "100.00"
    When I withdraw "200.00" for user "user-wd03"
    Then the response should return status 200
    And after waiting 3 seconds there should be no "WITHDRAW" transactions for user "user-wd03"
    And the balance of "user-wd03" should be "100.00"

  Scenario: WD-04 - Withdraw from empty wallet is rejected
    Given a wallet exists for user "user-wd04"
    When I withdraw "10.00" for user "user-wd04"
    Then after waiting 3 seconds there should be no "WITHDRAW" transactions for user "user-wd04"
    And the balance of "user-wd04" should be "0.00"

  Scenario: WD-05 - Withdraw with zero amount returns 400
    Given a wallet exists for user "user-wd05"
    When I withdraw "0.00" for user "user-wd05"
    Then the response should return status 400

  Scenario: WD-06 - Withdraw with negative amount returns 400
    Given a wallet exists for user "user-wd06"
    When I withdraw "-30.00" for user "user-wd06"
    Then the response should return status 400

  Scenario: WD-07 - Transaction record saves correct before and after balance for withdraw
    Given a wallet exists for user "user-wd07"
    And user "user-wd07" has a balance of "100.00"
    When I withdraw "30.00" for user "user-wd07"
    Then there should be 1 transaction of type "WITHDRAW" for user "user-wd07" within 10 seconds
    And the balance before the withdraw transaction for "user-wd07" should be "100.00"
    And the balance after the withdraw transaction for "user-wd07" should be "70.00"