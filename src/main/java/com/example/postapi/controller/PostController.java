package com.example.postapi.controller;

import com.example.postapi.response.ApiResult;
import com.example.postapi.response.PageResponse;
import com.example.postapi.response.PostResponse;
import com.example.postapi.request.CreatePostRequest;
import com.example.postapi.request.UpdatePostRequest;
import com.example.postapi.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "帖子管理", description = "帖子 CRUD 和列表操作")
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "创建帖子", description = "创建一个新的帖子")
    @PostMapping
    public ApiResult<PostResponse> create(@Valid @RequestBody CreatePostRequest request) {
        PostResponse post = postService.create(request);
        return ApiResult.ok("帖子创建成功", post);
    }

    @Operation(summary = "更新帖子", description = "根据 ID 更新帖子内容")
    @PutMapping("/{id}")
    public ApiResult<PostResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        PostResponse post = postService.update(id, request);
        return ApiResult.ok("帖子更新成功", post);
    }

    @Operation(summary = "获取帖子", description = "根据 ID 获取帖子详情，同时增加浏览次数")
    @GetMapping("/{id}")
    public ApiResult<PostResponse> getById(@PathVariable Long id) {
        PostResponse post = postService.getById(id);
        return ApiResult.ok(post);
    }

    @Operation(summary = "删除帖子", description = "根据 ID 删除帖子（软删除）")
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ApiResult.ok("帖子删除成功", null);
    }

    @Operation(summary = "获取已发布帖子列表", description = "分页获取所有已发布的帖子")
    @GetMapping("/published")
    public ApiResult<PageResponse<PostResponse>> listPublished(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<PostResponse> result = postService.listPublished(pageable);
        return ApiResult.ok(result);
    }

    @Operation(summary = "获取所有帖子", description = "分页获取所有帖子，包含未发布的（管理员用）")
    @GetMapping("/all")
    public ApiResult<PageResponse<PostResponse>> listAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<PostResponse> result = postService.listAll(pageable);
        return ApiResult.ok(result);
    }

    @Operation(summary = "搜索帖子", description = "根据标题关键词搜索帖子")
    @GetMapping("/search")
    public ApiResult<PageResponse<PostResponse>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<PostResponse> result = postService.search(keyword, pageable);
        return ApiResult.ok(result);
    }

    @Operation(summary = "切换发布状态", description = "发布或取消发布帖子")
    @PostMapping("/{id}/toggle-publish")
    public ApiResult<PostResponse> togglePublish(@PathVariable Long id) {
        PostResponse post = postService.togglePublish(id);
        return ApiResult.ok("发布状态已切换", post);
    }

    @Operation(summary = "点赞帖子", description = "为帖子增加一个点赞")
    @PostMapping("/{id}/like")
    public ApiResult<PostResponse> like(@PathVariable Long id) {
        PostResponse post = postService.like(id);
        return ApiResult.ok("点赞成功", post);
    }
}
