package com.example.postapi.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanupJobControllerTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job cleanupUnpublishedPostsJob;

    @InjectMocks
    private CleanupJobController controller;

    @Test
    void triggerCleanupJob_shouldLaunchJobWithParameters() throws Exception {
        when(jobLauncher.run(eq(cleanupUnpublishedPostsJob), any(JobParameters.class)))
                .thenReturn(null);

        String result = controller.triggerCleanupJob();

        assertEquals("Cleanup job triggered successfully", result);
        verify(jobLauncher, times(1)).run(eq(cleanupUnpublishedPostsJob), any(JobParameters.class));
    }

    @Test
    void triggerCleanupJob_shouldPassJobParametersWithTimestamp() throws Exception {
        when(jobLauncher.run(any(Job.class), any(JobParameters.class)))
                .thenReturn(null);

        controller.triggerCleanupJob();

        verify(jobLauncher).run(eq(cleanupUnpublishedPostsJob), any(JobParameters.class));
    }
}
