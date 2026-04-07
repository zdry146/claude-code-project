package com.example.postapi.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdatePostRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void allFieldsNullIsValid() {
        UpdatePostRequest req = new UpdatePostRequest();
        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void validPartialUpdate() {
        UpdatePostRequest req = UpdatePostRequest.builder()
                .title("New Title")
                .build();

        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void titleTooLongFails() {
        String longTitle = "a".repeat(201);
        UpdatePostRequest req = UpdatePostRequest.builder()
                .title(longTitle)
                .build();

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void titleAtMaxLengthIsValid() {
        String maxTitle = "a".repeat(200);
        UpdatePostRequest req = UpdatePostRequest.builder()
                .title(maxTitle)
                .build();

        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void authorNameTooLongFails() {
        String longName = "a".repeat(51);
        UpdatePostRequest req = UpdatePostRequest.builder()
                .authorName(longName)
                .build();

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void authorNameAtMaxLengthIsValid() {
        String maxName = "a".repeat(50);
        UpdatePostRequest req = UpdatePostRequest.builder()
                .authorName(maxName)
                .build();

        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void fullUpdateRequest() {
        UpdatePostRequest req = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .authorName("Updated Author")
                .coverImage("http://example.com/new-cover.jpg")
                .isPublished(true)
                .build();

        assertTrue(validator.validate(req).isEmpty());
        assertEquals("Updated Title", req.getTitle());
        assertEquals("Updated Content", req.getContent());
        assertEquals("Updated Author", req.getAuthorName());
        assertEquals("http://example.com/new-cover.jpg", req.getCoverImage());
        assertTrue(req.getIsPublished());
    }

    @Test
    void emptyCoverImageIsAllowed() {
        UpdatePostRequest req = UpdatePostRequest.builder()
                .coverImage("")
                .build();

        assertTrue(validator.validate(req).isEmpty());
    }
}
