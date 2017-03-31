package org.janelia.jacs2.asyncservice.demo;

import com.beust.jcommander.Parameter;
import org.janelia.jacs2.asyncservice.JacsServiceEngine;
import org.janelia.jacs2.asyncservice.common.*;
import org.janelia.jacs2.cdi.qualifier.PropertyValue;
import org.janelia.jacs2.dataservice.persistence.JacsServiceDataPersistence;
import org.janelia.jacs2.model.jacsservice.JacsServiceData;
import org.janelia.jacs2.model.jacsservice.ServiceMetaData;
import org.slf4j.Logger;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import java.util.stream.LongStream;

/**
 * Created by murphys on 2/23/17.
 */
@Named("integerComputeTest")
public class IntegerComputeTestProcessor extends AbstractBasicLifeCycleServiceProcessor<Long> {

    private static final int DEFAULT_MATRIX_SIZE=700;
    private static final int DEFAULT_ITERATIONS=10;

    public static class IntegerComputeTestArgs extends ServiceArgs {
        @Parameter(names = "-matrixSize", description = "Size of matrix NxN", required = false)
        Integer matrixSize=DEFAULT_MATRIX_SIZE;
        @Parameter(names = "-iterations", description = "Iterations per matrix multiply", required = false)
        Integer iterations=DEFAULT_ITERATIONS;
        @Parameter(names = "-testName", description = "Optional unique test name", required = false)
        String testName="IntegerComputeTest";
    }

    private long resultComputationTime;

    public static IntegerComputeTestArgs getArgs(JacsServiceData jacsServiceData) {
        return IntegerComputeTestArgs.parse(jacsServiceData.getArgsArray(), new IntegerComputeTestArgs());
    }

    @Inject
    public IntegerComputeTestProcessor (ServiceComputationFactory computationFactory,
                                        JacsServiceDataPersistence jacsServiceDataPersistence,
                                        @PropertyValue(name = "service.DefaultWorkingDir") String defaultWorkingDir,
                                        Logger logger) {
        super(computationFactory, jacsServiceDataPersistence, defaultWorkingDir, logger);
    }

    @Override
    public ServiceMetaData getMetadata() {
        return ServiceArgs.getMetadata(this.getClass(), new IntegerComputeTestProcessor.IntegerComputeTestArgs());
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
    protected List<JacsServiceData> submitServiceDependencies(JacsServiceData jacsServiceData) {
        return null;
    }

    @Override
    public ServiceComputation<JacsServiceData> processing(JacsServiceData jacsServiceData) {
        String serviceName=getArgs(jacsServiceData).testName;
        logger.debug(serviceName+" start");
        IntegerComputeTestArgs args=getArgs(jacsServiceData);
        int matrixSize=DEFAULT_MATRIX_SIZE;
        if (args.matrixSize!=null) {
            matrixSize=args.matrixSize;
        }
        int iterations=DEFAULT_ITERATIONS;
        if (args.iterations!=null) {
            iterations=args.iterations;
        }
        logger.debug("matrixSize="+matrixSize+", iterations="+iterations);
        long startTime=new Date().getTime();
        jacsServiceData.getArgs();
        Random random = new Random(startTime);
        LongStream longStream = random.longs(0L, 100L);
        PrimitiveIterator.OfLong iterator=longStream.iterator();
        long[] matrix1=new long[matrixSize*matrixSize];
        long[] matrix2=new long[matrixSize*matrixSize];
        long[] result=new long[matrixSize*matrixSize];
        int position=0;
        // Create matrices
        logger.debug("Creating matrices");
        for (int i=0;i<matrixSize;i++) {
            for (int j=0;j<matrixSize;j++) {
                matrix1[position]=iterator.nextLong();
                matrix2[position]=iterator.nextLong();
                position++;
            }
        }
        // Do multiply
        logger.debug("Doing matrix multiply");
        for (int i=0;i<iterations;i++) {
            logger.debug("Starting iteration "+i+" of "+iterations);
            for (int column2=0;column2<matrixSize;column2++) {
                for (int row1 = 0; row1 < matrixSize; row1++) {
                    long sum = 0L;
                    int row2=0;
                    for (int column1=0; column1 < matrixSize; column1++) {
                        sum += matrix1[row1 * matrixSize + column1] * matrix2[row2 * matrixSize + column2];
                        row2++;
                    }
                    result[row1 * matrixSize + column2] = sum;
                }
            }
        }
        long doneTime=new Date().getTime();
        resultComputationTime=doneTime-startTime;
        logger.debug(serviceName+" end, elapsed time ms="+resultComputationTime);
        return computationFactory.newCompletedComputation(jacsServiceData);
    }

}
