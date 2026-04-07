package com.example.postapi.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Properties;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class CleanupJobController {

    private final JobOperator jobOperator;
    private final Job cleanupUnpublishedPostsJob;

    @PostMapping("/cleanup-job")
    public String triggerCleanupJob() throws Exception {
        Properties params = new Properties();
        params.setProperty("time", String.valueOf(System.currentTimeMillis()));
        jobOperator.start(cleanupUnpublishedPostsJob.getName(), params);
        return "Cleanup job triggered successfully";
    }
}
