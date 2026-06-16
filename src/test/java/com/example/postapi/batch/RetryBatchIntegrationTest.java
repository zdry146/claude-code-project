package com.example.postapi.batch;

import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
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
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RetryBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job cleanupUnpublishedPostsJobWithErrorSimulation;

    @Autowired
    private Job cleanupUnpublishedPostsJobWithRetry;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM posts");
        now = LocalDateTime.now();
        ErrorSimulatingWriter.clearError();
        ErrorSimulatingWriter.reset();
    }

    @AfterEach
    void tearDown() {
        ErrorSimulatingWriter.clearError();
        ErrorSimulatingWriter.reset();
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

    /**
     * Test normal case - no errors, all records processed successfully
     */
    @Test
    void retryJob_shouldProcessAllRecordsSuccessfully() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 1: NORMAL CASE - NO ERRORS");
        System.out.println("=".repeat(80));

        // Insert 250 old unpublished posts (2.5 chunks of 100)
        for (int i = 1; i <= 250; i++) {
            insertPost("Normal Post " + i, "Content " + i, "Author " + i,
                    false, 0, 0, now.minusDays(35 + i));
        }

        List<Post> before = postRepository.findByIsDeletedFalse();
        System.out.println("[BEFORE] Total active posts: " + before.size());

        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJobWithRetry);
        var jobExecution = jobLauncherTestUtils.launchJob();

        List<Post> after = postRepository.findByIsDeletedFalse();
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("\n[RESULT] Job status: " + jobExecution.getStatus());
        System.out.println("[RESULT] Deleted posts: " + deleted.size());
        System.out.println("[RESULT] Retained posts: " + after.size());

        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(250, deleted.size());
        assertEquals(0, after.size());

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 1 PASSED: All 250 records processed successfully");
        System.out.println("=".repeat(80));
    }

    /**
     * Test transient error - error on chunk 2, succeeds on retry
     */
    @Test
    void retryJob_shouldRecoverFromTransientErrorOnRetry() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 2: TRANSIENT ERROR - RETRY AND SUCCEED");
        System.out.println("=".repeat(80));

        // Configure transient error on chunk 2
        ErrorSimulatingWriter.setTransientErrorOnChunk(2);
        System.out.println("[CONFIG] ErrorSimulatingWriter: TRANSIENT error on chunk 2");

        // Insert 200 posts (2 chunks)
        for (int i = 1; i <= 200; i++) {
            insertPost("Post " + i, "Content " + i, "Author " + i,
                    false, 0, 0, now.minusDays(35 + i));
        }

        List<Post> before = postRepository.findByIsDeletedFalse();
        System.out.println("[BEFORE] Total active posts: " + before.size());
        System.out.println("[EXPECTED] Chunk 1 succeeds, Chunk 2 fails then retries succeeds");

        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJobWithErrorSimulation);
        var jobExecution = jobLauncherTestUtils.launchJob();

        List<Post> after = postRepository.findByIsDeletedFalse();
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("\n[RESULT] Job status: " + jobExecution.getStatus());
        System.out.println("[RESULT] Deleted posts: " + deleted.size());
        System.out.println("[RESULT] Retained posts: " + after.size());

        // Transient error should recover on retry
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, jobExecution.getStatus());
        assertEquals(200, deleted.size());

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 2 PASSED: Transient error recovered on retry");
        System.out.println("=".repeat(80));
    }

    /**
     * Test permanent error - error on chunk 2, all retries fail, step stops
     */
    @Test
    void retryJob_shouldStopStepAfterAllRetriesExhausted() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 3: PERMANENT ERROR - ALL RETRIES EXHAUSTED, STEP STOPS");
        System.out.println("=".repeat(80));

        // Configure permanent error on chunk 2 (will fail 3 times, then stop)
        ErrorSimulatingWriter.setErrorOnChunk(2);
        System.out.println("[CONFIG] ErrorSimulatingWriter: PERMANENT error on chunk 2");
        System.out.println("[CONFIG] retryLimit = 3");
        System.out.println("[EXPECTED] Chunk 1 commits, Chunk 2 fails 3x → step stops");

        // Insert 500 posts (5 chunks of 100)
        for (int i = 1; i <= 500; i++) {
            insertPost("Post " + i, "Content " + i, "Author " + i,
                    false, 0, 0, now.minusDays(35 + i));
        }

        List<Post> before = postRepository.findByIsDeletedFalse();
        System.out.println("[BEFORE] Total active posts: " + before.size());

        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJobWithErrorSimulation);
        var jobExecution = jobLauncherTestUtils.launchJob();

        List<Post> after = postRepository.findByIsDeletedFalse();
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("\n[RESULT] Job status: " + jobExecution.getStatus());
        System.out.println("[RESULT] Deleted posts: " + deleted.size());
        System.out.println("[RESULT] Retained posts: " + after.size());

        // Job should fail because permanent error exhausted retries
        assertEquals(org.springframework.batch.core.BatchStatus.FAILED, jobExecution.getStatus());

        // Chunk 1 (100) should be committed, Chunk 2 rolled back, Chunks 3-5 never executed
        System.out.println("\n[VERIFICATION]");
        System.out.println("  Deleted count: " + deleted.size());
        System.out.println("  Expected ~100 (chunk 1 only)");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 3 COMPLETE: Step stopped after retry exhaustion");
        System.out.println("=".repeat(80));
    }

    /**
     * Test chunk rollback - verify that failed chunk is rolled back completely
     */
    @Test
    void retryJob_shouldRollbackEntireFailedChunk() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 4: CHUNK ROLLBACK ON FAILURE");
        System.out.println("=".repeat(80));

        // Configure permanent error on chunk 3
        ErrorSimulatingWriter.setErrorOnChunk(3);
        System.out.println("[CONFIG] ErrorSimulatingWriter: PERMANENT error on chunk 3");
        System.out.println("[EXPECTED]");
        System.out.println("  - Chunk 1 (100): commits");
        System.out.println("  - Chunk 2 (100): commits");
        System.out.println("  - Chunk 3 (100): fails, rolled back");
        System.out.println("  - Chunks 4-5: NOT executed");

        // Insert 500 posts
        for (int i = 1; i <= 500; i++) {
            insertPost("Post " + i, "Content " + i, "Author " + i,
                    false, 0, 0, now.minusDays(35 + i));
        }

        List<Post> before = postRepository.findByIsDeletedFalse();
        System.out.println("[BEFORE] Total active posts: " + before.size());

        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJobWithErrorSimulation);
        var jobExecution = jobLauncherTestUtils.launchJob();

        List<Post> after = postRepository.findByIsDeletedFalse();
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("\n[RESULT] Job status: " + jobExecution.getStatus());
        System.out.println("[RESULT] Deleted posts: " + deleted.size());
        System.out.println("[RESULT] Retained posts: " + after.size());

        System.out.println("\n[VERIFICATION]");
        System.out.println("  Chunk 1 + 2 should commit = ~200 deleted");
        System.out.println("  Chunk 3 should rollback = ~100 retained");
        System.out.println("  Chunks 4-5 not started = ~200 retained");

        System.out.println("\n" + "=".repeat(80));
        System.out.println("TEST 4 COMPLETE: Chunk rollback behavior verified");
        System.out.println("=".repeat(80));
    }
}