package com.example.postapi.exception;

import com.example.postapi.response.ApiResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFoundReturns404() {
        NotFoundException ex = new NotFoundException("Post not found");
        ApiResult<Void> result = handler.handleNotFound(ex);

        assertEquals(404, result.getCode());
        assertEquals("Post not found", result.getMessage());
    }

    @Test
    void handleBusinessExceptionReturns400() {
        BusinessException ex = new BusinessException(422, "Custom validation failed");
        ApiResult<Void> result = handler.handleBusiness(ex);

        assertEquals(422, result.getCode());
        assertEquals("Custom validation failed", result.getMessage());
    }

    @Test
    void handleBusinessExceptionDefaultCode() {
        BusinessException ex = new BusinessException("Generic error");
        ApiResult<Void> result = handler.handleBusiness(ex);

        assertEquals(400, result.getCode());
    }

    @Test
    void handleValidationWithFieldError() {
        FieldError fieldError = mock(FieldError.class);
        when(fieldError.getDefaultMessage()).thenReturn("Title is required");

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ApiResult<Void> result = handler.handleValidation(ex);

        assertEquals(400, result.getCode());
        assertEquals("Title is required", result.getMessage());
    }

    @Test
    void handleValidationWithNoFieldError() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(null);

        ApiResult<Void> result = handler.handleValidation(ex);

        assertEquals(400, result.getCode());
        assertEquals("Validation failed", result.getMessage());
    }

    @Test
    void handleBindException() {
        FieldError fieldError = mock(FieldError.class);
        when(fieldError.getDefaultMessage()).thenReturn("Bind error message");

        BindException ex = mock(BindException.class);
        org.springframework.validation.BindingResult bindingResult = mock(org.springframework.validation.BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(fieldError);

        ApiResult<Void> result = handler.handleBind(ex);

        assertEquals(400, result.getCode());
        assertEquals("Bind error message", result.getMessage());
    }

    @Test
    void handleOtherReturns500() {
        Exception ex = new RuntimeException("Unexpected");
        ApiResult<Void> result = handler.handleOther(ex);

        assertEquals(500, result.getCode());
        assertEquals("系统异常，请稍后重试", result.getMessage());
    }
}
