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
    public Level2ComputeTestProcessor(JacsServiceEngine jacsServiceEngine,
                                      ServiceComputationFactory computationFactory,
                                      JacsServiceDataPersistence jacsServiceDataPersistence,
                                      @PropertyValue(name="service.DefaultWorkingDir") String defaultWorkingDir,
                                      Logger logger,
                                      Level1ComputeTestProcessor level1ComputeTestProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.level1ComputeTestProcessor=level1ComputeTestProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Level2ComputeTestProcessor.Level2ComputeTestArgs());
    }

    @Override
    public ServiceComputation<Long> processing(JacsServiceData jacsServiceData) {
        String serviceName=getArgs(jacsServiceData).testName;
        logger.info(serviceName+" start processing");
        long startTime=new Date().getTime();
        Level2ComputeTestArgs args=getArgs(jacsServiceData);

        List<JacsServiceData> levelComputeTests=new ArrayList<>();
        for (int i=0;i<args.levelCount;i++) {
            JacsServiceData jsd =
                    level1ComputeTestProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData));
            jsd.addArg("-testName");
            jsd.addArg(args.testName+".Level1Test"+i);
            logger.info("adding level1ComputeTest to list id="+jsd.getId());
            levelComputeTests.add(jsd);
        }

        int iTest=0;
        for (JacsServiceData j : levelComputeTests) {
            logger.info("submitting level 1 ComputeTest "+iTest+" id="+j.getId());
            j=submit(j);
            logger.info("waiting for level 1 ComputeTest "+iTest+" id="+j.getId());
            waitFor(j);
            logger.info("finished level 1 ComputeTest "+iTest+" id="+j.getId()+" of "+levelComputeTests.size());
            iTest++;
        }

        long endTime=new Date().getTime();
        resultComputationTime=endTime-startTime;
        logger.info(serviceName+" end processing, time="+resultComputationTime);
        return computationFactory.newCompletedComputation(resultComputationTime);
    }

    @Override
    protected Long getResult(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    protected void setResult(Long result, JacsServiceData jacsServiceData) {

    }

    @Override
    protected ServiceComputation<JacsServiceData> prepareProcessing(JacsServiceData jacsServiceData) {
        return createComputation(jacsServiceData);
    }

    @Override
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    protected boolean isResultAvailable(JacsServiceData jacsServiceData) {
        return false;
    }

    @Override
    protected Long retrieveResult(JacsServiceData jacsServiceData) {
        return null;
    }

    private void waitFor(JacsServiceData j) {
        for (;;) {
            try {
                Thread.sleep(500);
            } catch (Exception ex) {
            }
            j = jacsServiceDataPersistence.findById(j.getId());
            if (j.hasCompleted()) {
                logger.info("service id="+j.getId()+" has completed");
                return;
            } else {
//                logger.info("service id="+j.getId()+" has not completed");
            }
        }
    }

}

