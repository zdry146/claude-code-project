package com.example.postapi.batch;

import com.example.postapi.model.Post;
import com.example.postapi.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class CleanupBatchConfig {

    private final PostRepository postRepository;

    @Bean
    public ItemReader<Post> unpublishedPostReader() {
        return new UnpublishedPostReader(postRepository);
    }

    @Bean
    public ItemProcessor<Post, Post> softDeleteProcessor() {
        return post -> {
            post.setIsDeleted(true);
            log.info("Soft-deleting unpublished post: id={}", post.getId());
            return post;
        };
    }

    @Bean
    public ItemWriter<Post> softDeleteWriter() {
        return posts -> {
            for (Post post : posts) {
                postRepository.save(post);
            }
            log.info("Batch soft-deleted {} unpublished posts", posts.size());
        };
    }

    @Bean
    public Step cleanupStep(JobRepository jobRepository,
                             PlatformTransactionManager transactionManager) {
        return new StepBuilder("cleanupStep", jobRepository)
                .<Post, Post>chunk(100)
                .transactionManager(transactionManager)
                .reader(unpublishedPostReader())
                .processor(softDeleteProcessor())
                .writer(softDeleteWriter())
                .build();
    }

    @Bean
    public Job cleanupUnpublishedPostsJob(JobRepository jobRepository,
                                          Step cleanupStep) {
        return new JobBuilder("cleanupUnpublishedPostsJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cleanupStep)
                .build();
    }
}
