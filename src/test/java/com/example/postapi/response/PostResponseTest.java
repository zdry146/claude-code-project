package com.example.postapi.response;

import com.example.postapi.model.Post;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PostResponseTest {

    @Test
    void fromEntityMapsAllFields() {
        LocalDateTime created = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime updated = LocalDateTime.of(2026, 1, 2, 15, 30);

        Post post = Post.builder()
                .id(1L)
                .title("Test Title")
                .content("Test Content")
                .authorName("Test Author")
                .coverImage("http://example.com/cover.jpg")
                .viewCount(100)
                .likeCount(50)
                .isPublished(true)
                .isDeleted(false)
                .createdAt(created)
                .updatedAt(updated)
                .build();

        PostResponse response = PostResponse.fromEntity(post);

        assertEquals(1L, response.getId());
        assertEquals("Test Title", response.getTitle());
        assertEquals("Test Content", response.getContent());
        assertEquals("Test Author", response.getAuthorName());
        assertEquals("http://example.com/cover.jpg", response.getCoverImage());
        assertEquals(100, response.getViewCount());
        assertEquals(50, response.getLikeCount());
        assertTrue(response.getIsPublished());
        assertEquals(created, response.getCreatedAt());
        assertEquals(updated, response.getUpdatedAt());
    }

    @Test
    void fromEntityWithNullCoverImage() {
        Post post = Post.builder()
                .id(2L)
                .title("Title")
                .content("Content")
                .authorName("Author")
                .coverImage(null)
                .viewCount(0)
                .likeCount(0)
                .isPublished(false)
                .isDeleted(false)
                .build();

        PostResponse response = PostResponse.fromEntity(post);

        assertNull(response.getCoverImage());
        assertEquals(2L, response.getId());
    }

    @Test
    void builderAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        PostResponse response = PostResponse.builder()
                .id(10L)
                .title("Built Title")
                .content("Built Content")
                .authorName("Built Author")
                .coverImage("cover.jpg")
                .viewCount(5)
                .likeCount(3)
                .isPublished(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(10L, response.getId());
        assertEquals("Built Title", response.getTitle());

        response.setId(99L);
        response.setTitle("Updated");
        assertEquals(99L, response.getId());
        assertEquals("Updated", response.getTitle());
    }
}
