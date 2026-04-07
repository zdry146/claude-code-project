package com.example.postapi.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PostTest {

    @Test
    void builderDefaultValues() {
        Post post = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .authorName("Author")
                .build();

        assertEquals("Test Title", post.getTitle());
        assertEquals("Test Content", post.getContent());
        assertEquals("Author", post.getAuthorName());
        assertEquals(0, post.getViewCount());
        assertEquals(0, post.getLikeCount());
        assertFalse(post.getIsPublished());
        assertFalse(post.getIsDeleted());
        assertNull(post.getCoverImage());
        assertNull(post.getId());
        assertNull(post.getCreatedAt());
        assertNull(post.getUpdatedAt());
    }

    @Test
    void builderOverridesDefaults() {
        Post post = Post.builder()
                .title("Title")
                .content("Content")
                .authorName("Author")
                .viewCount(100)
                .likeCount(50)
                .isPublished(true)
                .isDeleted(true)
                .coverImage("http://example.com/image.jpg")
                .id(1L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2026, 1, 2, 0, 0))
                .build();

        assertEquals(1L, post.getId());
        assertEquals(100, post.getViewCount());
        assertEquals(50, post.getLikeCount());
        assertTrue(post.getIsPublished());
        assertTrue(post.getIsDeleted());
        assertEquals("http://example.com/image.jpg", post.getCoverImage());
        assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), post.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 1, 2, 0, 0), post.getUpdatedAt());
    }

    @Test
    void noArgsConstructor() {
        Post post = new Post();
        assertNull(post.getId());
        assertNull(post.getTitle());
        assertEquals(0, post.getViewCount());
        assertEquals(0, post.getLikeCount());
        assertFalse(post.getIsPublished());
        assertFalse(post.getIsDeleted());
    }

    @Test
    void allArgsConstructor() {
        LocalDateTime now = LocalDateTime.now();
        Post post = new Post(1L, "Title", "Content", "Author", "cover.jpg",
                10, 5, true, false, now, now);

        assertEquals(1L, post.getId());
        assertEquals("Title", post.getTitle());
        assertEquals("Content", post.getContent());
        assertEquals("Author", post.getAuthorName());
        assertEquals("cover.jpg", post.getCoverImage());
        assertEquals(10, post.getViewCount());
        assertEquals(5, post.getLikeCount());
        assertTrue(post.getIsPublished());
        assertFalse(post.getIsDeleted());
        assertEquals(now, post.getCreatedAt());
        assertEquals(now, post.getUpdatedAt());
    }

    @Test
    void settersAndGetters() {
        Post post = new Post();
        LocalDateTime now = LocalDateTime.now();

        post.setId(99L);
        post.setTitle("New Title");
        post.setContent("New Content");
        post.setAuthorName("New Author");
        post.setCoverImage("new-cover.jpg");
        post.setViewCount(200);
        post.setLikeCount(150);
        post.setIsPublished(true);
        post.setIsDeleted(true);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);

        assertEquals(99L, post.getId());
        assertEquals("New Title", post.getTitle());
        assertEquals("New Content", post.getContent());
        assertEquals("New Author", post.getAuthorName());
        assertEquals("new-cover.jpg", post.getCoverImage());
        assertEquals(200, post.getViewCount());
        assertEquals(150, post.getLikeCount());
        assertTrue(post.getIsPublished());
        assertTrue(post.getIsDeleted());
        assertEquals(now, post.getCreatedAt());
        assertEquals(now, post.getUpdatedAt());
    }
}
