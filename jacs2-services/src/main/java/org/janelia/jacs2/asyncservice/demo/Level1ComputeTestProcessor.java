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
        String testName="Level1ComputeTest";
    }

    private long resultComputationTime;

    public static Level1ComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
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
        List<JacsServiceData> submittedJsdList=new ArrayList<>();

        for (JacsServiceData j : integerComputeTests) {
            IntegerComputeTestProcessor.IntegerComputeTestArgs jArgs=
                    IntegerComputeTestProcessor.getArgs(j);
            String jName=jArgs.testName;
            logger.info("submitting integerComputeTest "+jName);
            submittedJsdList.add(submit(j));
        }

        iTest=0;
        for (JacsServiceData j : submittedJsdList) {
            IntegerComputeTestProcessor.IntegerComputeTestArgs jArgs=
                    IntegerComputeTestProcessor.getArgs(j);
            String jName=jArgs.testName;
            logger.info("waiting for integerComputeTest "+jName);
            waitFor(j);
            logger.info("finished integerComputeTest "+jName);
            iTest++;
        }

        int fTest=0;
        submittedJsdList.clear();
        for (JacsServiceData j : floatComputeTests) {
            FloatComputeTestProcessor.FloatComputeTestArgs jArgs=
                    FloatComputeTestProcessor.getArgs(j);
            String jName=jArgs.testName;
            logger.info("submitting floatComputeTest "+jName);
            submittedJsdList.add(submit(j));
        }

        for (JacsServiceData j : submittedJsdList) {
            FloatComputeTestProcessor.FloatComputeTestArgs jArgs=
                    FloatComputeTestProcessor.getArgs(j);
            String jName=jArgs.testName;
            logger.info("waiting for floatComputeTest "+jName);
            waitFor(j);
            logger.info("finished floatComputeTest "+jName);
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
