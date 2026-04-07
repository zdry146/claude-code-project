package com.example.postapi.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResultTest {

    @Test
    void okWithDataOnly() {
        ApiResult<String> result = ApiResult.ok("Hello");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("Hello", result.getData());
    }

    @Test
    void okWithMessageAndData() {
        ApiResult<Integer> result = ApiResult.ok("Created", 42);
        assertEquals(200, result.getCode());
        assertEquals("Created", result.getMessage());
        assertEquals(42, result.getData());
    }

    @Test
    void okWithNullData() {
        ApiResult<Void> result = ApiResult.ok(null);
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void failWithCodeAndMessage() {
        ApiResult<Void> result = ApiResult.fail(500, "Server error");
        assertEquals(500, result.getCode());
        assertEquals("Server error", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void failWithMessageOnlyDefaultsTo400() {
        ApiResult<Void> result = ApiResult.fail("Bad request");
        assertEquals(400, result.getCode());
        assertEquals("Bad request", result.getMessage());
    }

    @Test
    void okWithComplexData() {
        PageResponse<String> page = PageResponse.<String>builder()
                .content(java.util.List.of("a", "b", "c"))
                .page(0)
                .size(10)
                .totalElements(3)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        ApiResult<PageResponse<String>> result = ApiResult.ok(page);
        assertEquals(200, result.getCode());
        assertEquals(3, result.getData().getContent().size());
    }
}
