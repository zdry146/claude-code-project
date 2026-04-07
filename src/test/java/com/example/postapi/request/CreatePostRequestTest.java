package com.example.postapi.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CreatePostRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validRequest() {
        CreatePostRequest req = CreatePostRequest.builder()
                .title("Valid Title")
                .content("Valid Content")
                .authorName("Author Name")
                .coverImage("http://example.com/cover.jpg")
                .build();

        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void titleCannotBeBlank() {
        CreatePostRequest req = CreatePostRequest.builder()
                .title("")
                .content("Content")
                .authorName("Author")
                .build();

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void titleCannotBeNull() {
        CreatePostRequest req = CreatePostRequest.builder()
                .title(null)
                .content("Content")
                .authorName("Author")
                .build();

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void authorNameCannotBeBlank() {
        CreatePostRequest req = CreatePostRequest.builder()
                .title("Title")
                .content("Content")
                .authorName("")
                .build();

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void authorNameCannotBeNull() {
        CreatePostRequest req = CreatePostRequest.builder()
                .title("Title")
                .content("Content")
                .authorName(null)
                .build();

        assertFalse(validator.validate(req).isEmpty());
    }

    @Test
    void contentCanBeNull() {
        CreatePostRequest req = CreatePostRequest.builder()
                .title("Title")
                .content(null)
                .authorName("Author")
                .build();

        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void coverImageCanBeNull() {
        CreatePostRequest req = CreatePostRequest.builder()
                .title("Title")
                .content("Content")
                .authorName("Author")
                .coverImage(null)
                .build();

        assertTrue(validator.validate(req).isEmpty());
    }

    @Test
    void builderDefaults() {
        CreatePostRequest req = new CreatePostRequest();
        assertNull(req.getTitle());
        assertNull(req.getContent());
        assertNull(req.getAuthorName());
        assertNull(req.getCoverImage());
    }

    @Test
    void settersAndGetters() {
        CreatePostRequest req = new CreatePostRequest();
        req.setTitle("Title");
        req.setContent("Content");
        req.setAuthorName("Author");
        req.setCoverImage("cover.jpg");

        assertEquals("Title", req.getTitle());
        assertEquals("Content", req.getContent());
        assertEquals("Author", req.getAuthorName());
        assertEquals("cover.jpg", req.getCoverImage());
    }
}
