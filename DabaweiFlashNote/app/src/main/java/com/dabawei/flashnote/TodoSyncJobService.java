package com.dabawei.flashnote;

import android.app.job.JobParameters;
import android.app.job.JobService;

public final class TodoSyncJobService extends JobService {
    @Override
    public boolean onStartJob(final JobParameters params) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                TodoSyncCoordinator.syncInNewDatabase(TodoSyncJobService.this);
                jobFinished(params, false);
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
