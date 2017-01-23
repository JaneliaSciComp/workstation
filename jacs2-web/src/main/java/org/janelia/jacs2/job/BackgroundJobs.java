package org.janelia.jacs2.job;

import org.janelia.jacs2.service.impl.JacsJobRunner;
import org.janelia.jacs2.service.impl.JacsQueueSyncer;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Singleton;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

@Singleton
public class BackgroundJobs implements ServletContextListener {

    private JacsQueueSyncer queueSyncer;
    private JacsJobRunner jobRunner;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        Instance<JacsQueueSyncer> queueSyncerSource = CDI.current().select(JacsQueueSyncer.class);
        Instance<JacsJobRunner> jobRunnerSource = CDI.current().select(JacsJobRunner.class);
        queueSyncer = queueSyncerSource.get();
        jobRunner = jobRunnerSource.get();
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        queueSyncer.destroy();
        jobRunner.destroy();
    }

}
