package com.example.postapi.service;

import com.example.postapi.exception.NotFoundException;
import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
import com.example.postapi.request.CreatePostRequest;
import com.example.postapi.request.UpdatePostRequest;
import com.example.postapi.response.PageResponse;
import com.example.postapi.response.PostResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    @Transactional
    public PostResponse create(CreatePostRequest request) {
        log.info("Creating post, title={}", request.getTitle());
        Post post = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .authorName(request.getAuthorName())
                .coverImage(request.getCoverImage())
                .viewCount(0)
                .likeCount(0)
                .isPublished(false)
                .isDeleted(false)
                .build();
        Post saved = postRepository.save(post);
        log.info("Post created, id={}", saved.getId());
        return PostResponse.fromEntity(saved);
    }

    @Transactional
    public PostResponse update(Long id, UpdatePostRequest request) {
        log.info("Updating post, id={}", id);
        Post post = findPostById(id);
        if (request.getTitle() != null) post.setTitle(request.getTitle());
        if (request.getContent() != null) post.setContent(request.getContent());
        if (request.getAuthorName() != null) post.setAuthorName(request.getAuthorName());
        if (request.getCoverImage() != null) {
            post.setCoverImage(request.getCoverImage().isEmpty() ? null : request.getCoverImage());
        }
        if (request.getIsPublished() != null) post.setIsPublished(request.getIsPublished());
        Post saved = postRepository.save(post);
        log.info("Post updated, id={}", saved.getId());
        return PostResponse.fromEntity(saved);
    }

    @Transactional
    public PostResponse getById(Long id) {
        log.info("Getting post by id={}", id);
        Post post = postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("帖子不存在，id=" + id));
        postRepository.incrementViewCount(id);
        post.setViewCount(post.getViewCount() + 1);
        return PostResponse.fromEntity(post);
    }

    @Transactional(readOnly = true)
    public PostResponse findById(Long id) {
        log.info("Finding post by id={}", id);
        return PostResponse.fromEntity(findPostById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listPublished(Pageable pageable) {
        log.info("Listing published posts");
        Page<Post> page = postRepository.findByIsPublishedTrueAndIsDeletedFalseOrderByCreatedAtDesc(pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> listAll(Pageable pageable) {
        log.info("Listing all posts");
        Page<Post> page = postRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
        return toPageResponse(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> search(String keyword, Pageable pageable) {
        log.info("Searching posts, keyword={}", keyword);
        Page<Post> page = postRepository.searchByTitle(keyword, pageable);
        return toPageResponse(page);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting post, id={}", id);
        Post post = findPostById(id);
        post.setIsDeleted(true);
        postRepository.save(post);
        log.info("Post soft-deleted, id={}", id);
    }

    @Transactional
    public PostResponse togglePublish(Long id) {
        log.info("Toggling publish status, id={}", id);
        Post post = findPostById(id);
        post.setIsPublished(!post.getIsPublished());
        Post saved = postRepository.save(post);
        return PostResponse.fromEntity(saved);
    }

    @Transactional
    public PostResponse like(Long id) {
        log.info("Liking post, id={}", id);
        Post post = findPostById(id);
        postRepository.incrementLikeCount(id);
        post.setLikeCount(post.getLikeCount() + 1);
        return PostResponse.fromEntity(post);
    }

    private Post findPostById(Long id) {
        return postRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("帖子不存在，id=" + id));
    }

    private PageResponse<PostResponse> toPageResponse(Page<Post> page) {
        List<PostResponse> content = page.getContent().stream()
                .map(PostResponse::fromEntity)
                .toList();
        return PageResponse.<PostResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
