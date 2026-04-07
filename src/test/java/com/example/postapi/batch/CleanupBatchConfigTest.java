package com.example.postapi.batch;

import com.example.postapi.model.Post;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ItemProcessor;

import static org.junit.jupiter.api.Assertions.*;

class CleanupBatchConfigTest {

    @Test
    void softDeleteProcessor_shouldSetIsDeletedTrue() throws Exception {
        CleanupBatchConfig config = new CleanupBatchConfig(null);

        Post post = Post.builder()
                .id(1L)
                .title("Test Post")
                .isDeleted(false)
                .build();

        ItemProcessor<Post, Post> processor = config.softDeleteProcessor();
        Post result = processor.process(post);

        assertNotNull(result);
        assertTrue(result.getIsDeleted());
        assertEquals(1L, result.getId());
        assertEquals("Test Post", result.getTitle());
    }

    @Test
    void softDeleteProcessor_shouldNotAffectOtherFields() throws Exception {
        CleanupBatchConfig config = new CleanupBatchConfig(null);

        Post post = Post.builder()
                .id(5L)
                .title("Another Post")
                .content("Some content")
                .authorName("Author")
                .isPublished(true)
                .isDeleted(false)
                .viewCount(10)
                .likeCount(3)
                .build();

        ItemProcessor<Post, Post> processor = config.softDeleteProcessor();
        Post result = processor.process(post);

        assertTrue(result.getIsDeleted());
        assertEquals(5L, result.getId());
        assertEquals("Another Post", result.getTitle());
        assertEquals("Some content", result.getContent());
        assertEquals("Author", result.getAuthorName());
        assertTrue(result.getIsPublished());
        assertEquals(10, result.getViewCount());
        assertEquals(3, result.getLikeCount());
    }
}
