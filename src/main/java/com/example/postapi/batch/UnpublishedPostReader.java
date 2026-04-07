package com.example.postapi.batch;

import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;

@RequiredArgsConstructor
public class UnpublishedPostReader implements ItemReader<Post> {

    private static final int DAYS_THRESHOLD = 30;

    private final PostRepository postRepository;
    private Iterator<Post> iterator;
    private boolean initialized = false;

    @Override
    public Post read() {
        if (!initialized) {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(DAYS_THRESHOLD);
            List<Post> unpublishedPosts = postRepository.findUnpublishedOlderThan(cutoffDate);
            this.iterator = unpublishedPosts.iterator();
            this.initialized = true;
        }

        if (iterator != null && iterator.hasNext()) {
            return iterator.next();
        }
        return null;
    }
}
