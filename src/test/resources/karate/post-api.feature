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
    Given path 'api', 'posts', '1'
    When method GET
    Then status 200
    And match response.code == 200
    And match response.data.id == 1

  Scenario: Update a post
    Given path 'api', 'posts', '1'
    And request { title: 'Karate Updated', content: 'Updated by Karate' }
    And header Content-Type = 'application/json'
    When method PUT
    Then status 200
    And match response.code == 200
    And match response.data.title == 'Karate Updated'

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
