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
 * Created by murphys on 3/22/17.
 */
@Named("level1ComputeTest")
public class Level1ComputeTestProcessor extends AbstractBasicLifeCycleServiceProcessor<Long> {

    private static final int DEFAULT_INTEGER_SERVICES=10;
    private static final int DEFAULT_FLOAT_SERVICES=5;

    public static class Level1ComputeTestArgs extends ServiceArgs {
        @Parameter(names="-integerServiceCount", description="Number of concurrent IntegerComputeTest", required=false)
        Integer integerServiceCount=DEFAULT_INTEGER_SERVICES;
        @Parameter(names="-floatServiceCount", description="Number of concurrent FloatComputeTest", required=false)
        Integer floatServiceCount=DEFAULT_FLOAT_SERVICES;
        @Parameter(names = "-testName", description = "Optional unique test name", required=false)
        String testName="Default Test Name";
    }

    private long resultComputationTime;

    private Level1ComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
        return Level1ComputeTestArgs.parse(jacsServiceData.getArgsArray(), new Level1ComputeTestArgs());
    }

    private final IntegerComputeTestProcessor integerComputeTestProcessor;
    private final FloatComputeTestProcessor floatComputeTestProcessor;

    @Inject
    public Level1ComputeTestProcessor(JacsServiceEngine jacsServiceEngine,
                                      ServiceComputationFactory computationFactory,
                                      JacsServiceDataPersistence jacsServiceDataPersistence,
                                      @PropertyValue(name="service.DefaultWorkingDir") String defaultWorkingDir,
                                      Logger logger,
                                      IntegerComputeTestProcessor integerComputeTestProcessor,
                                      FloatComputeTestProcessor floatComputeTestProcessor) {
        super(jacsServiceEngine, computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
        this.integerComputeTestProcessor=integerComputeTestProcessor;
        this.floatComputeTestProcessor=floatComputeTestProcessor;
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new Level1ComputeTestProcessor.Level1ComputeTestArgs());
    }

    @Override
    public ServiceComputation<Long> processing(JacsServiceData jacsServiceData) {
        String serviceName=getArgs(jacsServiceData).testName;
        logger.info(serviceName+" start processing");
        long startTime=new Date().getTime();
        Level1ComputeTestArgs args=getArgs(jacsServiceData);

        List<JacsServiceData> integerComputeTests=new ArrayList<>();
        for (int i=0;i<args.integerServiceCount;i++) {
            JacsServiceData integerComputeTestProcessorServiceData =
                    integerComputeTestProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData));
            integerComputeTestProcessorServiceData.addArg("-testName");
            integerComputeTestProcessorServiceData.addArg(args.testName+".IntegerTest"+i);
            logger.info("adding integerComputeTest to list id="+integerComputeTestProcessorServiceData.getId());
            integerComputeTests.add(integerComputeTestProcessorServiceData);
        }

        List<JacsServiceData> floatComputeTests=new ArrayList<>();
        for (int i=0;i<args.floatServiceCount;i++) {
            JacsServiceData floatComputeTestProcessorServiceData =
                    floatComputeTestProcessor.createServiceData(new ServiceExecutionContext(jacsServiceData));
            floatComputeTestProcessorServiceData.addArg("-testName");
            floatComputeTestProcessorServiceData.addArg(args.testName+".FloatTest"+i);
            logger.info("adding floatComputeTest to list id="+floatComputeTestProcessorServiceData.getId());
            floatComputeTests.add(floatComputeTestProcessorServiceData);
        }

        int iTest=0;
        for (JacsServiceData j : integerComputeTests) {
            logger.info("submitting integerComputeTest "+iTest+" id="+j.getId());
            j=submit(j);
            logger.info("waiting for integerComputeTest "+iTest+" id="+j.getId());
            waitFor(j);
            logger.info("finished integerComputeTest "+iTest+" id="+j.getId()+" of "+integerComputeTests.size());
            iTest++;
        }

        int fTest=0;
        for (JacsServiceData j : floatComputeTests) {
            logger.info("submitting floatComputeTest "+fTest+" id="+j.getId());
            j=submit(j);
            logger.info("waiting for floatComputeTest "+fTest+" id="+j.getId());
            waitFor(j);
            logger.info("finished floatComputeTest "+fTest+" id="+j.getId()+" of "+floatComputeTests.size());
            fTest++;
        }

        long endTime=new Date().getTime();
        resultComputationTime=endTime-startTime;
        logger.info(serviceName+" end processing, processing time= "+resultComputationTime);
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
