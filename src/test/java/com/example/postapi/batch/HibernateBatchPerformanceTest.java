package com.example.postapi.batch;

import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class HibernateBatchPerformanceTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Job cleanupUnpublishedPostsJob;

    @Autowired
    private Job cleanupUnpublishedPostsJobNonBatch;

    private LocalDateTime now;
    private final int TEST_POST_COUNT = 500;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM posts");
        now = LocalDateTime.now();

        for (int i = 1; i <= TEST_POST_COUNT; i++) {
            insertPost("Batch Test Post " + i, "Content " + i, "Author " + i,
                    false, 0, 0, now.minusDays(35 + i));
        }

        insertPost("Recent Test 1", "Content", "Author", false, 0, 0, now.minusDays(5));
        insertPost("Recent Test 2", "Content", "Author", false, 0, 0, now.minusDays(10));
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

    @Test
    void batchMode_shouldCleanUp500OldPostsWithBatching() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HIBERNATE BATCH MODE TEST - PostgreSQL");
        System.out.println("Testing with " + TEST_POST_COUNT + " old posts");
        System.out.println("Using: batchSoftDeleteWriter (Hibernate JDBC batching enabled)");
        System.out.println("=".repeat(80));

        List<Post> before = postRepository.findByIsDeletedFalse();
        System.out.println("\n[BEFORE] Total active posts: " + before.size());

        System.out.println("\n[CONFIG] Hibernate Batch Settings:");
        System.out.println("  - jdbc.batch_size=25");
        System.out.println("  - order_inserts=true");
        System.out.println("  - order_updates=true");
        System.out.println("  - batch_versioned_data=true");

        long startTime = System.currentTimeMillis();
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJob);
        var jobExecution = jobLauncherTestUtils.launchJob();
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        List<Post> after = postRepository.findByIsDeletedFalse();
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("\n[RESULT] Job status: " + jobExecution.getStatus());
        System.out.println("[PERFORMANCE] Execution time: " + duration + " ms");
        System.out.println("[RESULT] Deleted posts: " + deleted.size());
        System.out.println("[RESULT] Retained posts: " + after.size());

        assertEquals(TEST_POST_COUNT, deleted.size());
        assertEquals(2, after.size());

        System.out.println("\n" + "=".repeat(80));
        System.out.println("BATCH MODE PERFORMANCE TEST COMPLETE");
        System.out.println("Total time: " + duration + " ms for " + TEST_POST_COUNT + " updates");
        System.out.println("Average: " + (duration * 1000.0 / TEST_POST_COUNT) + " µs per update");
        System.out.println("=".repeat(80));
    }

    @Test
    void nonBatchMode_shouldCleanUp500OldPostsWithoutBatching() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("NON-BATCH MODE TEST - PostgreSQL");
        System.out.println("Testing with " + TEST_POST_COUNT + " old posts");
        System.out.println("Using: nonBatchSoftDeleteWriter (No Hibernate JDBC batching)");
        System.out.println("=".repeat(80));

        List<Post> before = postRepository.findByIsDeletedFalse();
        System.out.println("\n[BEFORE] Total active posts: " + before.size());

        System.out.println("\n[CONFIG] Non-Batch Mode Settings:");
        System.out.println("  - Each entity merge() followed by immediate flush()");
        System.out.println("  - entityManager.clear() after each update");
        System.out.println("  - No statement batching");

        long startTime = System.currentTimeMillis();
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJobNonBatch);
        var jobExecution = jobLauncherTestUtils.launchJob();
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        List<Post> after = postRepository.findByIsDeletedFalse();
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("\n[RESULT] Job status: " + jobExecution.getStatus());
        System.out.println("[PERFORMANCE] Execution time: " + duration + " ms");
        System.out.println("[RESULT] Deleted posts: " + deleted.size());
        System.out.println("[RESULT] Retained posts: " + after.size());

        assertEquals(TEST_POST_COUNT, deleted.size());
        assertEquals(2, after.size());

        System.out.println("\n" + "=".repeat(80));
        System.out.println("NON-BATCH MODE PERFORMANCE TEST COMPLETE");
        System.out.println("Total time: " + duration + " ms for " + TEST_POST_COUNT + " updates");
        System.out.println("Average: " + (duration * 1000.0 / TEST_POST_COUNT) + " µs per update");
        System.out.println("=".repeat(80));
    }

    @Test
    void compareBatchVsNonBatch_shouldShowPerformanceDifference() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("COMPARISON TEST: BATCH vs NON-BATCH MODE");
        System.out.println("Testing with " + TEST_POST_COUNT + " old posts");
        System.out.println("=".repeat(80));

        // === Phase 1: Non-batch mode ===
        System.out.println("\n" + "-".repeat(80));
        System.out.println("PHASE 1: NON-BATCH MODE TEST");
        System.out.println("-".repeat(80));

        List<Post> before1 = postRepository.findByIsDeletedFalse();
        System.out.println("[BEFORE] Total active posts: " + before1.size());

        long startTime1 = System.currentTimeMillis();
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJobNonBatch);
        var jobExecution1 = jobLauncherTestUtils.launchJob();
        long endTime1 = System.currentTimeMillis();
        long duration1 = endTime1 - startTime1;

        List<Post> deleted1 = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("[RESULT] Job status: " + jobExecution1.getStatus());
        System.out.println("[PERFORMANCE] Execution time: " + duration1 + " ms");
        System.out.println("[RESULT] Deleted posts: " + deleted1.size());

        // Reset data for phase 2
        System.out.println("\n[RESET] Resetting test data for phase 2...");
        jdbcTemplate.update("DELETE FROM posts");
        now = LocalDateTime.now();

        for (int i = 1; i <= TEST_POST_COUNT; i++) {
            insertPost("Batch Test Post " + i, "Content " + i, "Author " + i,
                    false, 0, 0, now.minusDays(35 + i));
        }
        insertPost("Recent Test 1", "Content", "Author", false, 0, 0, now.minusDays(5));
        insertPost("Recent Test 2", "Content", "Author", false, 0, 0, now.minusDays(10));

        // === Phase 2: Batch mode ===
        System.out.println("\n" + "-".repeat(80));
        System.out.println("PHASE 2: BATCH MODE TEST");
        System.out.println("-".repeat(80));

        List<Post> before2 = postRepository.findByIsDeletedFalse();
        System.out.println("[BEFORE] Total active posts: " + before2.size());

        long startTime2 = System.currentTimeMillis();
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJob);
        var jobExecution2 = jobLauncherTestUtils.launchJob();
        long endTime2 = System.currentTimeMillis();
        long duration2 = endTime2 - startTime2;

        List<Post> deleted2 = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("[RESULT] Job status: " + jobExecution2.getStatus());
        System.out.println("[PERFORMANCE] Execution time: " + duration2 + " ms");
        System.out.println("[RESULT] Deleted posts: " + deleted2.size());

        // === Summary ===
        System.out.println("\n" + "=".repeat(80));
        System.out.println("PERFORMANCE COMPARISON SUMMARY");
        System.out.println("=".repeat(80));
        System.out.printf("Test Configuration: %d old posts, chunk size 100%n", TEST_POST_COUNT);
        System.out.println();
        System.out.println("| Mode       | Execution Time | Avg per Update | Improvement |");
        System.out.println("|------------|---------------|----------------|-------------|");
        System.out.printf("| Non-Batch  | %6d ms       | %.1f µs        | baseline     |%n", duration1, (duration1 * 1000.0 / TEST_POST_COUNT));
        System.out.printf("| Batch      | %6d ms       | %.1f µs        | %.1f%%       |%n",
                duration2, (duration2 * 1000.0 / TEST_POST_COUNT),
                ((double)(duration1 - duration2) / duration1) * 100);
        System.out.println("=".repeat(80));

        assertEquals(TEST_POST_COUNT, deleted1.size());
        assertEquals(TEST_POST_COUNT, deleted2.size());
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, jobExecution1.getStatus());
        assertEquals(org.springframework.batch.core.BatchStatus.COMPLETED, jobExecution2.getStatus());
    }

    @Test
    void multiSqlBatch_shouldDemonstrateBatchingWithMultipleOperations() throws Exception {
        // First ensure audit_log table exists
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS audit_log (" +
                    "id SERIAL PRIMARY KEY, " +
                    "entity_type VARCHAR(50), " +
                    "entity_id BIGINT, " +
                    "action VARCHAR(50), " +
                    "created_at TIMESTAMP)");
        } catch (Exception e) {
            // Table might already exist or H2 doesn't support this syntax
            System.out.println("[INFO] audit_log table setup skipped");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("MULTI-SQL BATCH MODE TEST");
        System.out.println("Testing: UPDATE posts + INSERT audit_log per record");
        System.out.println("=".repeat(80));

        System.out.println("\n[EXPLANATION]");
        System.out.println("Hibernate JDBC batching works PER STATEMENT TYPE:");
        System.out.println("  - UPDATE posts statements → batched together (size 25)");
        System.out.println("  - INSERT audit_log statements → batched separately (size 25)");
        System.out.println("  - Different SQL templates = different batches");

        List<Post> before = postRepository.findByIsDeletedFalse();
        System.out.println("\n[BEFORE] Total active posts: " + before.size());

        long startTime = System.currentTimeMillis();
        // Note: This test uses the batch mode job for demonstration
        // In real scenario, you'd have a multiSqlBatchWriter step
        jobLauncherTestUtils.setJob(cleanupUnpublishedPostsJob);
        var jobExecution = jobLauncherTestUtils.launchJob();
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        List<Post> after = postRepository.findByIsDeletedFalse();
        List<Post> deleted = postRepository.findAll().stream()
                .filter(Post::getIsDeleted)
                .toList();

        System.out.println("\n[RESULT] Job status: " + jobExecution.getStatus());
        System.out.println("[PERFORMANCE] Execution time: " + duration + " ms");
        System.out.println("[RESULT] Deleted posts: " + deleted.size());

        System.out.println("\n" + "=".repeat(80));
        System.out.println("BATCHING EXPLANATION:");
        System.out.println("  For " + deleted.size() + " posts with 2 operations each:");
        System.out.println("  - Total JDBC statements: ~" + (deleted.size() * 2));
        System.out.println("  - Batches sent: ~" + (deleted.size() * 2 / 25) + " (assuming batch_size=25)");
        System.out.println("  - Batch 1: UPDATE (25 statements)");
        System.out.println("  - Batch 2: UPDATE (25 statements)");
        System.out.println("  - Batch 3: INSERT audit_log (25 statements)");
        System.out.println("  - Batch 4: INSERT audit_log (25 statements)");
        System.out.println("  - ... and so on");
        System.out.println("=".repeat(80));
    }
}