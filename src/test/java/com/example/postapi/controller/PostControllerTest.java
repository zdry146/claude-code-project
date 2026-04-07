package com.example.postapi.controller;

import com.example.postapi.exception.NotFoundException;
import com.example.postapi.response.ApiResult;
import com.example.postapi.response.PageResponse;
import com.example.postapi.response.PostResponse;
import com.example.postapi.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostController.class)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    private PostResponse makePost(Long id, String title) {
        return PostResponse.builder()
                .id(id)
                .title(title)
                .content("Content " + id)
                .authorName("Author " + id)
                .viewCount(0)
                .likeCount(0)
                .isPublished(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void createPost() throws Exception {
        PostResponse created = makePost(1L, "New Post");
        when(postService.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Post\",\"content\":\"Content\",\"authorName\":\"Author\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.title").value("New Post"));
    }

    @Test
    void updatePost() throws Exception {
        PostResponse updated = makePost(1L, "Updated Title");
        when(postService.update(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated Title\",\"content\":\"Updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated Title"));
    }

    @Test
    void getPostById() throws Exception {
        PostResponse post = makePost(1L, "Test Post");
        when(postService.getById(1L)).thenReturn(post);

        mockMvc.perform(get("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Test Post"));
    }

    @Test
    void getPostNotFound() throws Exception {
        when(postService.getById(99L)).thenThrow(new NotFoundException("Post not found"));

        mockMvc.perform(get("/api/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    @Test
    void deletePost() throws Exception {
        doNothing().when(postService).delete(1L);

        mockMvc.perform(delete("/api/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(postService).delete(1L);
    }

    @Test
    void listPublishedPosts() throws Exception {
        PageResponse<PostResponse> page = PageResponse.<PostResponse>builder()
                .content(List.of(makePost(1L, "Published Post")))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(postService.listPublished(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/posts/published")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Published Post"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void listAllPosts() throws Exception {
        PageResponse<PostResponse> page = PageResponse.<PostResponse>builder()
                .content(List.of(makePost(1L, "All Post")))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(postService.listAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/posts/all")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("All Post"));
    }

    @Test
    void searchPosts() throws Exception {
        PageResponse<PostResponse> page = PageResponse.<PostResponse>builder()
                .content(List.of(makePost(1L, "Search Result")))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .first(true)
                .last(true)
                .build();

        when(postService.search(eq("Search"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/posts/search")
                        .param("keyword", "Search")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Search Result"));
    }

    @Test
    void togglePublish() throws Exception {
        PostResponse toggled = makePost(1L, "Toggled");
        toggled.setIsPublished(true);
        when(postService.togglePublish(1L)).thenReturn(toggled);

        mockMvc.perform(post("/api/posts/1/toggle-publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isPublished").value(true));
    }

    @Test
    void likePost() throws Exception {
        PostResponse liked = makePost(1L, "Liked");
        liked.setLikeCount(1);
        when(postService.like(1L)).thenReturn(liked);

        mockMvc.perform(post("/api/posts/1/like"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }
}
