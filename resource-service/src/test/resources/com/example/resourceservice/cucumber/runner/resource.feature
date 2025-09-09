Feature: Resource Service API

  Scenario: Upload a resource
    Given I have a resource file
    When I POST the resource to "/resources"
    Then the response status should be 200
    And the resource should be stored in the database

  Scenario: Retrieve a resource
    Given a resource exists in the database
    When I GET the resource by its ID
    Then the response status should be 200
    And the response should contain the resource file name

  Scenario: Delete a resource
    Given a resource exists in the database
    When I DELETE the resource by its ID
    Then the response status should be 200
    And the resource should be removed from the database
