package com.example.postapi.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CleanupJobScheduler {

    private final JobOperator jobOperator;
    private final Job cleanupUnpublishedPostsJob;

    @Scheduled(cron = "0 0 0 * * ?")
    public void runCleanupJob() throws Exception {
        jobOperator.start(cleanupUnpublishedPostsJob, new JobParameters());
    }
}
