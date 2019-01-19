package io.mrarm.irc.job;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ServerPingJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        ServerPingScheduler.getInstance(this).onJobRan();
        ServerPingTask.pingServers(this, () -> jobFinished(jobParameters, false));
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        // We'll do nothing - the pings were already sent, and we're just awaiting reply.
        return false;
    }

}
