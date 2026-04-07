package com.example.postapi.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CleanupJobScheduler {

    private final JobLauncher jobLauncher;
    private final Job cleanupUnpublishedPostsJob;

    @Scheduled(cron = "0 0 0 * * ?")
    public void runCleanupJob() throws Exception {
        jobLauncher.run(cleanupUnpublishedPostsJob, new org.springframework.batch.core.JobParameters());
    }
}
