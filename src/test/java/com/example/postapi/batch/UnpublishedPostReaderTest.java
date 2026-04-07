package com.example.postapi.batch;

import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnpublishedPostReaderTest {

    @Mock
    private PostRepository postRepository;

    private UnpublishedPostReader reader;

    @BeforeEach
    void setUp() {
        reader = new UnpublishedPostReader(postRepository);
    }

    @Test
    void read_shouldReturnUnpublishedPostsOlderThan30Days() {
        Post post1 = Post.builder().id(1L).title("Old Post 1").build();
        Post post2 = Post.builder().id(2L).title("Old Post 2").build();
        List<Post> posts = Arrays.asList(post1, post2);
        when(postRepository.findUnpublishedOlderThan(any(LocalDateTime.class))).thenReturn(posts);

        Post first = reader.read();
        Post second = reader.read();
        Post third = reader.read();

        assertEquals(1L, first.getId());
        assertEquals(2L, second.getId());
        assertNull(third);
    }

    @Test
    void read_shouldReturnNullWhenNoUnpublishedPosts() {
        when(postRepository.findUnpublishedOlderThan(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        Post result = reader.read();

        assertNull(result);
        verify(postRepository, times(1)).findUnpublishedOlderThan(any(LocalDateTime.class));
    }

    @Test
    void read_shouldQueryWith30DaysThreshold() {
        when(postRepository.findUnpublishedOlderThan(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        reader.read();

        verify(postRepository).findUnpublishedOlderThan(argThat(dateTime ->
                dateTime.isAfter(LocalDateTime.now().minusDays(31)) &&
                dateTime.isBefore(LocalDateTime.now().minusDays(29))
        ));
    }

    @Test
    void read_shouldOnlyQueryOnce() {
        when(postRepository.findUnpublishedOlderThan(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        reader.read();
        reader.read();
        reader.read();

        verify(postRepository, times(1)).findUnpublishedOlderThan(any(LocalDateTime.class));
    }
}
