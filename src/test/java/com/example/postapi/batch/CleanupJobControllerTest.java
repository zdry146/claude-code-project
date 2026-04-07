package com.example.postapi.batch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanupJobControllerTest {

    @Mock
    private JobOperator jobOperator;

    @Mock
    private Job cleanupUnpublishedPostsJob;

    @InjectMocks
    private CleanupJobController controller;

    @Test
    void triggerCleanupJob_shouldLaunchJobWithParameters() throws Exception {
        when(jobOperator.start(any(), any(Properties.class)))
                .thenReturn(null);

        String result = controller.triggerCleanupJob();

        assertEquals("Cleanup job triggered successfully", result);
        verify(jobOperator, times(1)).start(any(), any(Properties.class));
    }

    @Test
    void triggerCleanupJob_shouldPassJobNameAndParameters() throws Exception {
        when(cleanupUnpublishedPostsJob.getName()).thenReturn("cleanupUnpublishedPostsJob");
        when(jobOperator.start(any(), any(Properties.class)))
                .thenReturn(null);

        controller.triggerCleanupJob();

        verify(jobOperator).start(eq("cleanupUnpublishedPostsJob"), any(Properties.class));
    }
}
