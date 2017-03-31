package org.janelia.jacs2.asyncservice.demo;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.*;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by murphys on 3/29/17.
 */

@Named("level2ComputeTest")
public class Level2ComputeTestProcessor extends AbstractBasicLifeCycleServiceProcessor<Long> {

    private static final int DEFAULT_COUNT=5;

    public static class Level2ComputeTestArgs extends ServiceArgs {
        @Parameter(names="-levelCount", description="Number of concurrent child level tests", required=false)
        Integer levelCount=DEFAULT_COUNT;
        @Parameter(names = "-testName", description = "Optional unique test name", required=false)
        String testName="Level2ComputeTest";
    }

    private long resultComputationTime;

    public static Level2ComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
        return Level2ComputeTestArgs.parse(jacsServiceData.getArgsArray(), new Level2ComputeTestArgs());
    }

    private final Level1ComputeTestProcessor level1ComputeTestProcessor;

    @Inject
    public Level2ComputeTestProcessor(ServiceComputationFactory computationFactory,
                                      JacsServiceDataPersistence jacsServiceDataPersistence,
                                      @PropertyValue(name="service.DefaultWorkingDir") String defaultWorkingDir,
                                      Logger logger,
                                      Level1ComputeTestProcessor level1ComputeTestProcessor) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.level1ComputeTestProcessor=level1ComputeTestProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Level2ComputeTestProcessor.Level2ComputeTestArgs());
    }

    @Override
    public ServiceResultHandler<Long> getResultHandler() {
        return new ServiceResultHandler<Long>() {
            @Override
            public boolean isResultReady(JacsServiceData jacsServiceData) {
                return true;
            }

            @Override
            public Long collectResult(JacsServiceData jacsServiceData) {
                return null;
            }

            @Override
            public void updateServiceDataResult(JacsServiceData jacsServiceData, Long result) {

            }

            @Override
            public Long getServiceDataResult(JacsServiceData jacsServiceData) {
                return null;
            }
        };
    }

    @Override
    public ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        String serviceName = getArgs(jacsServiceData).testName;
        logger.info(serviceName + " start processing");
        long startTime = new Date().getTime();
        Level2ComputeTestArgs args = getArgs(jacsServiceData);

        return computationFactory.newCompletedComputation(jacsServiceData)
                .thenApply(jsd -> {
                    for (int i = 0; i < args.levelCount; i++) {
                        String testName = args.testName + ".Level1Test" + i;
                        JacsServiceData j =
                                level1ComputeTestProcessor.createServiceData(new ServiceExecutionContext(jsd));
                        j.addArg("-testName");
                        j.addArg(testName);
                        logger.info("adding level1ComputeTest " + testName);
                        jacsServiceDataPersistence.saveHierarchy(j);
                    }
                    return jsd;
                })
                .thenSuspendUntil(() -> !suspendUntilAllDependenciesComplete(jacsServiceData))
                .thenApply(jsd -> {
                    long endTime = new Date().getTime();
                    resultComputationTime = endTime - startTime;
                    logger.info(serviceName + " end processing, time=" + resultComputationTime);
                    return jsd;
                });

    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        return null;
    }

}

