Feature: Dead letter messages

  Scenario: Message processing fails constantly
    Given I have an message broker with default config and a message in bdd.in
    When the processing always fails
    Then the message in queue bdd.in.failed has 5 retries
     And bdd.in is empty
     And bdd.out is empty
