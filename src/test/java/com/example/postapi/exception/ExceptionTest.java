package com.example.postapi.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    // --- BusinessException tests ---

    @Test
    void businessExceptionWithCodeAndMessage() {
        BusinessException ex = new BusinessException(422, "Custom error");
        assertEquals(422, ex.getCode());
        assertEquals("Custom error", ex.getMessage());
    }

    @Test
    void businessExceptionDefaultsToCode400() {
        BusinessException ex = new BusinessException("Bad request");
        assertEquals(400, ex.getCode());
        assertEquals("Bad request", ex.getMessage());
    }

    // --- NotFoundException tests ---

    @Test
    void notFoundExceptionSets404Code() {
        NotFoundException ex = new NotFoundException("Post not found");
        assertEquals(404, ex.getCode());
        assertEquals("Post not found", ex.getMessage());
    }

    @Test
    void notFoundExceptionIsBusinessException() {
        NotFoundException ex = new NotFoundException("Missing");
        assertInstanceOf(BusinessException.class, ex);
    }
}
