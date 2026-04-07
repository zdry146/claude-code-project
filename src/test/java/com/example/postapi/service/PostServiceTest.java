package com.example.postapi.service;

import com.example.postapi.exception.NotFoundException;
import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
import com.example.postapi.request.CreatePostRequest;
import com.example.postapi.request.UpdatePostRequest;
import com.example.postapi.response.PageResponse;
import com.example.postapi.response.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    private Post samplePost;

    @BeforeEach
    void setUp() {
        samplePost = Post.builder()
                .id(1L)
                .title("Sample Post")
                .content("Sample Content")
                .authorName("Sample Author")
                .coverImage(null)
                .viewCount(0)
                .likeCount(0)
                .isPublished(false)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- create() ---

    @Test
    void createValidPost() {
        CreatePostRequest request = CreatePostRequest.builder()
                .title("New Post")
                .content("New Content")
                .authorName("New Author")
                .coverImage("http://example.com/cover.jpg")
                .build();

        when(postRepository.save(any(Post.class))).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PostResponse result = postService.create(request);

        assertNotNull(result);
        assertEquals("New Post", result.getTitle());
        assertEquals("New Content", result.getContent());
        assertEquals("New Author", result.getAuthorName());
        assertEquals("http://example.com/cover.jpg", result.getCoverImage());
        assertFalse(result.getIsPublished());
        assertEquals(0, result.getViewCount());
        assertEquals(0, result.getLikeCount());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    void createSetsDefaultValues() {
        CreatePostRequest request = CreatePostRequest.builder()
                .title("Title")
                .content("Content")
                .authorName("Author")
                .build();

        ArgumentCaptor<Post> captor = ArgumentCaptor.forClass(Post.class);
        when(postRepository.save(captor.capture())).thenAnswer(inv -> {
            Post p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        postService.create(request);

        Post saved = captor.getValue();
        assertEquals(0, saved.getViewCount());
        assertEquals(0, saved.getLikeCount());
        assertFalse(saved.getIsPublished());
        assertFalse(saved.getIsDeleted());
    }

    // --- update() ---

    @Test
    void updatePostTitle() {
        UpdatePostRequest request = UpdatePostRequest.builder()
                .title("Updated Title")
                .build();

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        PostResponse result = postService.update(1L, request);

        assertEquals("Updated Title", samplePost.getTitle());
        verify(postRepository).save(samplePost);
    }

    @Test
    void updatePostContent() {
        UpdatePostRequest request = UpdatePostRequest.builder()
                .content("Updated Content")
                .build();

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        postService.update(1L, request);

        assertEquals("Updated Content", samplePost.getContent());
    }

    @Test
    void updatePostAuthorName() {
        UpdatePostRequest request = UpdatePostRequest.builder()
                .authorName("New Author")
                .build();

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        postService.update(1L, request);

        assertEquals("New Author", samplePost.getAuthorName());
    }

    @Test
    void updateEmptyCoverImageSetsNull() {
        UpdatePostRequest request = UpdatePostRequest.builder()
                .coverImage("")
                .build();

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        postService.update(1L, request);

        assertNull(samplePost.getCoverImage());
    }

    @Test
    void updateIsPublished() {
        UpdatePostRequest request = UpdatePostRequest.builder()
                .isPublished(true)
                .build();

        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        postService.update(1L, request);

        assertTrue(samplePost.getIsPublished());
    }

    @Test
    void updateNotFoundThrows() {
        UpdatePostRequest request = UpdatePostRequest.builder()
                .title("New Title")
                .build();

        when(postRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.update(99L, request));
    }

    // --- getById() ---

    @Test
    void getByIdFound() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        doNothing().when(postRepository).incrementViewCount(1L);

        PostResponse result = postService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Sample Post", result.getTitle());
        verify(postRepository).incrementViewCount(1L);
    }

    @Test
    void getByIdNotFoundThrows() {
        when(postRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.getById(99L));
    }

    // --- findById() ---

    @Test
    void findByIdFound() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));

        PostResponse result = postService.findById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    void findByIdNotFoundThrows() {
        when(postRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.findById(99L));
    }

    // --- listPublished() ---

    @Test
    void listPublishedReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(samplePost), pageable, 1);

        when(postRepository.findByIsPublishedTrueAndIsDeletedFalseOrderByCreatedAtDesc(pageable)).thenReturn(page);

        PageResponse<PostResponse> result = postService.listPublished(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());
    }

    // --- listAll() ---

    @Test
    void listAllReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(samplePost), pageable, 1);

        when(postRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable)).thenReturn(page);

        PageResponse<PostResponse> result = postService.listAll(pageable);

        assertEquals(1, result.getContent().size());
    }

    // --- search() ---

    @Test
    void searchReturnsResults() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Post> page = new PageImpl<>(List.of(samplePost), pageable, 1);

        when(postRepository.searchByTitle("Sample", pageable)).thenReturn(page);

        PageResponse<PostResponse> result = postService.search("Sample", pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Sample Post", result.getContent().get(0).getTitle());
    }

    // --- delete() ---

    @Test
    void deleteSoftDeletes() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        postService.delete(1L);

        assertTrue(samplePost.getIsDeleted());
        verify(postRepository).save(samplePost);
    }

    @Test
    void deleteNotFoundThrows() {
        when(postRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.delete(99L));
    }

    // --- togglePublish() ---

    @Test
    void togglePublishFromFalseToTrue() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        postService.togglePublish(1L);

        assertTrue(samplePost.getIsPublished());
    }

    @Test
    void togglePublishFromTrueToFalse() {
        samplePost.setIsPublished(true);
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        when(postRepository.save(any(Post.class))).thenReturn(samplePost);

        postService.togglePublish(1L);

        assertFalse(samplePost.getIsPublished());
    }

    @Test
    void togglePublishNotFoundThrows() {
        when(postRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.togglePublish(99L));
    }

    // --- like() ---

    @Test
    void likeIncrementsCount() {
        when(postRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(samplePost));
        doNothing().when(postRepository).incrementLikeCount(1L);

        PostResponse result = postService.like(1L);

        assertEquals(1, samplePost.getLikeCount());
        verify(postRepository).incrementLikeCount(1L);
    }

    @Test
    void likeNotFoundThrows() {
        when(postRepository.findByIdAndIsDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> postService.like(99L));
    }
}
