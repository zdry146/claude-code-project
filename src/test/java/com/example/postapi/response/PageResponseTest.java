package com.example.postapi.response;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResponseTest {

    @Test
    void builderWithAllFields() {
        List<String> content = List.of("one", "two", "three");
        PageResponse<String> page = PageResponse.<String>builder()
                .content(content)
                .page(2)
                .size(10)
                .totalElements(25)
                .totalPages(3)
                .first(false)
                .last(true)
                .build();

        assertEquals(content, page.getContent());
        assertEquals(2, page.getPage());
        assertEquals(10, page.getSize());
        assertEquals(25, page.getTotalElements());
        assertEquals(3, page.getTotalPages());
        assertFalse(page.isFirst());
        assertTrue(page.isLast());
    }

    @Test
    void emptyContent() {
        PageResponse<Integer> page = PageResponse.<Integer>builder()
                .content(List.of())
                .page(0)
                .size(10)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();

        assertTrue(page.getContent().isEmpty());
        assertEquals(0, page.getTotalElements());
        assertEquals(0, page.getTotalPages());
    }

    @Test
    void singlePage() {
        PageResponse<String> page = PageResponse.<String>builder()
                .content(List.of("only"))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        assertTrue(page.isFirst());
        assertTrue(page.isLast());
    }

    @Test
    void settersAndGetters() {
        PageResponse<String> page = new PageResponse<>();
        page.setContent(List.of("item"));
        page.setPage(5);
        page.setSize(20);
        page.setTotalElements(100);
        page.setTotalPages(5);
        page.setFirst(false);
        page.setLast(true);

        assertEquals(List.of("item"), page.getContent());
        assertEquals(5, page.getPage());
        assertEquals(20, page.getSize());
        assertEquals(100, page.getTotalElements());
        assertEquals(5, page.getTotalPages());
        assertFalse(page.isFirst());
        assertTrue(page.isLast());
    }
}
