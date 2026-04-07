package com.example.postapi.repository;

import com.example.postapi.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByIsPublishedTrueAndIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    Optional<Post> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT p FROM Post p WHERE p.isDeleted = false AND LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> searchByTitle(@Param("keyword") String keyword, Pageable pageable);

    @Modifying
    @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id")
    void incrementLikeCount(@Param("id") Long id);
}
