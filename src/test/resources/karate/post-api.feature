Feature: Post API E2E contract
  Background:
    * url baseUrl
    * def createdId = null

  Scenario: List published posts
    Given path 'api', 'posts', 'published'
    And param page = 0
    And param size = 5
    When method GET
    Then status 200
    And match response.code == 200
    And match response.data.content == '#array'
    And match response.data.totalElements == '#number'

  Scenario: Create a new post
    Given path 'api', 'posts'
    And request { title: 'Karate Test Post', content: 'Created by Karate', authorName: 'Karate Runner' }
    And header Content-Type = 'application/json'
    When method POST
    Then status 200
    And match response.code == 200
    And match response.data.id == '#number'
    And match response.data.title == 'Karate Test Post'
    And match response.data.isPublished == false
    * def createdId = response.data.id

  Scenario: Get post by id
    # Cleanup batch may mark old posts is_deleted=true, so we cannot
    # assume id=1 exists. Create a fresh post and use its id.
    Given path 'api', 'posts'
    And request { title: 'Karate Get Test', content: 'Created for get-by-id test', authorName: 'Karate' }
    And header Content-Type = 'application/json'
    When method POST
    Then status 200
    * def getTestId = response.data.id
    Given path 'api', 'posts', getTestId
    When method GET
    Then status 200
    And match response.code == 200
    And match response.data.id == getTestId

  Scenario: Update a post
    # Same as above: create a fresh post to get a valid id (id=1 may be cleaned up)
    Given path 'api', 'posts'
    And request { title: 'Karate Update Source', content: 'Created for update test', authorName: 'Karate' }
    And header Content-Type = 'application/json'
    When method POST
    Then status 200
    * def updateTestId = response.data.id
    Given path 'api', 'posts', updateTestId
    And request { title: 'Karate Updated', content: 'Updated by Karate' }
    And header Content-Type = 'application/json'
    When method PUT
    Then status 200
    And match response.code == 200
    And match response.data.title == 'Karate Updated'
    And match response.data.id == updateTestId

  Scenario: Like a non-existent post returns 404
    Given path 'api', 'posts', '999999', 'like'
    When method POST
    Then status 404
    And match response.code == 404

  Scenario: Search posts
    Given path 'api', 'posts', 'search'
    And param keyword = 'Karate'
    When method GET
    Then status 200
    And match response.code == 200
    And match response.data.content == '#array'

  Scenario: List all posts (admin)
    Given path 'api', 'posts', 'all'
    And param page = 0
    And param size = 5
    When method GET
    Then status 200
    And match response.code == 200
    And match response.data.content == '#array'

  Scenario: Validation - empty title rejected
    Given path 'api', 'posts'
    And request { title: '', content: 'x', authorName: 'test' }
    And header Content-Type = 'application/json'
    When method POST
    Then status 400

  Scenario: Validation - missing authorName rejected
    Given path 'api', 'posts'
    And request { title: 'Good', content: 'x' }
    And header Content-Type = 'application/json'
    When method POST
    Then status 400
