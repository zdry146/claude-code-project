package com.example.postapi.batch;

import com.example.postapi.model.Post;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom exception for simulating SQL errors during testing
 */
class SimulatedSQLException extends SQLException {
    private final boolean permanent;

    public SimulatedSQLException(String message, boolean permanent) {
        super(message);
        this.permanent = permanent;
    }

    public boolean isPermanent() {
        return permanent;
    }
}

/**
 * A writer that simulates SQL errors for testing Spring Batch retry behavior.
 *
 * Retry behavior:
 * - For TRANSIENT errors: fails on attempt 1, succeeds on attempt 2 (recovers)
 * - For PERMANENT errors: fails on all attempts 1-3, step stops after retries exhausted
 */
@Slf4j
@Component
public class ErrorSimulatingWriter implements ItemWriter<Post> {

    @PersistenceContext
    private EntityManager entityManager;

    // Track retry state: chunkNumber -> attempts made
    private static final Map<Integer, Integer> chunkRetryAttempts = new HashMap<>();

    // Configured error type per chunk
    private static final Map<Integer, String> chunkErrorTypes = new HashMap<>();

    // Global error chunk configuration
    private static int errorOnChunk = 0;
    private static boolean isTransientTest = false;

    private static final int CHUNK_SIZE = 100;

    public static void reset() {
        chunkRetryAttempts.clear();
        chunkErrorTypes.clear();
        errorOnChunk = 0;
        isTransientTest = false;
    }

    public static void setErrorOnChunk(int chunkNumber) {
        errorOnChunk = chunkNumber;
        isTransientTest = false;
        chunkRetryAttempts.clear();
        chunkErrorTypes.clear();
        chunkErrorTypes.put(chunkNumber, "PERMANENT");
        log.info("ErrorSimulatingWriter: PERMANENT error on chunk {}", chunkNumber);
    }

    public static void setTransientErrorOnChunk(int chunkNumber) {
        errorOnChunk = chunkNumber;
        isTransientTest = true;
        chunkRetryAttempts.clear();
        chunkErrorTypes.clear();
        chunkErrorTypes.put(chunkNumber, "TRANSIENT");
        log.info("ErrorSimulatingWriter: TRANSIENT error on chunk {} (recovers on retry)", chunkNumber);
    }

    public static void clearError() {
        reset();
        log.info("ErrorSimulatingWriter cleared");
    }

    @Override
    public void write(Chunk<? extends Post> chunk) throws Exception {
        var posts = chunk.getItems();
        if (posts.isEmpty()) {
            return;
        }

        int chunkNumber = calculateChunkNumber(posts.get(0).getId());

        // Increment retry attempt for this chunk
        int attempts = chunkRetryAttempts.merge(chunkNumber, 1, Integer::sum);
        String errorType = chunkErrorTypes.getOrDefault(chunkNumber, "NONE");

        log.info("Writing chunk {} (attempt {}/3), errorType={}, {} items",
                chunkNumber, attempts, errorType, posts.size());

        // Determine if we should throw an error
        boolean shouldFail = false;
        String failReason = null;

        if (chunkNumber == errorOnChunk && errorOnChunk > 0) {
            if ("PERMANENT".equals(errorType)) {
                // Permanent error: fail on all attempts until exhausted
                shouldFail = true;
                failReason = "PERMANENT";
                log.error("Chunk {} throwing PERMANENT error (attempt {}/3)",
                        chunkNumber, attempts);
            } else if ("TRANSIENT".equals(errorType)) {
                // Transient error: fail only on first attempt, recover on retry
                if (attempts == 1) {
                    shouldFail = true;
                    failReason = "TRANSIENT";
                    log.warn("Chunk {} throwing TRANSIENT error (attempt 1/3) - will recover on retry",
                            chunkNumber);
                } else {
                    log.info("Chunk {} transient error recovered on attempt {} - proceeding normally",
                            chunkNumber, attempts);
                }
            }
        }

        // Throw exception if needed
        if (shouldFail) {
            // For permanent error, mark as exhausted after 3 attempts
            if ("PERMANENT".equals(failReason) && attempts >= 3) {
                log.error("Chunk {} PERMANENT error: all 3 retries exhausted", chunkNumber);
            }
            throw new SimulatedSQLException(
                "Simulated " + failReason + " error in chunk " + chunkNumber +
                " (attempt " + attempts + "/3)", "PERMANENT".equals(failReason));
        }

        // Normal processing
        for (Post post : posts) {
            post.setIsDeleted(true);
            entityManager.merge(post);
        }
        entityManager.flush();
        entityManager.clear();

        log.info("Chunk {} processed successfully", chunkNumber);
    }

    private int calculateChunkNumber(Number id) {
        int idValue = id.intValue();
        return (idValue - 1) / CHUNK_SIZE + 1;
    }
}