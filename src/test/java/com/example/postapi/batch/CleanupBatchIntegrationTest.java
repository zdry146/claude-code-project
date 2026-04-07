package com.example.postapi.batch;

import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringBootTest
@TestPropertySource(properties = {
    "spring.batch.job.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.batch.jdbc.initialize-schema=always"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CleanupBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job cleanupUnpublishedPostsJob;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // Only delete posts created by this test, not all posts
        deleteTestPosts();
        now = LocalDateTime.now();

        // Posts older than 30 days - unpublished (SHOULD be deleted)
        insertPost("Old Unpublished Post 1", "This is old and unpublished", "Author A", false, 0, 0, now.minusDays(35));
        insertPost("Old Unpublished Post 2", "Another old unpublished post", "Author B", false, 5, 2, now.minusDays(45));
        insertPost("Old Unpublished Post 3", "Yet another old post", "Author C", false, 10, 1, now.minusDays(60));

        // Recent unpublished posts (SHOULD NOT be deleted)
        insertPost("Recent Unpublished Post 1", "This is recent and unpublished", "Author D", false, 0, 0, now.minusDays(5));
        insertPost("Recent Unpublished Post 2", "Another recent unpublished post", "Author E", false, 3, 0, now.minusDays(15));

        // Old but published post (SHOULD NOT be deleted)
        insertPost("Old Published Post", "This is old but published", "Author F", true, 100, 50, now.minusDays(40));
    }

    private void insertPost(String title, String content, String authorName,
                            boolean isPublished, int viewCount, int likeCount,
                            LocalDateTime createdAt) {
        Timestamp ts = Timestamp.valueOf(createdAt);
        Timestamp nowTs = Timestamp.valueOf(now);
        jdbcTemplate.update("""
            INSERT INTO posts (author_name, content, cover_image, created_at, is_deleted, is_published, like_count, title, updated_at, view_count)
            VALUES (?, ?, NULL, ?, false, ?, ?, ?, ?, ?)
            """,
            authorName, content, ts, isPublished, likeCount, title, nowTs, viewCount
        );
    }

    private void deleteTestPosts() {
        String[] testTitles = {
            "Old Unpublished Post 1", "Old Unpublished Post 2", "Old Unpublished Post 3",
            "Recent Unpublished Post 1", "Recent Unpublished Post 2", "Old Published Post"
        };
        for (String title : testTitles) {
            jdbcTemplate.update("DELETE FROM posts WHERE title = ?", title);
        }
    }

    @Test
    void cleanupJob_shouldDeleteOldUnpublishedPostsAndKeepOthers() throws Exception {
        // Verify initial state: 6 posts
        List<Post> before = postRepository.findByIsDeletedFalse();
        assertEquals(6, before.size(), "Should start with 6 posts");

        // Run the cleanup job
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJob);
        jobLauncherTestUtils.launchJob();

        // Verify final state: 3 posts remain (safe posts)
        List<Post> after = postRepository.findByIsDeletedFalse();
        assertEquals(3, after.size(), "Should have 3 posts after cleanup");

        // Verify safe posts are retained
        List<String> retainedTitles = after.stream()
                .map(Post::getTitle)
                .toList();
        assertTrue(retainedTitles.contains("Recent Unpublished Post 1"));
        assertTrue(retainedTitles.contains("Recent Unpublished Post 2"));
        assertTrue(retainedTitles.contains("Old Published Post"));

        // Verify deleted posts
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();
        assertEquals(3, deleted.size());

        List<String> deletedTitles = deleted.stream()
                .map(Post::getTitle)
                .toList();
        assertTrue(deletedTitles.contains("Old Unpublished Post 1"));
        assertTrue(deletedTitles.contains("Old Unpublished Post 2"));
        assertTrue(deletedTitles.contains("Old Unpublished Post 3"));
    }

    @Test
    void cleanupJob_shouldKeepRecentUnpublishedPosts() throws Exception {
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJob);
        jobLauncherTestUtils.launchJob();

        List<Post> remaining = postRepository.findByIsDeletedFalse();
        assertEquals(3, remaining.size());

        // Neither of the recent unpublished posts should be deleted
        assertTrue(remaining.stream().anyMatch(p -> p.getTitle().equals("Recent Unpublished Post 1")));
        assertTrue(remaining.stream().anyMatch(p -> p.getTitle().equals("Recent Unpublished Post 2")));
    }

    @Test
    void cleanupJob_shouldKeepOldPublishedPosts() throws Exception {
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJob);
        jobLauncherTestUtils.launchJob();

        Post publishedPost = postRepository.findAll().stream()
                .filter(p -> p.getTitle().equals("Old Published Post"))
                .findFirst()
                .orElseThrow();
        assertFalse(publishedPost.getIsDeleted(), "Old published post should NOT be deleted");
    }
}
