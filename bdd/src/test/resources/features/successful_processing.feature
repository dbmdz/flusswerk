Feature: Successful processing

  Scenario: Message is processed successfully
    Given I have an message broker with default config and a message in bdd.in
    When the processing always works
    Then the message in queue bdd.out has a field blah with value blubb
     And bdd.in.failed is empty
     And bdd.in.retry is empty
