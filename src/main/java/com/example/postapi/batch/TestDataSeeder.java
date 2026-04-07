package com.example.postapi.batch;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Slf4j
@Component
public class TestDataSeeder implements CommandLineRunner {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding test data for batch job testing...");

        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        Timestamp old35 = Timestamp.valueOf(LocalDateTime.now().minusDays(35));
        Timestamp old45 = Timestamp.valueOf(LocalDateTime.now().minusDays(45));
        Timestamp old60 = Timestamp.valueOf(LocalDateTime.now().minusDays(60));
        Timestamp recent5 = Timestamp.valueOf(LocalDateTime.now().minusDays(5));
        Timestamp recent15 = Timestamp.valueOf(LocalDateTime.now().minusDays(15));
        Timestamp old40 = Timestamp.valueOf(LocalDateTime.now().minusDays(40));

        // Posts older than 30 days - unpublished (SHOULD be cleaned up)
        insertPost("Old Unpublished Post 1", "This is old and unpublished", "Author A", false, false, 0, 0, old35, now);
        insertPost("Old Unpublished Post 2", "Another old unpublished post", "Author B", false, false, 5, 2, old45, now);
        insertPost("Old Unpublished Post 3", "Yet another old post", "Author C", false, false, 10, 1, old60, now);

        // Recent unpublished posts (SHOULD NOT be cleaned up)
        insertPost("Recent Unpublished Post 1", "This is recent and unpublished", "Author D", false, false, 0, 0, recent5, now);
        insertPost("Recent Unpublished Post 2", "Another recent unpublished post", "Author E", false, false, 3, 0, recent15, now);

        // Old but published post (SHOULD NOT be cleaned up)
        insertPost("Old Published Post", "This is old but published", "Author F", true, false, 100, 50, old40, now);

        log.info("Test data seeded: 3 old unpublished (will be cleaned), 2 recent unpublished (safe), 1 old published (safe)");
    }

    private void insertPost(String title, String content, String authorName,
                            boolean isPublished, boolean isDeleted,
                            int viewCount, int likeCount,
                            Timestamp createdAt, Timestamp updatedAt) {
        entityManager.createNativeQuery("""
            INSERT INTO posts (author_name, content, cover_image, created_at, is_deleted, is_published, like_count, title, updated_at, view_count)
            VALUES (:authorName, :content, NULL, :createdAt, :isDeleted, :isPublished, :likeCount, :title, :updatedAt, :viewCount)
            """)
            .setParameter("authorName", authorName)
            .setParameter("content", content)
            .setParameter("createdAt", createdAt)
            .setParameter("isDeleted", isDeleted)
            .setParameter("isPublished", isPublished)
            .setParameter("likeCount", likeCount)
            .setParameter("title", title)
            .setParameter("updatedAt", updatedAt)
            .setParameter("viewCount", viewCount)
            .executeUpdate();
    }
}
